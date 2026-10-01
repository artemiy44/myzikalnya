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
import com.artemiy.player.lyrics.ParsedLyrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private var lines: List<WLine>? = null
    private var lyricsLoading = false
    private var lastTick = ""

    init {
        current = this
        player.addListener(this)
        refresh()
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (!player.isPlaying || !Widgets.any(context)) return
            // Only redrawn when something shows: another line of the lyrics, another two seconds of progress.
            val snap = snapshot()
            val key = (if (Widgets.wantsLyrics(context)) snap.activeLine().toString() else "") +
                "|" + (if (Widgets.wantsProgress(context)) (snap.positionMs / 2000).toString() else "")
            if (key != lastTick) {
                lastTick = key
                WidgetState.snapshot = snap
                Widgets.updateAll(context)
            }
            handler.postDelayed(this, TICK_MS)
        }
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
            lines = if (lyricsFor == (item.mediaId.toLongOrNull() ?: -1)) lines else null,
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
        handler.removeCallbacks(ticker)
        if (player.isPlaying) handler.postDelayed(ticker, TICK_MS)
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
        lines = null
        lyricsLoading = true
        lyricsJob?.cancel()
        val uri = player.currentMediaItem?.localConfiguration?.uri ?: return
        lyricsJob = scope.launch {
            // The same breather the app's own player takes: reading tags beside the starting decoder raced for it.
            delay(1500)
            val parsed = runCatching { LyricsExtractor.extract(context, uri) }.getOrNull()
            if (lyricsFor != snap.songId) return@launch
            lines = (parsed as? ParsedLyrics.Synced)?.lines
                ?.filter { it.text.isNotBlank() }
                ?.map { WLine(it.timeMs, it.text.trim()) }
                ?.takeIf { it.isNotEmpty() }
            lyricsLoading = false
            refresh()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        lastTick = ""
        refresh()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
    override fun onPlaybackStateChanged(playbackState: Int) = refresh()
    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        lastTick = ""
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
        private const val TICK_MS = 400L
        private const val COVER_SIZE = 400
    }
}
