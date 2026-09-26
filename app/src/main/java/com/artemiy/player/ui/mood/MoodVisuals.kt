package com.artemiy.player.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.PathParser
import com.artemiy.player.data.Mood
import com.artemiy.player.ui.theme.PlayerColors
import kotlin.math.PI
import kotlin.math.sin

val MOOD_COLORS = mapOf(
    Mood.NORMAL to Color(0xFF8E8E93),
    Mood.HAPPY to Color(0xFFFFB020),
    Mood.LOUD to Color(0xFFFF4D6D),
    Mood.SAD to Color(0xFF4B6CB7),
    Mood.CRY to Color(0xFF5B3FAE),
)

// Display order matches the approved mockup, not the enum's declaration order.
val MOOD_DISPLAY_ORDER = listOf(Mood.HAPPY, Mood.LOUD, Mood.NORMAL, Mood.SAD, Mood.CRY)

/*
 * The moods as weather: happy = a burst of sun, normal = sun behind a cloud, sad = fog,
 * cry = heavy rain, loud = lightning. Framework7 Icons (MIT, Vladimir Kharlampidi) and Phosphor
 * Icons (MIT) for the lightning; the original paths are split into their pieces (cloud, rain
 * streaks, fog lines, rays) so each piece can move on its own.
 */

// Burst (Framework7) — one piece.
private val BURST = listOf(
    "M54.084 31.229 C55.376 30.605 56 29.714 56 28.823 S55.376 27.108 54.084 26.663 L43.548 23.03 L48.07 13.786 C48.315 13.318 48.382 12.828 48.382 12.405 C48.382 11.158 47.446 10.245 46.221 10.245 C45.798 10.245 45.308 10.311 44.841 10.556 L35.284 15.234 L30.294 4.564 C29.76 3.385 28.891 2.783 28 2.783 C27.131 2.783 26.262 3.385 25.728 4.565 L20.738 15.235 L11.182 10.557 C10.753 10.344 10.28 10.237 9.802 10.245 C8.576 10.245 7.618 11.159 7.618 12.406 C7.618 12.829 7.708 13.319 7.952 13.787 L12.474 23.031 L1.916 26.662 C0.668 27.085 0 27.954 0 28.845 C0 29.714 0.646 30.605 1.916 31.229 L12.029 36.062 L6.57 47.623 C6.326 48.075 6.196 48.579 6.192 49.093 C6.192 50.207 6.949 51.009 8.018 51.009 C8.464 51.009 8.954 50.853 9.466 50.541 L22.386 42.901 L25.927 53.861 C26.351 55.063 27.153 55.687 27.999 55.687 C28.89 55.687 29.669 55.063 30.071 53.86 L33.635 42.9 L46.555 50.541 C47.067 50.853 47.557 50.987 48.002 50.987 C49.027 50.987 49.829 50.229 49.829 49.116 C49.829 48.648 49.695 48.136 49.449 47.623 L43.97 36.063 Z",
)

// Cloud + fog (Framework7): [0] cloud, [1..2] fog lines.
private val FOG = listOf(
    "M12.262 36.719 L41.348 36.719 C48.778 36.719 54.52 31.094 54.52 23.899 C54.52 16.609 48.52 11.149 40.574 11.219 C37.574 5.172 32.067 1.679 25.574 1.679 C16.926 1.679 9.637 8.5 8.957 17.242 C4.41 18.484 1.48 22.305 1.48 26.922 C1.48 32.734 5.84 36.719 12.262 36.719",
    "M43.481 43.399 L12.988 43.399 C12.168 43.399 11.512 44.031 11.512 44.899 C11.512 45.742 12.168 46.375 12.988 46.375 L43.481 46.375 C44.301 46.375 44.957 45.742 44.957 44.898 C44.957 44.031 44.301 43.398 43.48 43.398",
    "M43.48 51.366 L12.988 51.366 C12.168 51.366 11.512 51.999 11.512 52.843 C11.512 53.686 12.168 54.319 12.988 54.319 L43.481 54.319 C44.301 54.319 44.957 53.686 44.957 52.843 C44.957 51.999 44.301 51.366 43.48 51.366",
)

