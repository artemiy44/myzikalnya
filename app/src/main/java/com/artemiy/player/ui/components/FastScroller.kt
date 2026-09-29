package com.artemiy.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.ui.theme.LocalBarsInset
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.expressiveUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** What the fast scroller needs from a list or a grid. */
class ScrollTarget internal constructor(
    val firstIndex: () -> Int,
    val total: () -> Int,
    val visible: () -> Int,
    val scrolling: () -> Boolean,
    val scrollTo: suspend (Int) -> Unit,
)

@Composable
fun rememberScrollTarget(state: LazyListState): ScrollTarget = remember(state) {
    ScrollTarget(
        { state.firstVisibleItemIndex },
        { state.layoutInfo.totalItemsCount },
        { state.layoutInfo.visibleItemsInfo.size },
        { state.isScrollInProgress },
        { state.scrollToItem(it) },
    )
}

@Composable
fun rememberScrollTarget(state: LazyGridState): ScrollTarget = remember(state) {
    ScrollTarget(
        { state.firstVisibleItemIndex },
        { state.layoutInfo.totalItemsCount },
        { state.layoutInfo.visibleItemsInfo.size },
        { state.isScrollInProgress },
        { state.scrollToItem(it) },
    )
}

/**
 * A thumb along the right edge of a long list: where you are in it, and a handle to fly anywhere
 * in it by dragging. Out of sight until the list moves; gone again two seconds after it stops.
 * While it's dragged, a pill beside it says where you've got to — [label] of the first item in
 * view: a letter, a year, "September 2026"… (null: no pill).
 */
@Composable
fun BoxScope.FastScroller(target: ScrollTarget, label: (Int) -> String? = { null }) {
    val total = target.total()
    if (total < MIN_ITEMS) return
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val expressive = expressiveUi
    var dragging by remember { mutableStateOf(false) }
    val active = target.scrolling() || dragging
    val shown = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            shown.animateTo(1f, tween(150))
        } else {
            delay(HIDE_AFTER_MS)
            shown.animateTo(0f, tween(300))
        }
    }
    val touchable = active || shown.value > 0.05f
    var trackHeight by remember { mutableIntStateOf(0) }
    val thumbPx = with(density) { THUMB_HEIGHT.roundToPx() }
    val topPx = with(density) { EDGE_GAP.roundToPx() }
    fun fraction(): Float {
        val room = (target.total() - target.visible()).coerceAtLeast(1)
        return (target.firstIndex().toFloat() / room).coerceIn(0f, 1f)
    }
    val thumbWidth by animateDpAsState(
        when {
            dragging -> if (expressive) 10.dp else 8.dp
            expressive -> 6.dp
            else -> 4.dp
        },
        label = "thumbWidth",
    )
    var jump by remember { mutableStateOf<Job?>(null) }
    fun scrollToY(y: Float) {
        val f = ((y - thumbPx / 2f) / (trackHeight - thumbPx).coerceAtLeast(1)).coerceIn(0f, 1f)
        val index = (f * (target.total() - 1)).roundToInt()
        jump?.cancel()
        jump = scope.launch { target.scrollTo(index) }
    }

    // The track: a narrow strip down the right edge, touchable only while the thumb is showing —
    // otherwise a swipe near the edge just scrolls the list, as ever.
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .fillMaxHeight()
            .width(TOUCH_WIDTH)
            .padding(top = EDGE_GAP, bottom = EDGE_GAP + LocalBarsInset.current)
            .onSizeChanged { trackHeight = it.height }
            .then(
                if (touchable) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragging = true; scrollToY(it.y) },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                        ) { change, _ ->
                            change.consume()
                            scrollToY(change.position.y)
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 3.dp)
                .offset { IntOffset(0, ((trackHeight - thumbPx) * fraction()).roundToInt()) }
                .graphicsLayer { alpha = shown.value }
                .width(thumbWidth)
                .height(THUMB_HEIGHT)
                .clip(CircleShape)
                .background(if (expressive) PlayerColors.AccentStandalone else PlayerColors.TextSecondary),
        )
    }

    // The pill: level with the thumb, just left of it, while it's dragged.
    val text = if (dragging) label(target.firstIndex()) else null
    if (text != null) {
        var pillHeight by remember { mutableIntStateOf(0) }
        Text(
            text = text,
            color = PlayerColors.Background,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val thumbTop = topPx + ((trackHeight - thumbPx) * fraction()).roundToInt()
                    IntOffset(-(TOUCH_WIDTH + 10.dp).roundToPx(), thumbTop + thumbPx / 2 - pillHeight / 2)
                }
                .onSizeChanged { pillHeight = it.height }
                .clip(if (expressive) CircleShape else RoundedCornerShape(10.dp))
                .background(if (expressive) PlayerColors.AccentStandalone else PlayerColors.TextPrimary)
                .padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

/** A letter to show for [text] in the pill: its first letter or digit, uppercase; "#" otherwise. */
fun indexLetter(text: String): String =
    text.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "#"

/** "September 2026" (in the app's language) for a moment in time — the pill of a list by date. */
fun monthYear(millis: Long, locale: java.util.Locale = java.util.Locale.getDefault()): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "LLLLyyyy")
    return java.text.SimpleDateFormat(pattern, locale).format(java.util.Date(millis))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

private const val MIN_ITEMS = 40
private const val HIDE_AFTER_MS = 2_000L
private val TOUCH_WIDTH = 20.dp
private val THUMB_HEIGHT = 48.dp
private val EDGE_GAP = 8.dp
