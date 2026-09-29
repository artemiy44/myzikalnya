package com.artemiy.player.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.artemiy.player.MainActivity
import com.artemiy.player.ui.i18n.withAppLanguage
import com.artemiy.player.playback.alac.AlacRenderer

/** Adds software ALAC decoding — Android's platform MediaCodec has none on most devices. */
private class PlayerRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>,
    ) {
        out.add(AlacRenderer(eventHandler, eventListener, audioSink))
        super.buildAudioRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            enableDecoderFallback,
            audioSink,
            eventHandler,
            eventListener,
            out,
        )
    }
}

class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        // Some MP3s ship a Xing/VBR seek header that doesn't match the real stream size
        // (seen in logs as "Data size mismatch... using Xing value") — trusting it can throw
        // off seeking. Constant-bitrate seeking sidesteps that header entirely.
        val extractorsFactory = DefaultExtractorsFactory()
            .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING_ALWAYS)

        player = ExoPlayer.Builder(this, PlayerRenderersFactory(this))
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractorsFactory))
            .build()

        val sessionActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(SessionCallback())
            .build()

        // The app's own burst in the corner of the system media player (and in the status bar),
        // instead of the library's stock note.
        setMediaNotificationProvider(
            androidx.media3.session.DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(com.artemiy.player.R.drawable.ic_notification)
            },
        )
    }

    // ---- Shuffle and endless play buttons in the system media player ----
    //
    // Both modes live in the app (shuffle reorders the queue its own way; endless play tops the
    // queue up from the library), so a tap on one of these buttons is passed on to the app, which
    // toggles the mode and then tells this service how things stand, to redraw the buttons.

    private var shuffleOn = false
    private var endlessOn = false
    private var repeatOn = false
    /** The second button: repeat instead of endless play (a setting). */
    private var secondIsRepeat = false

    private fun modeButtons(): com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton> {
        val words = withAppLanguage()
        return com.google.common.collect.ImmutableList.of(
            androidx.media3.session.CommandButton.Builder()
                .setDisplayName(words.getString(com.artemiy.player.R.string.shuffle))
                .setIconResId(if (shuffleOn) com.artemiy.player.R.drawable.ic_notif_shuffle_on else com.artemiy.player.R.drawable.ic_notif_shuffle_off)
                .setSessionCommand(androidx.media3.session.SessionCommand(COMMAND_TOGGLE_SHUFFLE, android.os.Bundle.EMPTY))
                .build(),
            if (secondIsRepeat) {
                androidx.media3.session.CommandButton.Builder()
                    .setDisplayName(words.getString(com.artemiy.player.R.string.repeat))
                    .setIconResId(if (repeatOn) com.artemiy.player.R.drawable.ic_notif_repeat_on else com.artemiy.player.R.drawable.ic_notif_repeat_off)
                    .setSessionCommand(androidx.media3.session.SessionCommand(COMMAND_TOGGLE_REPEAT, android.os.Bundle.EMPTY))
                    .build()
            } else {
                androidx.media3.session.CommandButton.Builder()
                    .setDisplayName(words.getString(com.artemiy.player.R.string.endless_play))
                    .setIconResId(if (endlessOn) com.artemiy.player.R.drawable.ic_notif_endless_on else com.artemiy.player.R.drawable.ic_notif_endless_off)
                    .setSessionCommand(androidx.media3.session.SessionCommand(COMMAND_TOGGLE_ENDLESS, android.os.Bundle.EMPTY))
                    .build()
            },
        )
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(androidx.media3.session.SessionCommand(COMMAND_TOGGLE_SHUFFLE, android.os.Bundle.EMPTY))
                .add(androidx.media3.session.SessionCommand(COMMAND_TOGGLE_ENDLESS, android.os.Bundle.EMPTY))
                .add(androidx.media3.session.SessionCommand(COMMAND_TOGGLE_REPEAT, android.os.Bundle.EMPTY))
                .add(androidx.media3.session.SessionCommand(COMMAND_MODES, android.os.Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .setCustomLayout(modeButtons())
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: androidx.media3.session.SessionCommand,
            args: android.os.Bundle,
        ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
            when (customCommand.customAction) {
                // From the app: how the modes stand now.
                COMMAND_MODES -> {
                    shuffleOn = args.getBoolean(KEY_SHUFFLE)
                    endlessOn = args.getBoolean(KEY_ENDLESS)
                    repeatOn = args.getBoolean(KEY_REPEAT)
                    secondIsRepeat = args.getBoolean(KEY_SECOND_IS_REPEAT)
                    session.setCustomLayout(modeButtons())
                }
                // From the system media player: passed on to the app.
                COMMAND_TOGGLE_SHUFFLE, COMMAND_TOGGLE_ENDLESS, COMMAND_TOGGLE_REPEAT -> session.broadcastCustomCommand(customCommand, args)
            }
            return com.google.common.util.concurrent.Futures.immediateFuture(
                androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS),
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession.player
        if (!p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}

internal const val COMMAND_TOGGLE_SHUFFLE = "lumine.toggle_shuffle"
internal const val COMMAND_TOGGLE_ENDLESS = "lumine.toggle_endless"
internal const val COMMAND_MODES = "lumine.modes"
internal const val KEY_SHUFFLE = "shuffle"
internal const val KEY_ENDLESS = "endless"
internal const val COMMAND_TOGGLE_REPEAT = "lumine.toggle_repeat"
internal const val KEY_REPEAT = "repeat"
internal const val KEY_SECOND_IS_REPEAT = "second_is_repeat"
