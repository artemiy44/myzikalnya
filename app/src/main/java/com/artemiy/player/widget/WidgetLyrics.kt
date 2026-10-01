package com.artemiy.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.artemiy.player.R
import com.artemiy.player.lyrics.LyricLine
import com.artemiy.player.lyrics.LyricVoice
import com.artemiy.player.lyrics.ParsedLyrics
import com.artemiy.player.lyrics.withInstrumentalBreaks

/**
 * One row of the lyrics in a widget: a line (or the translation / background vocals under it).
 * [marks] are the word-synced line's words: when each begins and where, in the text, it ends — what
 * lets the part already sung be coloured while the rest waits.
 */
class WItem(
    val text: String,
    /** When the line begins; -1 for lyrics that aren't timed. */
    val timeMs: Long,
    val marks: List<Pair<Long, Int>>?,
    val alignEnd: Boolean,
    val small: Boolean,
    val seekable: Boolean,
    /** Not sung at all: an instrumental break that lasts until this time, shown as three dots. */
    val breakUntilMs: Long? = null,
)

/** The widget's rows for [lyrics]. */
fun itemsOf(lyrics: ParsedLyrics, gapDots: Boolean = true): List<WItem> = when (lyrics) {
    is ParsedLyrics.Synced -> buildList {
        for (line in withInstrumentalBreaks(lyrics.lines, gapDots)) {
            val end = line.voice == LyricVoice.V2
            val until = line.instrumentalUntilMs
            if (until != null) {
                add(WItem("", line.timeMs, null, end, small = false, seekable = true, breakUntilMs = until))
                continue
            }
            if (line.text.isBlank()) continue
            add(WItem(line.text.trim(), line.timeMs, marksOf(line), end, small = false, seekable = true))
            for (other in line.secondary) {
                if (other.text.isBlank()) continue
                // The other singer at the same moment is a full line on their side; a translation is smaller.
                val otherSinger = other.voice != null && other.voice != line.voice
                add(WItem(other.text.trim(), other.timeMs, if (otherSinger) marksOf(other) else null, (other.voice ?: line.voice) == LyricVoice.V2, small = !otherSinger, seekable = false))
            }
            line.background?.takeIf { it.text.isNotBlank() }?.let { bg ->
                add(WItem(bg.text.trim(), bg.timeMs, null, end, small = true, seekable = false))
            }
        }
    }
    is ParsedLyrics.Unsynced -> lyrics.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        .map { WItem(it, -1, null, false, small = false, seekable = false) }
}

private fun marksOf(line: LyricLine): List<Pair<Long, Int>>? {
    val words = line.words?.takeIf { it.isNotEmpty() } ?: return null
    val whole = words.joinToString("") { it.text }
    val lead = whole.length - whole.trimStart().length
    var upTo = 0
    return words.map { word ->
        upTo += word.text.length
        word.timeMs to (upTo - lead).coerceIn(0, line.text.length)
    }
}

/** Which row is being sung at [positionMs]: the last main line that has begun; -1 before the first or without timing. */
fun activeRow(items: List<WItem>, positionMs: Long): Int {
    var found = -1
    for (i in items.indices) {
        val it = items[i]
        if (it.small || it.timeMs < 0) continue
        if (it.timeMs <= positionMs) found = i else break
    }
    return found
}

/** When the lyrics next change on screen — the next line or the next word of the one being sung — or null. */
fun nextLyricEvent(items: List<WItem>, positionMs: Long): Long? {
    var best: Long? = null
    fun consider(t: Long) {
        if (t > positionMs && (best == null || t < best!!)) best = t
    }
    val active = activeRow(items, positionMs)
    items.forEach { if (!it.small && it.timeMs >= 0) consider(it.timeMs) }
    if (active >= 0) {
        items[active].marks?.forEach { consider(it.first) }
        // A break: the dots fill little by little, and fold away just before the singing starts.
        items[active].breakUntilMs?.let { until ->
            val start = items[active].timeMs
            for (step in 1..DOT_STEPS) consider(start + (until - start) * step / DOT_STEPS)
            consider(until - DOTS_FOLD_EARLY_MS)
        }
    }
    return best
}

/**
 * The colours a widget's lyrics are drawn in — by brightness, not by hue (a wallpaper without much
 * colour makes grey accents): the part already sung is at full strength, the rest of that line a bit
 * under, every other line under half.
 */
class LyricColors(val dim: Int, val text: Int, val sung: Int)

private const val LAYOUTS = 4

/** How many times the dots of a break are redrawn while they fill. */
private const val DOT_STEPS = 9

/** The dots fold away this long before the singing starts again (as in the player). */
private const val DOTS_FOLD_EARLY_MS = 450L

