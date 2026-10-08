package com.artemiy.player.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.dp

/** How far a row must be pulled for a swipe to count. */
val SWIPE_COMMIT = 72.dp

/** A fast flick counts too, but only after at least this much pull — a finger drifting sideways
 * while a list is being scrolled quickly must never add up to "a swipe". */
val SWIPE_FLICK_MIN = 40.dp
val SWIPE_FLICK_SPEED = 700.dp

/**
 * A sideways drag that starts only when the finger really moves sideways (clearly more across than
 * up/down) — a mostly-vertical movement is left to the list that is scrolling — and that gives up
 * quietly if something else takes the touch. [onEnd] gets the speed (px/s, + to the right) when the
 * finger lifts.
 */
fun Modifier.horizontalSwipe(
    onDrag: (deltaX: Float) -> Unit,
    onEnd: (velocityX: Float) -> Unit,
    onCancel: () -> Unit = {},
    enabled: Boolean = true,
): Modifier = if (!enabled) this else pointerInput(Unit) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
        val tracker = VelocityTracker()
        tracker.addPointerInputChange(down)
        var total = Offset.Zero
        // 0 = not decided yet, 1 = sideways (ours)
        var state = 0
        var finished = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            // Taken by someone else before we claimed it (the list scrolling): not ours.
            if (state == 0 && change.isConsumed) break
            val delta = change.positionChange()
            if (state == 0) {
                total += delta
                if (total.getDistance() > slop) {
                    if (kotlin.math.abs(total.x) > 2.2f * kotlin.math.abs(total.y)) {
                        state = 1
                        onDrag(total.x)
                        change.consume()
                    } else {
                        break
                    }
                }
            } else {
                onDrag(delta.x)
                change.consume()
            }
            tracker.addPointerInputChange(change)
            if (!change.pressed) {
                if (state == 1) {
                    finished = true
                    onEnd(tracker.calculateVelocity().x)
                }
                break
            }
        }
        if (state == 1 && !finished) onCancel()
    }
}
