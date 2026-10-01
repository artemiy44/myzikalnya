package com.artemiy.player.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.artemiy.player.data.FadeMode
import com.artemiy.player.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Soft joins between songs: the end of a song fades out and the next song fades in, by turning the
 * player's own volume down and up (a true overlap would take two players). Songs of one album that
 * follow each other are left alone — live albums and mixes are meant to run into each other.
 */
class SoftFades(context: Context, private val player: Player) : Player.Listener {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mode = FadeMode.OFF
    /** Where the current song was when it started playing, for the fade-in. */
    private var startedAt = 0L

    init {
        player.addListener(this)
        scope.launch {
            SettingsRepository(context).fadeMode.collect {
                mode = it
                apply()
            }
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            apply()
            if (player.isPlaying && mode != FadeMode.OFF) handler.postDelayed(this, STEP_MS)
        }
    }

    private fun sameAlbumNext(): Boolean {
        val here: MediaItem = player.currentMediaItem ?: return false
        val next: MediaItem = player.getMediaItemAt(player.nextMediaItemIndex.takeIf { it >= 0 } ?: return false)
        val album = here.mediaMetadata.albumTitle?.toString().orEmpty()
        return album.isNotBlank() && album.equals(next.mediaMetadata.albumTitle?.toString().orEmpty(), ignoreCase = true)
    }

    /** The volume the position calls for. */
    private fun apply() {
        if (mode == FadeMode.OFF) {
            if (player.volume != 1f) player.volume = 1f
            return
        }
        val length = mode.ms
        val position = player.currentPosition.coerceAtLeast(0)
        var volume = 1f
        // In: over the first stretch of the song (a skip by hand gets a shorter one).
        val into = position - startedAt
        if (into in 0 until length) volume = minOf(volume, ease(into.toFloat() / length))
        // Out: over the last stretch, when another song follows that isn't the album's next track.
        val duration = player.duration
        if (duration > 0 && player.hasNextMediaItem() && player.repeatMode != Player.REPEAT_MODE_ONE) {
            val left = duration - position
            if (left in 0 until length && !sameAlbumNext()) volume = minOf(volume, ease(left.toFloat() / length))
        }
        if (volume != player.volume) player.volume = volume
    }

    private fun ease(t: Float): Float = t.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Straight on from the previous song of the same album: no fade-in either.
        startedAt = if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && lastAlbumMatched) -mode.ms else 0L
        lastAlbumMatched = false
        apply()
    }

    private var lastAlbumMatched = false

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        // Remember whether the song just left was followed by one of its album: the new one starts without a fade-in.
        if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
            val prev = oldPosition.mediaItem?.mediaMetadata?.albumTitle?.toString().orEmpty()
            val now = newPosition.mediaItem?.mediaMetadata?.albumTitle?.toString().orEmpty()
            lastAlbumMatched = prev.isNotBlank() && prev.equals(now, ignoreCase = true)
            startedAt = if (lastAlbumMatched) -mode.ms else 0L
        } else {
            startedAt = 0L
        }
        apply()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        handler.removeCallbacks(tick)
        if (isPlaying) handler.post(tick) else apply()
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        player.removeListener(this)
        player.volume = 1f
        scope.cancel()
    }

    private companion object {
        const val STEP_MS = 50L
    }
}
