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
)

/** The widget's rows for [lyrics]. */
fun itemsOf(lyrics: ParsedLyrics): List<WItem> = when (lyrics) {
    is ParsedLyrics.Synced -> buildList {
        for (line in lyrics.lines) {
            if (line.text.isBlank()) continue
            val end = line.voice == LyricVoice.V2
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
    if (active >= 0) items[active].marks?.forEach { consider(it.first) }
    return best
}

/** The colours a widget's lyrics are drawn in. */
class LyricColors(val dim: Int, val text: Int, val sung: Int)

private const val LAYOUTS = 4

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
        val timed = item.timeMs >= 0
        // Lyrics that aren't timed are all just read; timed ones: the one being sung stands out.
        rv.setTextColor(R.id.lyric_item, if (isActive || !timed) colors.text else colors.dim)
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
fun setLyricList(context: Context, rv: RemoteViews, widgetId: Int, listId: Int, emptyId: Int, emptyText: CharSequence, snap: WidgetSnapshot, colors: LyricColors, dark: Boolean) {
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
    if (active >= 0) rv.setScrollPosition(listId, (active - 1).coerceAtLeast(0))
}

fun lyricColors(context: Context, dark: Boolean): LyricColors =
    if (dark) LyricColors(dim = 0x99FFFFFF.toInt(), text = 0xFFFFFFFF.toInt(), sung = context.getColor(R.color.widget_card_sung))
    else LyricColors(dim = context.getColor(R.color.widget_text_dim), text = context.getColor(R.color.widget_text), sung = context.getColor(R.color.widget_accent))

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
