@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.artemiy.player.ui.mood

import com.artemiy.player.data.label
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.artemiy.player.R
import com.artemiy.player.data.Mood
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.inAppFont
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// Each mood's title has a typeface of its own (all SIL Open Font License, credited in "О
// приложении"); "Обычное" keeps the app's font. Sizes differ since the faces differ a lot in
// width: Unbounded and Climate Crisis are very wide, Caveat small and narrow.

private val Unbounded = FontFamily(
    Font(R.font.mood_unbounded, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)
private val PlaypenSans = FontFamily(Font(R.font.mood_playpen_sans, FontWeight.Light))

/**
 * Climate Crisis at a few points of its YEAR axis: 1979 is the letters whole, later years melt
 * them into puddles. A handful of fixed steps (each loaded once) rather than a new one every
 * frame — loading the font is far too slow for that.
 */
private val ClimateCrisisYears = listOf(2050f, 2036f, 2022f, 2008f, 1994f, 1979f).map { year ->
    FontFamily(
        Font(R.font.mood_climate_crisis, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.Setting("YEAR", year))),
    )
}
private val Caveat = FontFamily(Font(R.font.mood_caveat, FontWeight.Medium))

/** Loads every title font ahead of time, so the first entrance doesn't stall on it. */
@Composable
internal fun PreloadMoodFonts() {
    val resolver = LocalFontFamilyResolver.current
    LaunchedEffect(resolver) {
        (listOf(Unbounded, PlaypenSans, Caveat) + ClimateCrisisYears).forEach { runCatching { resolver.preload(it) } }
    }
}

/** How long each title's entrance takes. */
internal fun moodTitleIntroMs(mood: Mood): Int = when (mood) {
    Mood.NORMAL -> 600
    Mood.HAPPY -> 950
    Mood.LOUD -> 900
    Mood.SAD -> 1500
    Mood.CRY -> 1300
}

/**
 * [mood]'s title, in the mood's color with a soft glow of it behind, and its own way of
 * appearing as [progress] runs 0 → 1:
 * - Весёлое: melted into puddles, it sets into firm letters with a little wobble;
 * - Громкое: thumps in twice like a speaker on a bass hit, with a shake;
 * - Грустное: letter by letter, each settling down into place;
 * - Поплакать: written out by hand, left to right;
 * - Обычное: simply fades in.
 * "Громкое" is also heavier than Unbounded's heaviest weight: the text is drawn a second time as
 * a thick rounded outline under the letters.
 */
@Composable
internal fun MoodTitle(mood: Mood, progress: () -> Float, modifier: Modifier = Modifier) {
    val base = MOOD_COLORS.getValue(mood)
    val light = com.artemiy.player.ui.theme.LocalPlayerPalette.current.isLight
    // The mood's own color, made just light enough (dark theme) or dark enough (light theme) to
    // read well; "Обычное" is the theme's plain text color. The glow only in the dark theme — on
    // a light background it just muddied the letters.
    val textColor = when {
        mood == Mood.NORMAL -> PlayerColors.TextPrimary
        light -> lerp(base, Color.Black, 0.22f)
        else -> lerp(base, Color.White, 0.3f)
    }
    val glowColor = if (light) Color.Transparent else base.copy(alpha = 0.7f)
    // "Обычное" is written in the app's own font.
    var style = moodTitleStyle(mood).inAppFont()
    if (mood == Mood.HAPPY) {
        val step by remember {
            derivedStateOf {
                val p = FastOutSlowInEasing.transform(((progress() - 0.05f) / 0.75f).coerceIn(0f, 1f))
                (p * ClimateCrisisYears.lastIndex).toInt()
            }
        }
        style = style.copy(fontFamily = ClimateCrisisYears[step])
    }
    val density = LocalDensity.current
    val loudStroke = if (mood == Mood.LOUD) {
        val thump by remember { derivedStateOf { loudThump(progress()) } }
        with(density) { (LOUD_EXTRA_WEIGHT + 4.dp * thump).toPx() }
    } else 0f
    val shakePx = with(density) { 4.dp.toPx() }
    val softEdgePx = with(density) { 28.dp.toPx() }

    Box(
        modifier = modifier
            .bleed(TITLE_BLEED)
            .graphicsLayer {
                val p = progress()
                when (mood) {
                    Mood.NORMAL -> {
                        alpha = p
                        val s = 0.96f + 0.04f * LinearOutSlowInEasing.transform(p)
                        scaleX = s; scaleY = s
                    }
                    Mood.HAPPY -> {
                        alpha = (p * 4f).coerceAtMost(1f)
                        // Jelly: a squash and stretch that dies out as the letters set.
                        val wobble = sin(p * 2.5f * PI.toFloat()) * (1f - p) * 0.12f
                        scaleY = 1f + wobble
                        scaleX = 1f - wobble * 0.5f
                    }
                    Mood.LOUD -> {
                        alpha = (p * 6f).coerceAtMost(1f)
                        val s = 1f + 0.08f * loudThump(p)
                        scaleX = s; scaleY = s
                        translationX = sin(p * 55f) * shakePx * (1f - p) * (1f - p)
                    }
                    Mood.SAD -> Unit // per letter, below
                    Mood.CRY -> {
                        // The reveal needs its own layer to cut the not-yet-written part away.
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                }
            }
            .then(
                if (mood == Mood.CRY) Modifier.drawWithContent {
                    drawContent()
                    val edge = progress() * (size.width + softEdgePx)
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = edge - softEdgePx, endX = edge),
                        blendMode = BlendMode.DstIn,
                    )
                } else Modifier,
            )
            .padding(TITLE_BLEED),
    ) {
        if (!light) TitleText(mood, style, glowColor, progress, Modifier.blur(14.dp, BlurredEdgeTreatment.Unbounded))
        if (mood == Mood.LOUD) {
            TitleText(
                mood,
                style.copy(drawStyle = Stroke(width = loudStroke, join = StrokeJoin.Round, cap = StrokeCap.Round)),
                textColor,
                progress,
            )
        }
        TitleText(mood, style, textColor, progress)
    }
}

