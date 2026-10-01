package com.artemiy.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.artemiy.player.MainActivity
import com.artemiy.player.R
import com.artemiy.player.ui.i18n.withAppLanguage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** What the widgets show of the playing song. */
class WidgetSnapshot(
    val hasSong: Boolean = false,
    val songId: Long = -1,
    val title: String = "",
    val artist: String = "",
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    /** The lyrics' rows; null: no lyrics (yet). */
    val items: List<WItem>? = null,
    val lyricsLoading: Boolean = false,
)

/** Kept in the process, written by the playback service, read whenever a widget is drawn. */
object WidgetState {
    @Volatile var snapshot = WidgetSnapshot()
    /** The playing song's cover, as the service loaded it (null: none). */
    @Volatile var cover: Bitmap? = null
}

const val ACTION_PLAY_PAUSE = "lumine.widget.play_pause"
const val ACTION_NEXT = "lumine.widget.next"
const val ACTION_PREV = "lumine.widget.prev"
const val ACTION_SEEK = "lumine.widget.seek"
const val ACTION_TOGGLE_INFO = "lumine.widget.toggle_info"
const val ACTION_TOGGLE_LYRICS = "lumine.widget.toggle_lyrics"
const val EXTRA_POSITION = "position"
const val EXTRA_WIDGET_ID = "widget_id"

object Widgets {
    private val providers = listOf(CoverWidget::class.java, LyricsWidget::class.java, ControlsWidget::class.java)

