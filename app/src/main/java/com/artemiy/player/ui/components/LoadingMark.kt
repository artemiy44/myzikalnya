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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * "Something's loading" — one of three quiet marks, picked anew each time the app starts and
 * then kept for the whole session (so it never changes from one screen to the next): the app's
 * own burst, three bouncing dots, or rings spreading out like sound. The dots and rings are
 * svg-spinners' "3-dots-bounce" and "pulse-rings-2" (MIT, Utkarsh Verma), redrawn with the
 * originals' timing since Android can't play SVG animations.
 */
@Composable
fun LoadingMark(color: Color, modifier: Modifier = Modifier) {
    when (SESSION_MARK) {
        0 -> com.artemiy.player.ui.mood.LoadingBurst(color = color, modifier = modifier)
        1 -> BouncingDots(color, modifier)
        else -> PulseRings(color, modifier)
    }
}

private val SESSION_MARK = kotlin.random.Random.nextInt(3)

/** Three dots hopping up one after another, 0.1 s apart, each hop 0.6 s; a short rest, again. */
@Composable
private fun BouncingDots(color: Color, modifier: Modifier) {
    val time by rememberInfiniteTransition(label = "dots").animateFloat(
        0f, DOTS_CYCLE_MS, infiniteRepeatable(tween(DOTS_CYCLE_MS.toInt(), easing = LinearEasing)), label = "dotsTime",
    )
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val left = (size.width - 24f * unit) / 2
        val top = (size.height - 24f * unit) / 2
        for (i in 0..2) {
            val local = (time - i * 100f) / HOP_MS
            val lift = when {
                local !in 0f..1f -> 0f
                local < 0.5f -> HOP_UP.transform(local * 2f)
                else -> 1f - HOP_DOWN.transform((local - 0.5f) * 2f)
            }
            drawCircle(color, radius = 3f * unit, center = Offset(left + (4f + i * 8f) * unit, top + (12f - 6f * lift) * unit))
        }
    }
}

private const val HOP_MS = 600f
private const val DOTS_CYCLE_MS = 1050f
private val HOP_UP = SafeCubicBezier(0.33f, 0.66f, 0.66f, 1f)
private val HOP_DOWN = SafeCubicBezier(0.33f, 0f, 0.66f, 0.33f)

/** Two rings growing out of the middle and fading as they go, half a beat apart. */
@Composable
private fun PulseRings(color: Color, modifier: Modifier) {
    val time by rememberInfiniteTransition(label = "rings").animateFloat(
        0f, RING_MS, infiniteRepeatable(tween(RING_MS.toInt(), easing = LinearEasing)), label = "ringsTime",
    )
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        for (k in 0..1) {
            val p = RING_EASE.transform(((time + k * RING_MS / 2) % RING_MS) / RING_MS)
            drawCircle(
                color = color.copy(alpha = color.alpha * (1f - p)),
                radius = 10f * unit * p,
                center = center,
                style = Stroke(width = 2f * unit * p.coerceAtLeast(0.2f)),
            )
        }
    }
}

private const val RING_MS = 1200f
private val RING_EASE = SafeCubicBezier(0.52f, 0.6f, 0.25f, 0.99f)
