package com.artemiy.player.widget

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.artemiy.player.lyrics.LyricsExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lives in the playback service: watches the player and keeps [WidgetState] (and with it the
 * widgets on the home screen) up to date — the song, its cover, play / pause, and, for the widgets
 * that show them, the lyrics line by line and the progress.
 */
class WidgetUpdater(private val context: Context, private val player: Player) : Player.Listener {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var coverJob: Job? = null
    private var lyricsJob: Job? = null
    private var coverFor: Long = -1
    private var lyricsFor: Long = -1
    private var items: List<WItem>? = null
    private var lyricsLoading = false

    init {
        current = this
        player.addListener(this)
        refresh()
    }

    /** Fires when something on the widgets is due to change: the next word, the next line, the next stretch of progress. */
    private val ticker = Runnable { refresh() }

    private fun scheduleNext(snap: WidgetSnapshot) {
        handler.removeCallbacks(ticker)
        if (!player.isPlaying || !Widgets.any(context)) return
        var wait = Long.MAX_VALUE
        if (Widgets.wantsProgress(context)) wait = PROGRESS_MS
        if (Widgets.wantsLyrics(context)) {
            val next = snap.items?.let { nextLyricEvent(it, snap.positionMs) }
            if (next != null) wait = minOf(wait, ((next - snap.positionMs) / player.playbackParameters.speed).toLong())
        }
        if (wait != Long.MAX_VALUE) handler.postDelayed(ticker, wait.coerceAtLeast(MIN_WAIT_MS))
    }

    private fun snapshot(): WidgetSnapshot {
        val item: MediaItem? = player.currentMediaItem
        if (item == null) return WidgetSnapshot()
        return WidgetSnapshot(
            hasSong = true,
            songId = item.mediaId.toLongOrNull() ?: -1,
            title = item.mediaMetadata.title?.toString().orEmpty(),
            artist = item.mediaMetadata.artist?.toString().orEmpty(),
            playing = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.coerceAtLeast(0),
            items = if (lyricsFor == (item.mediaId.toLongOrNull() ?: -1)) items else null,
            lyricsLoading = lyricsLoading,
        )
    }

    /** Read the player afresh and redraw. */
    fun refresh() {
        val snap = snapshot()
        WidgetState.snapshot = snap
        if (!Widgets.any(context)) return
        if (snap.hasSong) {
            loadCoverIfNeeded(snap)
            loadLyricsIfNeeded(snap)
        } else {
            WidgetState.cover = null
            coverFor = -1
        }
        Widgets.updateAll(context)
        scheduleNext(snap)
    }

    private fun loadCoverIfNeeded(snap: WidgetSnapshot) {
        if (coverFor == snap.songId) return
        coverFor = snap.songId
        WidgetState.cover = null
        coverJob?.cancel()
        val uri = player.currentMediaItem?.localConfiguration?.uri ?: return
        if (Build.VERSION.SDK_INT < 29) return
        coverJob = scope.launch {
            val bmp: Bitmap? = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.loadThumbnail(uri, Size(COVER_SIZE, COVER_SIZE), null) }.getOrNull()
            }
            if (coverFor == snap.songId) {
                WidgetState.cover = bmp
                Widgets.updateAll(context)
            }
        }
    }

    private fun loadLyricsIfNeeded(snap: WidgetSnapshot) {
        if (!Widgets.wantsLyrics(context)) return
        if (lyricsFor == snap.songId) return
        lyricsFor = snap.songId
        items = null
        lyricsLoading = true
        lyricsJob?.cancel()
        val uri = player.currentMediaItem?.localConfiguration?.uri ?: return
        lyricsJob = scope.launch {
            // The same breather the app's own player takes: reading tags beside the starting decoder raced for it.
            delay(1500)
            val parsed = runCatching { LyricsExtractor.extract(context, uri) }.getOrNull()
            if (lyricsFor != snap.songId) return@launch
            val gapDots = runCatching { com.artemiy.player.data.SettingsRepository(context).lrcGapDots.first() }.getOrDefault(true)
            items = parsed?.let { itemsOf(it, gapDots) }?.takeIf { it.isNotEmpty() }
            lyricsLoading = false
            refresh()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        refresh()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
    override fun onPlaybackStateChanged(playbackState: Int) = refresh()
    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        refresh()
    }

    /** The service is going: the widgets go back to "nothing playing". */
    fun release() {
        handler.removeCallbacksAndMessages(null)
        scope.coroutineContext[Job]?.cancel()
        player.removeListener(this)
        WidgetState.snapshot = WidgetSnapshot()
        WidgetState.cover = null
        if (Widgets.any(context)) Widgets.updateAll(context)
        if (current === this) current = null
    }

    companion object {
        @Volatile var current: WidgetUpdater? = null
        private const val PROGRESS_MS = 2000L
        private const val MIN_WAIT_MS = 120L
        private const val COVER_SIZE = 400
    }
}
