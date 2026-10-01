package com.artemiy.player.ui.library

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import com.artemiy.player.data.Song
import com.artemiy.player.data.primaryGenre
import com.artemiy.player.ui.components.CoverTone
import com.artemiy.player.ui.components.rememberCoverTone
import com.artemiy.player.ui.home.drawGenreMotif
import com.artemiy.player.ui.home.genrePictureOf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * An artist page's header picture, drawn rather than made of covers: a deep gradient in the colour
 * of the artist's covers with a couple of soft glows, and over it the pictures of the artist's main
 * genres (up to three, the biggest share largest), overlapping at different sizes and angles. The
 * layout comes from the artist's name, so each artist always gets the same one; it drifts slowly.
 */
@Composable
fun BoxScope.ArtistGenreArt(artist: String, songs: List<Song>, pictures: List<String>? = null) {
    val tone = rememberCoverTone(songs)
    // No genre tags at all: a picture picked by the name — still a picture. [pictures] given:
    // exactly those (none at all for the "no genre" page).
    val named = remember(songs, pictures) { pictures ?: topGenres(songs) }
    val genres = remember(named, artist) { named.ifEmpty { listOf(artist) } }
    // Real genres we can't draw get the plain "tag" label; an artist without any is seeded by name.
    val fromGenres = named.isNotEmpty()
    val seed = remember(artist) { artist.lowercase().hashCode() }
    val phase by rememberInfiniteTransition(label = "artistArt").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart), label = "drift",
    )
    // Clipped to the header: the turned and enlarged pictures and the glows reach past its edges,
    // and below it they covered the header's fade into the page — a hard edge, and the gradient
    // showing under the header.
    Canvas(modifier = Modifier.matchParentSize().clipToBounds()) {
        val colors = palette(tone)
        drawRect(Brush.linearGradient(listOf(colors[0], colors[1]), start = Offset(0f, 0f), end = Offset(size.width, size.height)))
        // Two soft glows of neighbouring hues, for some depth.
        glow(colors[2], Offset(size.width * 0.85f, size.height * 0.2f), size.maxDimension * 0.55f)
        glow(colors[3], Offset(size.width * 0.1f, size.height * 0.8f), size.maxDimension * 0.5f)
        val random = Random(seed)
        genres.forEachIndexed { i, genre ->
            val layer = LAYERS[i]
            val dx = size.width * (layer.x + (random.nextFloat() - 0.5f) * 0.12f) + size.minDimension * 0.02f * cos(phase + i)
            val dy = size.height * (layer.y + (random.nextFloat() - 0.5f) * 0.1f) + size.minDimension * 0.015f * sin(phase * 2 + i)
            val angle = layer.angle + (random.nextFloat() - 0.5f) * 14f
            translate(dx, dy) {
                rotate(angle) {
                    scale(layer.scale) {
                        drawLayer(genre, seed + i, layer.alpha, fromGenres)
                    }
                }
            }
        }
    }
}

/**
 * Just the colours of [ArtistGenreArt] — the covers' tone as a gradient with its two glows, no
 * pictures on top — for small tiles (a year, a genre) that put their own mark over it.
 */
@Composable
fun BoxScope.ToneBackdrop(songs: List<Song>) {
    val tone = rememberCoverTone(songs)
    Canvas(modifier = Modifier.matchParentSize()) {
        val colors = palette(tone)
        drawRect(Brush.linearGradient(listOf(colors[0], colors[1]), start = Offset(0f, 0f), end = Offset(size.width, size.height)))
        glow(colors[2], Offset(size.width * 0.85f, size.height * 0.2f), size.maxDimension * 0.55f)
        glow(colors[3], Offset(size.width * 0.1f, size.height * 0.8f), size.maxDimension * 0.5f)
    }
}

/** Where and how big each genre's picture goes: the main one large, the others smaller. */
private class Layer(val x: Float, val y: Float, val scale: Float, val angle: Float, val alpha: Float)

private val LAYERS = listOf(
    Layer(x = 0.08f, y = -0.02f, scale = 1.35f, angle = -8f, alpha = 1f),
    Layer(x = -0.3f, y = -0.28f, scale = 0.85f, angle = 14f, alpha = 0.75f),
    Layer(x = -0.36f, y = 0.18f, scale = 0.6f, angle = -18f, alpha = 0.6f),
)

private fun DrawScope.drawLayer(genre: String, seed: Int, alpha: Float, fromGenres: Boolean) {
    // The genre pictures draw in translucent white; a whole layer is faded as one.
    drawContext.canvas.saveLayer(
        androidx.compose.ui.geometry.Rect(-size.width, -size.height, size.width * 2, size.height * 2),
        androidx.compose.ui.graphics.Paint().apply { this.alpha = alpha },
    )
    drawGenreMotif(genre, seed, unknownAsTag = fromGenres)
    drawContext.canvas.restore()
}

private fun DrawScope.glow(color: Color, center: Offset, radius: Float) {
    drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius), radius, center)
}

/**
 * Background colours from the covers' tone: a deep and a lighter shade, and two glows staying
 * close to that hue. As colourful as the covers themselves are — mostly grey covers with a
 * touch of pink give a grey tinged with pink, not a pink picture — and a quiet graphite for
 * covers with no colour at all.
 */
private fun palette(tone: CoverTone?): List<Color> {
    val hue = tone?.hue ?: GRAPHITE_HUE
    // How colourful the covers are, and how much they agree: covers of every colour at once
    // (an artist of singles, each its own) give a quiet ground, not whichever colour won.
    val agreement = tone?.agreement ?: 0f
    val amount = ((tone?.colourful ?: 0f) / FULLY_COLOURFUL).coerceIn(0f, 1f) * (0.25f + 0.75f * agreement * agreement)
    val sat = if (tone == null) GRAPHITE_SAT else (tone.saturation.coerceIn(0.25f, 0.7f) * (0.12f + 0.88f * amount)).coerceAtLeast(GRAPHITE_SAT)
    return listOf(
        Color.hsl(hue, sat * 0.8f, 0.14f),
        Color.hsl((hue + 10f) % 360f, sat, 0.32f),
        Color.hsl((hue + 350f) % 360f, sat, 0.45f, alpha = 0.55f),
        Color.hsl((hue + 20f) % 360f, sat * 0.9f, 0.4f, alpha = 0.4f),
    )
}

/** Covers this colourful (share of strongly coloured pixels, weighted) count as fully so. */
private const val FULLY_COLOURFUL = 0.18f
private const val GRAPHITE_HUE = 230f
private const val GRAPHITE_SAT = 0.06f

/** The artist's main genres, up to three with a real share of the songs, one per picture. */
private fun topGenres(songs: List<Song>): List<String> {
    val counted = songs.mapNotNull(::primaryGenre)
        .groupBy { genrePictureOf(it) }
        .map { (_, names) -> names.groupingBy { it.lowercase() }.eachCount().maxBy { it.value }.key to names.size }
        .sortedByDescending { it.second }
    val total = counted.sumOf { it.second }.coerceAtLeast(1)
    return counted.filterIndexed { i, (_, n) -> i == 0 || n.toFloat() / total >= 0.12f }.take(3).map { it.first }
}
