package com.artemiy.player.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.artemiy.player.playback.PlaybackService

/** Widget 1: the cover alone. */
class CoverWidget : BaseWidget()

/** Widget 2: the lyrics, resizable. */
class LyricsWidget : BaseWidget()

/** Widget 3: cover on the left, controls or lyrics on the right. */
class ControlsWidget : BaseWidget()

abstract class BaseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) Widgets.update(context, appWidgetManager, id, javaClass)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        Widgets.update(context, appWidgetManager, appWidgetId, javaClass)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { Widgets.forget(context, it) }
    }

    /** A widget was put on the screen: the service may not be running, so it learns of it by being asked. */
    override fun onEnabled(context: Context) {
        WidgetUpdater.current?.refresh()
    }
}

/** The taps on the widgets: switches inside a widget are done here, play / skip / jump go to the player. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
        when (intent.action) {
            ACTION_TOGGLE_INFO -> { Widgets.flip(context, "info_$id"); Widgets.updateAll(context); WidgetUpdater.current?.refresh() }
            ACTION_TOGGLE_LYRICS -> { Widgets.flip(context, "lyrics_$id"); Widgets.updateAll(context); WidgetUpdater.current?.refresh() }
            ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREV, ACTION_SEEK -> control(context, intent)
        }
    }

    private fun control(context: Context, intent: Intent) {
        val pending = goAsync()
        // A receiver's own context may not bind to services; the application's may.
        val app = context.applicationContext
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                when (intent.action) {
                    ACTION_PLAY_PAUSE -> if (c.isPlaying) c.pause() else c.play()
                    ACTION_NEXT -> c.seekToNextMediaItem()
                    ACTION_PREV -> c.seekToPrevious()
                    ACTION_SEEK -> c.seekTo(intent.getLongExtra(EXTRA_POSITION, 0L))
                }
            } catch (_: Exception) {
            } finally {
                MediaController.releaseFuture(future)
                pending.finish()
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