// Cloud + heavy rain (Framework7): [0] cloud, [1..4] rain streaks.
private val RAIN = listOf(
    "M12.262 36.098 L41.348 36.098 C48.778 36.098 54.52 30.473 54.52 23.278 C54.52 15.988 48.52 10.528 40.574 10.598 C37.574 4.551 32.067 1.058 25.574 1.058 C16.926 1.058 9.637 7.878 8.957 16.621 C4.41 17.863 1.48 21.684 1.48 26.301 C1.48 32.113 5.84 36.098 12.262 36.098",
    "M8.066 54.028 L14.934 42.098 C15.379 41.348 15.144 40.551 14.442 40.128 C13.738 39.73 12.942 39.941 12.496 40.668 L5.606 52.645 C5.231 53.325 5.418 54.145 6.121 54.566 A1.44 1.44 0 0 0 8.066 54.027",
    "M17.395 54.027 L24.285 42.097 C24.707 41.347 24.473 40.55 23.793 40.127 C23.067 39.729 22.27 39.94 21.848 40.667 L14.934 52.644 C14.559 53.324 14.746 54.144 15.473 54.565 C16.176 54.94 17.02 54.706 17.395 54.026",
    "M26.723 54.026 L33.613 42.096 C34.035 41.346 33.801 40.549 33.121 40.126 C32.395 39.728 31.598 39.939 31.176 40.666 L24.262 52.643 C23.887 53.323 24.074 54.143 24.801 54.564 C25.481 54.939 26.348 54.705 26.723 54.025",
    "M36.004 54.048 L42.918 42.048 C43.34 41.345 43.129 40.501 42.426 40.126 C41.746 39.774 40.926 39.938 40.504 40.642 L33.637 52.595 C33.215 53.321 33.402 54.142 34.105 54.563 A1.41 1.41 0 0 0 36.004 54.048",
)

// Sun behind a cloud (Framework7): [3] sun, [4] cloud, the rest rays.
private val CLOUD_SUN = listOf(
    "M35.986 17.817 C36.876 17.817 37.591 17.082 37.591 16.212 L37.591 12.035 C37.591 11.145 36.876 10.43 35.986 10.43 S34.381 11.145 34.381 12.035 L34.381 16.212 C34.381 17.082 35.097 17.817 35.986 17.817",
    "M24.771 21.549 C25.409 22.187 26.453 22.167 27.053 21.549 C27.652 20.929 27.671 19.924 27.053 19.305 L24.075 16.328 A1.593 1.593 0 0 0 21.812 16.328 C21.193 16.927 21.193 17.952 21.812 18.59 Z",
    "M44.921 21.549 C45.52 22.167 46.564 22.187 47.201 21.549 L50.161 18.59 C50.779 17.952 50.76 16.927 50.161 16.328 A1.593 1.593 0 0 0 47.898 16.328 L44.92 19.305 C44.301 19.925 44.32 20.93 44.92 21.549",
    "M40.955 38.759 C43.797 36.844 45.557 33.886 45.557 30.482 A9.55 9.55 0 0 0 35.985 20.892 C32.485 20.892 29.449 22.767 27.845 25.571 C28.657 26.402 29.353 27.311 29.933 28.433 C36.313 28.955 40.433 32.919 40.955 38.759",
    "M8.025 49.413 L28.948 49.413 C34.42 49.413 38.674 45.275 38.674 39.919 C38.674 34.582 34.343 30.559 28.561 30.521 C26.298 26.209 22.276 23.695 17.597 23.695 C11.312 23.695 5.975 28.587 5.395 34.93 C2.108 35.916 0 38.7 0 42.104 C0 46.435 3.268 49.414 8.025 49.414",
    "M50.219 32.088 L54.395 32.088 C55.265 32.088 55.981 31.373 56 30.483 C56 29.593 55.265 28.878 54.395 28.878 L50.218 28.878 C49.348 28.878 48.613 29.594 48.613 30.483 C48.613 31.373 49.348 32.088 50.218 32.088",
    "M47.898 44.677 A1.61 1.61 0 0 0 50.159 44.657 C50.759 44.039 50.779 43.014 50.159 42.414 L47.162 39.436 C46.544 38.837 45.538 38.818 44.919 39.436 A1.634 1.634 0 0 0 44.919 41.718 Z",
)

