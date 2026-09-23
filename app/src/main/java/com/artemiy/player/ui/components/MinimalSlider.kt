package com.artemiy.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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

/** Thin flat seek/volume bar — matches the app's design, not Material's default Slider look. */
@Composable
fun MinimalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 4.dp,
    thumbSize: Dp = 12.dp,
    activeColor: Color = PlayerColors.TextPrimary,
    inactiveColor: Color = PlayerColors.TextSecondary.copy(alpha = 0.3f),
) {
    val density = LocalDensity.current
    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbSize),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val thumbPx = with(density) { thumbSize.toPx() }
        val usableWidth = (widthPx - thumbPx).coerceAtLeast(0f)

        fun updateFromX(x: Float) {
            val newFraction = (x / usableWidth).coerceIn(0f, 1f)
            onValueChange(valueRange.start + newFraction * range)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(inactiveColor)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        updateFromX(offset.x - thumbPx / 2)
                        onValueChangeFinished?.invoke()
                    }
                },
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
                .background(activeColor)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { onValueChangeFinished?.invoke() },
                    ) { change, dragAmount ->
                        change.consume()
                        val currentX = fraction * usableWidth
                        updateFromX(currentX + dragAmount.x)
                    }
                },
        )
    }
}