    private fun prefs(context: Context) = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    private fun ids(context: Context, cls: Class<*>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, cls))

    fun any(context: Context): Boolean = providers.any { ids(context, it).isNotEmpty() }

    /** Whether any widget currently shows the lyrics (and so needs them loaded and followed). */
    fun wantsLyrics(context: Context): Boolean =
        ids(context, LyricsWidget::class.java).isNotEmpty() ||
            ids(context, ControlsWidget::class.java).any { prefs(context).getBoolean("lyrics_$it", false) }

    /** Whether any widget shows the song's progress. */
    fun wantsProgress(context: Context): Boolean =
        ids(context, CoverWidget::class.java).any { prefs(context).getBoolean("info_$it", false) }

    fun flip(context: Context, key: String) {
        prefs(context).edit().putBoolean(key, !prefs(context).getBoolean(key, false)).apply()
    }

    fun forget(context: Context, id: Int) {
        prefs(context).edit().remove("info_$id").remove("lyrics_$id").apply()
    }

    fun updateAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        for (id in ids(context, CoverWidget::class.java)) mgr.updateAppWidget(id, buildCover(context, mgr, id))
        for (id in ids(context, LyricsWidget::class.java)) mgr.updateAppWidget(id, buildLyrics(context, mgr, id))
        for (id in ids(context, ControlsWidget::class.java)) mgr.updateAppWidget(id, buildControls(context, mgr, id))
        // Before Android 12 a widget's list is read from a service: tell it to read again.
        if (android.os.Build.VERSION.SDK_INT < 31) {
            @Suppress("DEPRECATION")
            run {
                mgr.notifyAppWidgetViewDataChanged(ids(context, LyricsWidget::class.java), R.id.list_view)
                mgr.notifyAppWidgetViewDataChanged(ids(context, ControlsWidget::class.java), R.id.list_view)
            }
        }
    }

    fun update(context: Context, mgr: AppWidgetManager, id: Int, cls: Class<*>) {
        val views = when (cls) {
            CoverWidget::class.java -> buildCover(context, mgr, id)
            LyricsWidget::class.java -> buildLyrics(context, mgr, id)
            else -> buildControls(context, mgr, id)
        }
        mgr.updateAppWidget(id, views)
    }

    // ---- taps ----

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun tap(context: Context, action: String, key: String, extras: Intent.() -> Unit = {}): PendingIntent {
        val intent = Intent(context, WidgetActionReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("lumine://widget/$key"))
            .apply(extras)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // ---- sizes and pictures ----

    private fun dp(context: Context, v: Float) = v * context.resources.displayMetrics.density

    /** The widget's current size in dp (as the home screen has it upright). */
    private fun sizeDp(mgr: AppWidgetManager, id: Int): Pair<Int, Int> {
        val o = mgr.getAppWidgetOptions(id)
        return o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110) to o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110)
    }

    /** [src] cut to the aspect ratio of [wDp]×[hDp], kept small enough for a widget, with rounded corners. */
    private fun fitted(context: Context, src: Bitmap, wDp: Int, hDp: Int, radiusDp: Float): Bitmap {
        val aspect = wDp.toFloat() / hDp
        val cropW: Int
        val cropH: Int
        if (src.width.toFloat() / src.height > aspect) { cropH = src.height; cropW = (src.height * aspect).roundToInt() } else { cropW = src.width; cropH = (src.width / aspect).roundToInt() }
        val left = (src.width - cropW) / 2
        val top = (src.height - cropH) / 2
        val scale = min(1f, MAX_SIDE / max(cropW, cropH).toFloat())
        val w = max(1, (cropW * scale).roundToInt())
        val h = max(1, (cropH * scale).roundToInt())
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val radius = dp(context, radiusDp) * (w.toFloat() / dp(context, wDp.toFloat())).coerceAtMost(1f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, android.graphics.Rect(left, top, left + cropW, top + cropH), RectF(0f, 0f, w.toFloat(), h.toFloat()), paint)
        return out
    }

    private const val MAX_SIDE = 340f

    // ---- widget 1: the cover ----

    private fun buildCover(context: Context, mgr: AppWidgetManager, id: Int): RemoteViews {
        val s = WidgetState.snapshot
        val words = context.withAppLanguage()
        val rv = RemoteViews(context.packageName, R.layout.widget_cover)
        val (w, h) = sizeDp(mgr, id)
        val cover = WidgetState.cover
        if (s.hasSong && cover != null) rv.setImageViewBitmap(R.id.w_cover, fitted(context, cover, w, h, 22f))
        else rv.setImageViewResource(R.id.w_cover, R.drawable.widget_idle)

        val info = s.hasSong && prefs(context).getBoolean("info_$id", false)
        val paused = s.hasSong && !s.playing
        rv.setViewVisibility(R.id.w_dim, if (paused && !info) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.w_state, if (paused && !info) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.w_hint_prev, if (s.hasSong) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.w_hint_next, if (s.hasSong) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.w_gradient, if (info) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.w_info, if (info) View.VISIBLE else View.GONE)
        if (info) {
            rv.setTextViewText(R.id.w_title, s.title)
            rv.setTextViewText(R.id.w_artist, s.artist)
            rv.setProgressBar(R.id.w_progress, 1000, if (s.durationMs > 0) (s.positionMs * 1000 / s.durationMs).toInt().coerceIn(0, 1000) else 0, false)
        }
        if (s.hasSong) {
            rv.setOnClickPendingIntent(R.id.z_prev, tap(context, ACTION_PREV, "prev"))
            rv.setOnClickPendingIntent(R.id.z_play, tap(context, ACTION_PLAY_PAUSE, "play"))
            rv.setOnClickPendingIntent(R.id.z_next, tap(context, ACTION_NEXT, "next"))
            rv.setOnClickPendingIntent(R.id.z_info, tap(context, ACTION_TOGGLE_INFO, "info$id") { putExtra(EXTRA_WIDGET_ID, id) })
        } else {
            val open = openApp(context)
            for (zone in intArrayOf(R.id.z_prev, R.id.z_play, R.id.z_next, R.id.z_info)) rv.setOnClickPendingIntent(zone, open)
            rv.setContentDescription(R.id.w_cover, words.getString(R.string.widget_idle))
        }
        return rv
    }

    // ---- widget 2: the lyrics ----

    /** What to say where the list is empty: nothing playing, loading, or no lyrics. */
    private fun emptyText(context: Context, s: WidgetSnapshot): String {
        val words = context.withAppLanguage()
        return when {
            !s.hasSong -> words.getString(R.string.widget_idle)
            s.lyricsLoading -> words.getString(R.string.widget_loading_lyrics)
            else -> "${s.title}\n${s.artist}\n\n" + words.getString(R.string.widget_no_lyrics)
        }
    }

    private fun buildLyrics(context: Context, mgr: AppWidgetManager, id: Int): RemoteViews {
        val s = WidgetState.snapshot
        val rv = RemoteViews(context.packageName, R.layout.widget_lyrics)
        rv.setOnClickPendingIntent(R.id.w_root, openApp(context))
        setLyricList(context, rv, id, R.id.list_view, R.id.empty_view, emptyText(context, s), s, lyricColors(context, dark = false), dark = false)
        return rv
    }

    // ---- widget 3: cover and controls ----

    private fun buildControls(context: Context, mgr: AppWidgetManager, id: Int): RemoteViews {
        val s = WidgetState.snapshot
        val rv = RemoteViews(context.packageName, R.layout.widget_controls)
        val (_, h) = sizeDp(mgr, id)
        val side = max(60, h - 16)
        val cover = WidgetState.cover
        if (s.hasSong && cover != null) rv.setImageViewBitmap(R.id.w_cover, fitted(context, cover, side, side, 16f))
        else rv.setImageViewResource(R.id.w_cover, R.drawable.widget_idle)

        val lyricsMode = s.hasSong && prefs(context).getBoolean("lyrics_$id", false)
        rv.setViewVisibility(R.id.w_controls_panel, if (lyricsMode) View.GONE else View.VISIBLE)
        rv.setViewVisibility(R.id.w_lyrics_panel, if (lyricsMode) View.VISIBLE else View.GONE)
        val words = context.withAppLanguage()
        val open = openApp(context)
        if (s.hasSong) {
            rv.setOnClickPendingIntent(R.id.w_cover, tap(context, ACTION_TOGGLE_LYRICS, "lyrics$id") { putExtra(EXTRA_WIDGET_ID, id) })
        } else {
            rv.setOnClickPendingIntent(R.id.w_cover, open)
        }
        if (!lyricsMode) {
            rv.setTextViewText(R.id.w_title, if (s.hasSong) s.title else "Lumine")
            rv.setTextViewText(R.id.w_artist, if (s.hasSong) s.artist else words.getString(R.string.widget_idle))
            rv.setImageViewResource(R.id.w_play, if (s.playing) R.drawable.ic_w_pause else R.drawable.ic_w_play)
            if (s.hasSong) {
                rv.setOnClickPendingIntent(R.id.w_prev, tap(context, ACTION_PREV, "prev"))
                rv.setOnClickPendingIntent(R.id.w_play, tap(context, ACTION_PLAY_PAUSE, "play"))
                rv.setOnClickPendingIntent(R.id.w_next, tap(context, ACTION_NEXT, "next"))
            } else {
                for (b in intArrayOf(R.id.w_prev, R.id.w_play, R.id.w_next)) rv.setOnClickPendingIntent(b, open)
            }
        } else {
            setLyricList(context, rv, id, R.id.list_view, R.id.empty_view, emptyText(context, s), s, lyricColors(context, dark = true), dark = true)
        }
        return rv
    }
}
