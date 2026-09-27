package com.artemiy.player.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * "Something's loading": five bars pulsing out from the middle like an equalizer — the
 * "bars-scale-middle" spinner from svg-spinners (MIT, Utkarsh Verma), redrawn here with the
 * original's timing since Android can't play SVG animations. Each bar grows from 12 to 22 (of
 * 24) and back in 0.6s; the middle one starts, its neighbours 0.2s later, the outer ones 0.4s
 * later, and the whole wave repeats every 0.9s.
 */
@Composable
fun EqualizerLoader(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val timeMs by transition.animateFloat(
        initialValue = 0f,
        targetValue = CYCLE_MS,
        animationSpec = infiniteRepeatable(tween(CYCLE_MS.toInt(), easing = LinearEasing)),
        label = "equalizerTime",
    )
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val left = (size.width - 24f * unit) / 2
        val top = (size.height - 24f * unit) / 2
        BAR_X.forEachIndexed { i, x ->
            val local = timeMs - BAR_DELAY_MS[i]
            val height = if (local in 0f..BAR_MS) barHeight(local / BAR_MS) else 12f
            drawRoundRect(
                color = color,
                topLeft = Offset(left + x * unit, top + (12f - height / 2) * unit),
                size = Size(2.8f * unit, height * unit),
                cornerRadius = CornerRadius(1.4f * unit),
            )
        }
    }
}

private const val CYCLE_MS = 900f
private const val BAR_MS = 600f
private val BAR_X = floatArrayOf(1f, 5.8f, 10.6f, 15.4f, 20.2f)
private val BAR_DELAY_MS = floatArrayOf(400f, 200f, 0f, 200f, 400f)
private val GROW = CubicBezierEasing(0.14f, 0.73f, 0.34f, 1f)
private val SHRINK = CubicBezierEasing(0.65f, 0.26f, 0.82f, 0.45f)

private fun barHeight(p: Float): Float =
    if (p < 0.5f) 12f + 10f * GROW.transform(p * 2f) else 22f - 10f * SHRINK.transform((p - 0.5f) * 2f)