/** The title's text once: whole, or for "Грустное" letter by letter, each on its own timing. */
@Composable
private fun TitleText(mood: Mood, style: TextStyle, color: Color, progress: () -> Float, modifier: Modifier = Modifier) {
    if (mood != Mood.SAD) {
        Text(text = mood.label, color = color, style = style, modifier = modifier)
        return
    }
    val letters = mood.label
    val dropPx = with(LocalDensity.current) { 12.dp.toPx() }
    Row(modifier = modifier) {
        letters.forEachIndexed { i, letter ->
            Text(
                text = letter.toString(),
                color = color,
                style = style,
                modifier = Modifier.graphicsLayer {
                    // Each letter over a stretch of the whole, one after another, overlapping.
                    val local = ((progress() * (letters.length + 4) - i) / 5f).coerceIn(0f, 1f)
                    val eased = LinearOutSlowInEasing.transform(local)
                    alpha = local
                    translationY = -dropPx * (1f - eased)
                },
            )
        }
    }
}

/** Two bass thumps over the entrance, the second weaker: 0 → 1 → 0 twice. */
private fun loudThump(p: Float): Float = abs(sin(p * 2f * PI.toFloat())) * (1f - p)

/**
 * Lets whatever is drawn in the layer below reach [pad] past the title on every side (the glow,
 * the blur, the slanted handwriting) without the extra room counting in the layout.
 */
private fun Modifier.bleed(pad: Dp): Modifier = layout { measurable, constraints ->
    val p = pad.roundToPx()
    val placeable = measurable.measure(constraints.offset(2 * p, 2 * p))
    layout(placeable.width - 2 * p, placeable.height - 2 * p) { placeable.place(-p, -p) }
}

private val TITLE_BLEED = 30.dp

/** How much the outline under "Громкое" thickens its letters (half of it on each side). */
private val LOUD_EXTRA_WEIGHT = 3.dp

/** How [mood]'s title is written on its page. */
internal fun moodTitleStyle(mood: Mood): TextStyle = when (mood) {
    Mood.NORMAL -> TextStyle(fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
    Mood.LOUD -> TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.Black, fontSize = 38.sp)
    // Quiet, soft handwriting.
    Mood.SAD -> TextStyle(fontFamily = PlaypenSans, fontWeight = FontWeight.Light, fontSize = 46.sp)
    Mood.HAPPY -> TextStyle(fontFamily = ClimateCrisisYears.last(), fontSize = 37.sp)
    // A hand-written note.
    Mood.CRY -> TextStyle(fontFamily = Caveat, fontWeight = FontWeight.Medium, fontSize = 60.sp)
}
