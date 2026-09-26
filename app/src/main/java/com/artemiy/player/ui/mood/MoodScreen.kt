package com.artemiy.player.ui.mood

import com.artemiy.player.ui.icons.AppIcons
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.Mood
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The "Настроение" tab: one mood per page, swiped sideways. Behind the page sits a slowly
 * breathing, wavy glowing ring in the mood's color; on every switch the mood's own glyph shows
 * up big for a moment first, then dissolves into the ring.
 */
@Composable
fun MoodScreen(onPlayMood: (Mood) -> Unit, genresFor: (Mood) -> List<String>) {
    val pager = rememberPagerState(pageCount = { MOOD_DISPLAY_ORDER.size })

    // Ring color follows the swipe continuously instead of jumping when a page settles.
    val position = pager.currentPage + pager.currentPageOffsetFraction
    val from = MOOD_DISPLAY_ORDER[position.toInt().coerceIn(0, MOOD_DISPLAY_ORDER.lastIndex)]
    val to = MOOD_DISPLAY_ORDER[(position.toInt() + 1).coerceIn(0, MOOD_DISPLAY_ORDER.lastIndex)]
    val blend = position - position.toInt()
    val ringColor = lerp(MOOD_COLORS.getValue(from), MOOD_COLORS.getValue(to), blend)

    // Glyph-first intro, replayed every time a new mood settles: the ring (and the page text)
    // step back, the glyph pops in, holds, then fades as the ring comes back.
    val ringAlpha = remember { Animatable(0f) }
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.8f) }
    LaunchedEffect(pager.settledPage, pager.isScrollInProgress) {
        if (pager.isScrollInProgress) {
            // A swipe interrupts the intro at once: glyph out, ring and text back. Taking over the
            // same animations also cancels an intro that was still running.
            coroutineScope {
                listOf(
                    async { iconAlpha.animateTo(0f, tween(150)) },
                    async { ringAlpha.animateTo(1f, tween(200)) },
                ).awaitAll()
            }
            return@LaunchedEffect
        }
        // Only once the page has come fully to rest — a quick series of flings doesn't replay it.
        delay(120)
        coroutineScope {
            listOf(
                async { ringAlpha.animateTo(0f, tween(200)) },
                async { iconScale.snapTo(0.8f); iconScale.animateTo(1f, tween(450)) },
                async { iconAlpha.animateTo(1f, tween(300)) },
            ).awaitAll()
        }
        delay(550)
        coroutineScope {
            listOf(
                async { iconAlpha.animateTo(0f, tween(450)) },
                async { ringAlpha.animateTo(1f, tween(650)) },
            ).awaitAll()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background)
            .statusBarsPadding(),
    ) {
        Text(
            text = "Настроение",
            color = PlayerColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 0.dp),
        )

        GlowingRing(
            color = ringColor,
            from = RING_SHAPES.getValue(from),
            to = RING_SHAPES.getValue(to),
            blend = blend,
            modifier = Modifier
                .fillMaxSize()
                // ModulateAlpha: fading without an offscreen buffer, which would cut the blurred
                // glow off at the edges.
                .graphicsLayer {
                    alpha = ringAlpha.value
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
        )

        MoodIcon(
            mood = MOOD_DISPLAY_ORDER[pager.settledPage],
            glyph = ringColor,
            modifier = Modifier
                .align(Alignment.Center)
                .size(120.dp)
                // graphicsLayer rather than alpha()/scale(): alpha() also clips to the bounds, and
                // the glyphs (the sun's rays especially) reach past them while scaling up.
                .graphicsLayer {
                    alpha = iconAlpha.value
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
        )

        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val mood = MOOD_DISPLAY_ORDER[page]
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer { alpha = 1f - iconAlpha.value }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPlayMood(mood) }
                        .padding(24.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(PlayerColors.TextPrimary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = AppIcons.Play,
                                contentDescription = null,
                                tint = PlayerColors.TextPrimary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Text(
                            text = mood.label,
                            color = PlayerColors.TextPrimary,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Text(
                        text = mood.subtitle,
                        color = PlayerColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The library's own most common genres in this mood's mix — what's actually going to play.
            Crossfade(targetState = MOOD_DISPLAY_ORDER[pager.settledPage], label = "moodGenres") { mood ->
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    genresFor(mood).forEach { genre ->
                        Text(
                            text = genre,
                            color = PlayerColors.TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PlayerColors.Surface)
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MOOD_DISPLAY_ORDER.forEachIndexed { index, mood ->
                    // How "current" this page is right now (1 = fully, 0 = not at all), following the
                    // swipe itself so the pill stretches and shrinks with the finger, not after it.
                    val closeness = (1f - abs(position - index)).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .height(7.dp)
                            .width(7.dp + 17.dp * closeness)
                            .clip(CircleShape)
                            .background(lerp(PlayerColors.TextTertiary, MOOD_COLORS.getValue(mood), closeness)),
                    )
                }
            }
        }
    }
}

/**
 * Each mood's own ring shape: stretch of the oval, a few waves around it (amplitude, how many
 * bumps, which clock drives it) and an optional sag at the bottom.
 */
private class RingShape(
    val stretchX: Float,
    val stretchY: Float,
    val waves: List<Triple<Float, Int, Int>>,
    val sag: Float = 0f,
) {
    val maxWobble: Float get() = waves.sumOf { it.first.toDouble() }.toFloat() + sag
}

private val RING_SHAPES = mapOf(
    // Bubbly: lots of small round bumps.
    Mood.HAPPY to RingShape(1.06f, 0.92f, listOf(Triple(0.05f, 5, 0), Triple(0.035f, 3, 1), Triple(0.02f, 7, 2))),
    // Energetic: more, sharper bumps, bigger swing.
    Mood.LOUD to RingShape(1.04f, 0.94f, listOf(Triple(0.065f, 4, 0), Triple(0.04f, 7, 1), Triple(0.03f, 9, 2))),
    // Calm wide oval.
    Mood.NORMAL to RingShape(1.1f, 0.86f, listOf(Triple(0.05f, 2, 0), Triple(0.03f, 3, 1), Triple(0.015f, 5, 2))),
    // Soft and slightly drooping at the bottom.
    Mood.SAD to RingShape(1.0f, 0.95f, listOf(Triple(0.035f, 2, 0), Triple(0.02f, 3, 1), Triple(0.01f, 4, 2)), sag = 0.07f),
    // A tall teardrop: a single-bump wave makes it lopsided.
    Mood.CRY to RingShape(0.9f, 1.06f, listOf(Triple(0.08f, 1, 0), Triple(0.025f, 3, 1), Triple(0.012f, 5, 2)), sag = 0.04f),
)

/**
 * The mood's glowing loop. Two layers: a wide, faint glow blurred into a cloud that spreads well
 * past the loop, and the crisp line with a little glow of its own. Swiping between moods morphs
 * one mood's shape into the next (point by point, [blend] of the way).
 */
@Composable
private fun GlowingRing(color: Color, from: RingShape, to: RingShape, blend: Float, modifier: Modifier = Modifier) {
    // Three waves on independent clocks (each read at a 1x multiplier, so every loop wraps smoothly)
    // plus a slower breath — the shape never visibly repeats.
    val waveA by rememberInfiniteTransition(label = "waveA").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Restart), label = "a",
    )
    val waveB by rememberInfiniteTransition(label = "waveB").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(13_000, easing = LinearEasing), RepeatMode.Restart), label = "b",
    )
    val waveC by rememberInfiniteTransition(label = "waveC").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(17_000, easing = LinearEasing), RepeatMode.Restart), label = "c",
    )
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Restart), label = "d",
    )
    val clocks = floatArrayOf(waveA, waveB, waveC)
    Box(modifier = modifier) {
        // Wide cloud: spread across the screen, kept faint.
        Canvas(modifier = Modifier.fillMaxSize().blur(80.dp, BlurredEdgeTreatment.Unbounded)) {
            val path = morphedLoop(from, to, blend, clocks, breath, grow = 1.1f)
            drawPath(path = path, color = color.copy(alpha = 0.12f))
            drawPath(path = path, color = color.copy(alpha = 0.22f), style = Stroke(width = 90.dp.toPx(), join = StrokeJoin.Round))
        }
        // Close glow hugging the line.
        Canvas(modifier = Modifier.fillMaxSize().blur(14.dp, BlurredEdgeTreatment.Unbounded)) {
            drawPath(
                path = morphedLoop(from, to, blend, clocks, breath),
                color = color.copy(alpha = 0.45f),
                style = Stroke(width = 12.dp.toPx(), join = StrokeJoin.Round),
            )
        }
        // The crisp line on top.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPath(
                path = morphedLoop(from, to, blend, clocks, breath),
                color = color.copy(alpha = 0.95f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** Keeps the loop (at its widest wobble) this far from the screen's side edges. */
private val RING_SIDE_MARGIN = 26.dp

private fun DrawScope.loopPoint(shape: RingShape, t: Float, clocks: FloatArray, breath: Float, grow: Float): Offset {
    val maxHalfWidth = size.width / 2 - RING_SIDE_MARGIN.toPx()
    val base = minOf(
        maxHalfWidth / (shape.stretchX * (1f + shape.maxWobble) * 1.025f),
        size.height * 0.3f / (shape.stretchY * (1f + shape.maxWobble)),
    ) * (1f + 0.025f * sin(breath)) * grow
    var wobble = 0f
    shape.waves.forEach { (amplitude, bumps, clock) -> wobble += amplitude * sin(bumps * t + clocks[clock]) }
    // Sag only pulls the lower half (sin t > 0 is downward on screen) further down.
    val sag = shape.sag * maxOf(0f, sin(t))
    val r = base * (1f + wobble)
    return Offset(
        size.width / 2 + r * shape.stretchX * cos(t),
        size.height / 2 + r * shape.stretchY * sin(t) + base * sag,
    )
}

private fun DrawScope.morphedLoop(
    from: RingShape,
    to: RingShape,
    blend: Float,
    clocks: FloatArray,
    breath: Float,
    grow: Float = 1f,
): Path {
    val path = Path()
    val steps = 200
    for (i in 0..steps) {
        val t = (i.toFloat() / steps) * 2f * PI.toFloat()
        val a = loopPoint(from, t, clocks, breath, grow)
        val b = loopPoint(to, t, clocks, breath, grow)
        val x = a.x + (b.x - a.x) * blend
        val y = a.y + (b.y - a.y) * blend
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