/** The rows as views, with the one being sung coloured (its sung part in [LyricColors.sung]). */
fun lyricViews(context: Context, items: List<WItem>, positionMs: Long, colors: LyricColors): List<RemoteViews> {
    val active = activeRow(items, positionMs)
    return items.mapIndexed { index, item ->
        val layout = when {
            item.small && item.alignEnd -> R.layout.widget_lyric_item_right_small
            item.small -> R.layout.widget_lyric_item_left_small
            item.alignEnd -> R.layout.widget_lyric_item_right
            else -> R.layout.widget_lyric_item_left
        }
        val rv = RemoteViews(context.packageName, layout)
        val isActive = index == active
        val breakUntil = item.breakUntilMs
        if (breakUntil != null) {
            // Three dots that take no room until it's their turn, then fill one after another.
            val showing = isActive && positionMs < breakUntil - DOTS_FOLD_EARLY_MS
            if (showing) {
                val progress = ((positionMs - item.timeMs).toFloat() / (breakUntil - item.timeMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                val dots = SpannableString("●  ●  ●")
                for (i in 0 until 3) {
                    val lit = (progress * 3f - i).coerceIn(0f, 1f)
                    val alpha = ((0.3f + 0.7f * lit) * 255).toInt()
                    dots.setSpan(ForegroundColorSpan((colors.sung and 0x00FFFFFF) or (alpha shl 24)), i * 3, i * 3 + 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }
                rv.setTextViewText(R.id.lyric_item, dots)
                rv.setTextViewTextSize(R.id.lyric_item, android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
                rv.setViewPadding(R.id.lyric_item, 0, (8 * context.resources.displayMetrics.density).toInt(), 0, (8 * context.resources.displayMetrics.density).toInt())
            } else {
                rv.setTextViewText(R.id.lyric_item, "")
                rv.setTextViewTextSize(R.id.lyric_item, android.util.TypedValue.COMPLEX_UNIT_SP, 1f)
                rv.setViewPadding(R.id.lyric_item, 0, 0, 0, 0)
            }
            if (item.timeMs >= 0) rv.setOnClickFillInIntent(R.id.lyric_item, Intent().putExtra(EXTRA_POSITION, item.timeMs))
            return@mapIndexed rv
        }
        val timed = item.timeMs >= 0
        // Lyrics that aren't timed are all just read; timed ones: the one being sung stands out.
        rv.setTextColor(R.id.lyric_item, if (isActive) colors.text else if (!timed) colors.sung else colors.dim)
        // The line being sung is a size bigger as well, so it can't be missed.
        if (!item.small) rv.setTextViewTextSize(R.id.lyric_item, android.util.TypedValue.COMPLEX_UNIT_SP, if (isActive) 22f else 17f)
        val text = SpannableString(item.text)
        if (isActive) {
            val sungUpTo = item.marks?.lastOrNull { it.first <= positionMs }?.second ?: if (item.marks == null) text.length else 0
            if (sungUpTo > 0) text.setSpan(ForegroundColorSpan(colors.sung), 0, sungUpTo.coerceAtMost(text.length), Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }
        rv.setTextViewText(R.id.lyric_item, text)
        if (item.seekable && item.timeMs >= 0) {
            rv.setOnClickFillInIntent(R.id.lyric_item, Intent().putExtra(EXTRA_POSITION, item.timeMs))
        }
        rv
    }
}

/** Puts the lyrics list into [rv]'s ListView ([listId]), scrolled to the line being sung. */
fun setLyricList(context: Context, rv: RemoteViews, widgetId: Int, listId: Int, emptyId: Int, emptyText: CharSequence, snap: WidgetSnapshot, colors: LyricColors, dark: Boolean, heightDp: Int) {
    val items = snap.items.orEmpty()
    val template = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, WidgetActionReceiver::class.java).setAction(ACTION_SEEK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
    rv.setPendingIntentTemplate(listId, template)
    rv.setTextViewText(emptyId, emptyText)
    rv.setEmptyView(listId, emptyId)
    if (Build.VERSION.SDK_INT >= 31) {
        val views = lyricViews(context, items, snap.positionMs, colors)
        rv.setRemoteAdapter(
            listId,
            RemoteViews.RemoteCollectionItems.Builder()
                .setViewTypeCount(LAYOUTS)
                .setHasStableIds(false)
                .apply { views.forEachIndexed { i, v -> addItem(i.toLong(), v) } }
                .build(),
        )
    } else {
        @Suppress("DEPRECATION")
        rv.setRemoteAdapter(
            listId,
            Intent(context, LyricWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("dark", dark)
                // Intents are compared without their extras: the data keeps one widget's list from being taken for another's.
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            },
        )
    }
    val active = activeRow(items, snap.positionMs)
    if (active >= 0) {
        // The system scrolls only until the row asked for is in view — at the bottom edge. So the
        // row asked for is one a few rows further on, which leaves the line being sung around the
        // middle; after a jump back it's the line itself, which then lands at the top.
        val rows = ((heightDp - 16) / 38).coerceAtLeast(2)
        val ahead = ((rows - 1) / 2).coerceAtLeast(1)
        val before = lastActive[widgetId]
        lastActive[widgetId] = active
        val target = if (before != null && active < before) active else active + ahead
        rv.setScrollPosition(listId, target.coerceAtMost(items.lastIndex))
    }
}

/** The line each widget was last scrolled to, to tell going on from jumping back. */
private val lastActive = HashMap<Int, Int>()

fun lyricColors(context: Context, dark: Boolean): LyricColors =
    (if (dark) 0xFFFFFFFF.toInt() else context.getColor(R.color.widget_active)).let { full ->
        fun strength(percent: Int) = (full and 0x00FFFFFF) or ((255 * percent / 100) shl 24)
        LyricColors(dim = strength(40), text = strength(70), sung = full)
    }

/** Before Android 12 a widget's list comes from a service like this one. */
class LyricWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        LyricFactory(applicationContext, intent.getBooleanExtra("dark", false))

    private class LyricFactory(private val context: Context, private val dark: Boolean) : RemoteViewsFactory {
        private var views: List<RemoteViews> = emptyList()

        override fun onCreate() = onDataSetChanged()

        override fun onDataSetChanged() {
            val snap = WidgetState.snapshot
            views = lyricViews(context, snap.items.orEmpty(), snap.positionMs, lyricColors(context, dark))
        }

        override fun onDestroy() {}
        override fun getCount(): Int = views.size
        override fun getViewAt(position: Int): RemoteViews? = views.getOrNull(position)
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = LAYOUTS
        override fun getItemId(position: Int): Long = position.toLong()
        override fun hasStableIds(): Boolean = false
    }
}