// Lightning (Phosphor, 256 grid) — one piece.
private val BOLT = listOf(
    "M213.85 125.46 L101.85 245.46 A8 8 0 0 1 88.16 238.46 L102.82 165.13 L45.19 143.49 A8 8 0 0 1 42.19 130.49 L154.19 10.49 A8 8 0 0 1 167.88 17.49 L153.18 90.9 L210.81 112.51 A8 8 0 0 1 213.81 125.46 Z",
)

private fun partsFor(mood: Mood): List<String> = when (mood) {
    Mood.HAPPY -> BURST
    Mood.LOUD -> BOLT
    Mood.NORMAL -> CLOUD_SUN
    Mood.SAD -> FOG
    Mood.CRY -> RAIN
}

/**
 * The mood's glyph. [motion] (0→1, read while drawing) plays its little animation — the burst
 * turns, the lightning flickers, the rays of the sun behind the cloud turn and pulse, the fog
 * lines drift apart, the rain falls. At 0 it's the plain icon.
 */
@Composable
fun MoodIcon(
    mood: Mood,
    modifier: Modifier = Modifier,
    glyph: Color = PlayerColors.TextPrimary,
    motion: () -> Float = { 0f },
) {
    val parts = remember(mood) { partsFor(mood).map { PathParser().parsePathString(it).toPath() } }
    val grid = if (mood == Mood.LOUD) 256f else 56f
    Canvas(modifier = modifier) {
        val k = size.minDimension / grid
        translate((size.width - grid * k) / 2, (size.height - grid * k) / 2) {
            scale(k, k, pivot = Offset.Zero) {
                drawMoodParts(mood, parts, motion().coerceIn(0f, 1f), glyph)
            }
        }
    }
}

private val BURST_NODES by lazy { PathParser().parsePathString(BURST[0]).toNodes() }

/**
 * The burst with every point pushed away from / pulled toward [center] by an amount that depends
 * on its angle and the time — so the rays grow and shrink unevenly, like a living spark. The
 * middle barely moves; the tips move the most.
 */
private fun warpedBurst(center: Offset, t: Float, amount: Float): Path {
    val phase = 2f * PI.toFloat() * t
    fun warp(x: Float, y: Float): Offset {
        val dx = x - center.x
        val dy = y - center.y
        val r = kotlin.math.sqrt(dx * dx + dy * dy)
        if (r < 0.001f) return Offset(x, y)
        val angle = kotlin.math.atan2(dy, dx)
        val reach = ((r - 11f) / 16f).coerceIn(0f, 1f)
        val wobble = 0.6f * sin(3f * angle + 3f * phase) + 0.4f * sin(5f * angle - 2f * phase)
        val k = 1f + amount * wobble * reach
        return Offset(center.x + dx * k, center.y + dy * k)
    }
    val path = Path()
    var current = Offset.Zero
    var lastControl: Offset? = null
    for (node in BURST_NODES) {
        when (node) {
            is PathNode.MoveTo -> { current = warp(node.x, node.y); path.moveTo(current.x, current.y); lastControl = null }
            is PathNode.LineTo -> { current = warp(node.x, node.y); path.lineTo(current.x, current.y); lastControl = null }
            is PathNode.CurveTo -> {
                val c1 = warp(node.x1, node.y1)
                val c2 = warp(node.x2, node.y2)
                current = warp(node.x3, node.y3)
                path.cubicTo(c1.x, c1.y, c2.x, c2.y, current.x, current.y)
                lastControl = c2
            }
            is PathNode.ReflectiveCurveTo -> {
                val c1 = lastControl?.let { current * 2f - it } ?: current
                val c2 = warp(node.x1, node.y1)
                current = warp(node.x2, node.y2)
                path.cubicTo(c1.x, c1.y, c2.x, c2.y, current.x, current.y)
                lastControl = c2
            }
            is PathNode.Close -> path.close()
            else -> Unit
        }
    }
    return path
}

