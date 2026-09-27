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
import com.artemiy.player.ui.components.pressScale
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
import kotlinx.coroutines.launch
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
    val screenWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

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
    // The glyph sinks into the background (shrinking, blurring away) and the ring grows back out
    // of the spot where it sank; when a new mood comes, the ring draws back into the middle.
    val iconSink = remember { Animatable(0f) }
    val ringScale = remember { Animatable(1f) }
    // The settled mood's title making its entrance (0 → 1) as the glyph sinks away.
    val titleIntro = remember { Animatable(1f) }
    PreloadMoodFonts()
    // The glyph's own little animation, running for as long as it's on screen.
    val iconMotion = remember { Animatable(0f) }
    LaunchedEffect(pager.settledPage, pager.isScrollInProgress) {
        if (pager.isScrollInProgress) {
            // A swipe interrupts the intro at once: glyph out, ring and text back. Taking over the
            // same animations also cancels an intro that was still running.
            coroutineScope {
                listOf(
                    async { iconAlpha.animateTo(0f, tween(150)) },
                    async { ringAlpha.animateTo(1f, tween(200)) },
                    async { ringScale.animateTo(1f, tween(200)) },
                    async { titleIntro.snapTo(1f) },
                ).awaitAll()
            }
            return@LaunchedEffect
        }
        // Only once the page has come fully to rest — a quick series of flings doesn't replay it.
        delay(120)
        launch { iconMotion.snapTo(0f); iconMotion.animateTo(1f, tween(1450, easing = LinearEasing)) }
        coroutineScope {
            listOf(
                async { ringAlpha.animateTo(0f, tween(220)) },
                async { ringScale.animateTo(RING_SUNK_SCALE, tween(260)) },
                async { iconSink.snapTo(0f); iconScale.snapTo(0.8f); iconScale.animateTo(1f, tween(450)) },
                async { titleIntro.snapTo(0f) },
                async { iconAlpha.animateTo(1f, tween(300)) },
            ).awaitAll()
        }
        delay(550)
        coroutineScope {
            listOf(
                // Sinking: slow at first, then gone — and the ring rises out of it a moment later.
                async { iconSink.animateTo(1f, tween(700, easing = androidx.compose.animation.core.FastOutLinearInEasing)) },
                async { iconAlpha.animateTo(0f, tween(650, delayMillis = 50)) },
                async { delay(180); ringScale.animateTo(1f, tween(900, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) },
                async { delay(180); ringAlpha.animateTo(1f, tween(700)) },
                async {
                    delay(250)
                    val mood = MOOD_DISPLAY_ORDER[pager.settledPage]
                    titleIntro.animateTo(1f, tween(moodTitleIntroMs(mood), easing = LinearEasing))
                },
            ).awaitAll()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background)
            .statusBarsPadding(),
    ) {
        MoodParticles(
            from = from,
            to = to,
            blend = blend,
            color = ringColor,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = ringAlpha.value },
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
                    scaleX = ringScale.value
                    scaleY = ringScale.value
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
        )

        MoodIcon(
            mood = MOOD_DISPLAY_ORDER[pager.settledPage],
            glyph = ringColor,
            motion = { iconMotion.value },
            modifier = Modifier
                .align(Alignment.Center)
                // Room around the 120dp glyph, so the layer below (blurred while sinking) doesn't
                // cut off the rays or the blur at its edges.
                .size(120.dp + GLYPH_BLEED * 2)
                // graphicsLayer rather than alpha()/scale(): alpha() also clips to the bounds, and
                // the glyphs (the sun's rays especially) reach past them while scaling up.
                .graphicsLayer {
                    val sink = iconSink.value
                    alpha = iconAlpha.value
                    scaleX = iconScale.value * (1f - 0.45f * sink)
                    scaleY = iconScale.value * (1f - 0.45f * sink)
                    // Sinking also goes soft and out of focus (blur needs Android 12+; older ones
                    // just shrink and fade).
                    if (sink > 0.01f && android.os.Build.VERSION.SDK_INT >= 31) {
                        val radius = 22.dp.toPx() * sink
                        renderEffect = androidx.compose.ui.graphics.BlurEffect(radius, radius, androidx.compose.ui.graphics.TileMode.Decal)
                    } else {
                        renderEffect = null
                    }
                    compositingStrategy = if (sink > 0.01f) CompositingStrategy.Offscreen else CompositingStrategy.ModulateAlpha
                }
                .padding(GLYPH_BLEED),
        )

        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val mood = MOOD_DISPLAY_ORDER[page]
            // How far this page is from the middle of the screen, in pages (+ = off to the left).
            val pageOffset = { (pager.currentPage - page) + pager.currentPageOffsetFraction }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer { alpha = 1f - iconAlpha.value }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPlayMood(mood) }
                        .padding(24.dp),
                ) {
                    // Parallax: the title trails the swipe a little and the line under it runs a
                    // little ahead, so the page has some depth as it slides.
                    MoodTitle(
                        mood = mood,
                        progress = { if (page == pager.settledPage) titleIntro.value else 1f },
                        // Fades out on its way, so a title trailing behind its page is gone before
                        // the page's edge could cut it off.
                        modifier = Modifier.graphicsLayer {
                            val offset = pageOffset()
                            translationX = offset * screenWidthPx * 0.3f
                            alpha = (1f - kotlin.math.abs(offset) * 1.8f).coerceIn(0f, 1f)
                        },
                    )
                    Text(
                        text = mood.subtitle,
                        color = PlayerColors.TextSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .graphicsLayer {
                                val offset = pageOffset()
                                translationX = -offset * screenWidthPx * 0.15f
                                alpha = (1f - kotlin.math.abs(offset) * 1.8f).coerceIn(0f, 1f)
                            },
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
            MoodPlayButton(
                onClick = { onPlayMood(MOOD_DISPLAY_ORDER[pager.settledPage]) },
                modifier = Modifier.padding(bottom = 22.dp),
            )
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
            val path = morphedLoop(from, to, blend, clocks, breath, grow = 1.15f)
            drawPath(path = path, color = color.copy(alpha = 0.12f))
            drawPath(path = path, color = color.copy(alpha = 0.22f), style = Stroke(width = 160.dp.toPx(), join = StrokeJoin.Round))
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

/**
 * "Слушать" for the mood on screen: the album/artist pages' button (same size, same little
 * shrink when pressed), but in the theme's own text/background colors rather than the accent.
 */
@Composable
private fun MoodPlayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.93f)
            .clip(CircleShape)
            .background(PlayerColors.TextPrimary)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = AppIcons.Play, contentDescription = null, tint = PlayerColors.Background, modifier = Modifier.size(20.dp))
        Text(text = "Слушать", color = PlayerColors.Background, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

/** How small the ring draws back to while a mood's glyph is on screen. */
private const val RING_SUNK_SCALE = 0.55f

private val GLYPH_BLEED = 50.dp

/**
 * A few faint particles drifting behind the ring, in the mood's color: rain for "Поплакать",
 * sparks flying up for "Громкое", bubbles rising for "Весёлое"; the other moods stay clear.
 * Crossfades between two moods' kinds while swiping. Only a dozen shapes, drawn on the frame the
 * ring is redrawn anyway.
 */
@Composable
private fun MoodParticles(from: Mood, to: Mood, blend: Float, color: Color, modifier: Modifier = Modifier) {
    val time by rememberInfiniteTransition(label = "particles").animateFloat(
        0f, 1f, infiniteRepeatable(tween(PARTICLE_LOOP_MS, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(modifier = modifier) {
        drawParticles(from, 1f - blend, time, color)
        if (to != from) drawParticles(to, blend, time, color)
    }
}

private fun DrawScope.drawParticles(mood: Mood, weight: Float, time: Float, color: Color) {
    if (weight <= 0.01f) return
    val tint = lerp(color, Color.White, 0.25f)
    repeat(PARTICLE_COUNT) { i ->
        // Fixed per particle: where across, how many trips per loop (whole numbers, so the loop
        // wraps seamlessly), and where in its trip it starts.
        val x0 = ((i * 0.618034f + 0.13f) % 1f) * size.width
        val phase = (i * 0.381966f + 0.37f) % 1f
        when (mood) {
            Mood.CRY -> {
                val trips = 5 + i % 4
                val f = (time * trips + phase) % 1f
                val y = -30.dp.toPx() + f * (size.height + 60.dp.toPx())
                drawLine(
                    color = tint.copy(alpha = 0.22f * weight),
                    start = Offset(x0, y),
                    end = Offset(x0 - 2.dp.toPx(), y + 16.dp.toPx()),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            Mood.LOUD -> {
                val trips = 6 + i % 5
                val f = (time * trips + phase) % 1f
                val y = size.height * (0.95f - 0.8f * f)
                val x = x0 + sin(f * 9f + i) * 14.dp.toPx()
                drawCircle(
                    color = tint.copy(alpha = 0.55f * sin(f * PI.toFloat()) * weight),
                    radius = (1.2f + (i % 3) * 0.5f).dp.toPx(),
                    center = Offset(x, y),
                )
            }
            Mood.HAPPY -> {
                val trips = 2 + i % 3
                val f = (time * trips + phase) % 1f
                val y = size.height * (1.05f - 1.1f * f)
                val x = x0 + sin(f * 7f + i) * 18.dp.toPx()
                drawCircle(
                    color = tint.copy(alpha = 0.3f * sin(f * PI.toFloat()) * weight),
                    radius = (4 + i % 5).dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 1.4.dp.toPx()),
                )
            }
            Mood.NORMAL, Mood.SAD -> return
        }
    }
}

private const val PARTICLE_COUNT = 14
private const val PARTICLE_LOOP_MS = 40_000
