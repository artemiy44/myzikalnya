package com.artemiy.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.artemiy.player.ui.theme.PlayerColors
import kotlin.math.roundToInt

/**
 * Thin flat seek/volume bar — matches the app's design, not Material's default Slider look.
 *
 * The whole bar takes touches, not just the dot: a tap jumps there, and a drag started anywhere
 * on it follows the finger. (Android also stretches this thin bar's touch area to a finger-sized
 * height on its own.) Positions come straight from where the finger is, never from the value as
 * it was when the gesture began.
 */
@Composable
fun MinimalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 4.dp,
    thumbSize: Dp = 12.dp,
    activeColor: Color = PlayerColors.Accent,
    inactiveColor: Color = PlayerColors.TextSecondary.copy(alpha = 0.3f),
) {
    val density = LocalDensity.current
    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)
    val onChange by rememberUpdatedState(onValueChange)
    val onFinished by rememberUpdatedState(onValueChangeFinished)
    val start by rememberUpdatedState(valueRange.start)
    val span by rememberUpdatedState(range)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbSize),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val thumbPx = with(density) { thumbSize.toPx() }
        val usableWidth = (widthPx - thumbPx).coerceAtLeast(1f)

        fun valueAt(x: Float): Float {
            val newFraction = ((x - thumbPx / 2) / usableWidth).coerceIn(0f, 1f)
            return start + newFraction * span
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbSize)
                .pointerInput(usableWidth, thumbPx) {
                    detectTapGestures { offset ->
                        onChange(valueAt(offset.x))
                        onFinished?.invoke()
                    }
                }
                .pointerInput(usableWidth, thumbPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> onChange(valueAt(offset.x)) },
                        onDragEnd = { onFinished?.invoke() },
                        onDragCancel = { onFinished?.invoke() },
                    ) { change, _ ->
                        change.consume()
                        onChange(valueAt(change.position.x))
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(inactiveColor),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(trackHeight)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(activeColor),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((fraction * usableWidth).roundToInt(), 0) }
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(activeColor),
            )
        }
    }
}