private fun easeOut(t: Float) = 1f - (1f - t) * (1f - t) * (1f - t)

/** All in the icon's own grid units (56, or 256 for the lightning). */
private fun DrawScope.drawMoodParts(mood: Mood, parts: List<Path>, t: Float, color: Color) {
    val wave = sin(2f * PI.toFloat() * t)
    when (mood) {
        Mood.HAPPY -> {
            val pivot = Offset(28f, 28.8f)
            // The rays play: each one stretches and pulls back on its own beat (two waves
            // running round the burst at different speeds), easing in and out with the glyph.
            val amount = 0.16f * sin(PI.toFloat() * t)
            val burst = if (amount == 0f) parts[0] else warpedBurst(pivot, t, amount)
            rotate(40f * easeOut(t), pivot) { drawPath(burst, color) }
        }
        Mood.LOUD -> {
            // Two quick flashes and a shiver that dies down.
            val flash = when (t) {
                in 0.14f..0.19f -> 0.25f
                in 0.27f..0.31f -> 0.55f
                else -> 1f
            }
            val shake = 7f * sin(t * 90f) * (1f - t)
            translate(shake, 0f) { drawPath(parts[0], color.copy(alpha = color.alpha * flash)) }
        }
        Mood.NORMAL -> {
            val sun = Offset(36f, 30.5f)
            val rays = listOf(0, 1, 2, 5, 6)
            rotate(22f * easeOut(t), sun) {
                scale(1f + 0.1f * sin(PI.toFloat() * t), sun) { rays.forEach { drawPath(parts[it], color) } }
            }
            scale(1f + 0.04f * sin(PI.toFloat() * t), sun) { drawPath(parts[3], color) }
            translate(-2.5f * easeOut(t), 0f) { drawPath(parts[4], color) }
        }
        Mood.SAD -> {
            translate(0f, 1.2f * wave) { drawPath(parts[0], color) }
            translate(3.5f * wave, 0f) { drawPath(parts[1], color) }
            translate(-3.5f * wave, 0f) { drawPath(parts[2], color) }
        }
        Mood.CRY -> {
            drawPath(parts[0], color)
            // Streaks slide down along their own slant and fade in and out, one after another.
            for (i in 1..4) {
                if (t == 0f) {
                    drawPath(parts[i], color)
                    continue
                }
                val phase = (t * 1.6f + i * 0.23f) % 1f
                val along = phase * 7f - 2f
                translate(-0.5f * along, 0.866f * along) {
                    drawPath(parts[i], color.copy(alpha = color.alpha * sin(PI.toFloat() * phase)))
                }
            }
        }
    }
}

/** The happy burst as a loading mark: spinning slowly with its rays playing, round and round. */
@Composable
fun LoadingBurst(color: Color, modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "loadingBurst")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "burstPlay",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(9000, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "burstSpin",
    )
    Canvas(modifier = modifier) {
        val k = size.minDimension / 56f
        val pivot = Offset(28f, 28.8f)
        translate((size.width - 56f * k) / 2, (size.height - 56f * k) / 2) {
            scale(k, k, pivot = Offset.Zero) {
                rotate(spin, pivot) { drawPath(warpedBurst(pivot, t, 0.14f), color) }
            }
        }
    }
}
