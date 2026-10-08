package com.artemiy.player.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.artemiy.player.R
import com.artemiy.player.data.Song
import com.artemiy.player.ui.icons.AppIcons
import kotlinx.coroutines.launch

/** What swiping a song in a list does: to the right puts it in the queue, to the left asks to delete it. */
class SongSwipeActions(val addToQueue: (Song) -> Unit, val delete: (Song) -> Unit)

/** Set once for the tabs; a list without it (or with swiping switched off) just shows its rows. */
val LocalSongSwipeActions = staticCompositionLocalOf<SongSwipeActions?> { null }

private val QueueViolet = Color(0xFF7B6A99)
private val DeleteRed = Color(0xFFD64545)

/**
 * A song row you can swipe: right (a muted violet strip, "To queue") adds it to the queue, left (a
 * red strip, "Delete") asks to delete it. The row always springs back — the answer to the question
 * is the page's to give. Built like the queue's swipe-to-remove: a strip that only fills the part
 * already uncovered, the label staying at its edge, and nothing heavier than one drag listener.
 */
@Composable
fun SwipeSongRow(song: Song, enabled: Boolean = true, content: @Composable () -> Unit) {
    val actions = LocalSongSwipeActions.current
    if (actions == null || !enabled) {
        content()
        return
    }
    var dragPx by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    var armed by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val threshold = with(density) { SWIPE_COMMIT.toPx() }
    val dragState = rememberDraggableState { delta ->
        dragPx = (dragPx + delta).coerceIn(-widthPx * 0.9f, widthPx * 0.9f)
        val now = when {
            dragPx > threshold -> 1
            dragPx < -threshold -> -1
            else -> 0
        }
        if (now != armed) {
            armed = now
            if (now != 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    Box(modifier = Modifier.onSizeChanged { widthPx = it.width }) {
        val shown = kotlin.math.abs(dragPx)
        if (shown > 0f) {
            val toQueue = dragPx > 0f
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .wrapContentWidth(if (toQueue) Alignment.Start else Alignment.End)
                    .fillMaxHeight()
                    .width(with(density) { shown.toDp() })
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (toQueue) QueueViolet else DeleteRed),
                contentAlignment = if (toQueue) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                // Measured at full width however narrow the strip is, so it isn't squeezed; the
                // strip's clip uncovers it.
                Row(
                    modifier = Modifier
                        .wrapContentWidth(if (toQueue) Alignment.Start else Alignment.End, unbounded = true)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (toQueue) {
                        Icon(AppIcons.AddToQueue, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(
                            text = stringResource(R.string.swipe_to_queue),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.delete),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Icon(AppIcons.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(dragPx.toInt(), 0) }
                .horizontalSwipe(
                    onDrag = { delta -> dragState.dispatchRawDelta(delta) },
                    onCancel = {
                        scope.launch {
                            animate(dragPx, 0f) { value, _ -> dragPx = value }
                            armed = 0
                        }
                    },
                    onEnd = { velocity ->
                        val flickMin = with(density) { SWIPE_FLICK_MIN.toPx() }
                        val flickSpeed = with(density) { SWIPE_FLICK_SPEED.toPx() }
                        val toQueue = dragPx > threshold || (dragPx > flickMin && velocity > flickSpeed)
                        val toDelete = dragPx < -threshold || (dragPx < -flickMin && velocity < -flickSpeed)
                        scope.launch {
                            animate(dragPx, 0f) { value, _ -> dragPx = value }
                            armed = 0
                            if (toQueue) actions.addToQueue(song) else if (toDelete) actions.delete(song)
                        }
                    },
                ),
        ) {
            content()
        }
    }
}
