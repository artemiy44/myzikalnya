package com.artemiy.player.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.artemiy.player.data.Song
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The main colour of a set of covers: hue in degrees and saturation, 0..1 — how much of the covers
 * is coloured at all ([colourful], 0..1: nearly grey covers with a small red detail are "red" but
 * only a little) — and how much they agree on that hue ([agreement], 0..1: all red is 1; one red,
 * one green, one blue, one pink is low).
 */
class CoverTone(val hue: Float, val saturation: Float, val colourful: Float = 1f, val agreement: Float = 1f)

/**
 * The shared colour of [songs]' covers — up to eight different albums, spread over all of them:
 * the hue they agree on, each cover having one equal say (a single loud cover doesn't outvote the
 * rest). Null while the covers load, and for black-and-white ones.
 */
@Composable
fun rememberCoverTone(songs: List<Song>): CoverTone? {
    val picks = remember(songs) {
        val albums = songs.distinctBy { it.album.lowercase() }
        if (albums.size <= TONE_COVERS) albums else List(TONE_COVERS) { albums[it * albums.size / TONE_COVERS] }
    }
    // A fixed number of calls, so the composable's shape never changes.
    val b0 = rememberAlbumArtBitmap(picks.getOrNull(0)?.uri)
    val b1 = rememberAlbumArtBitmap(picks.getOrNull(1)?.uri)
    val b2 = rememberAlbumArtBitmap(picks.getOrNull(2)?.uri)
    val b3 = rememberAlbumArtBitmap(picks.getOrNull(3)?.uri)
    val b4 = rememberAlbumArtBitmap(picks.getOrNull(4)?.uri)
    val b5 = rememberAlbumArtBitmap(picks.getOrNull(5)?.uri)
    val b6 = rememberAlbumArtBitmap(picks.getOrNull(6)?.uri)
    val b7 = rememberAlbumArtBitmap(picks.getOrNull(7)?.uri)
    val bitmaps = listOfNotNull(b0, b1, b2, b3, b4, b5, b6, b7)
    val tone by produceState<CoverTone?>(null, bitmaps) {
        if (bitmaps.isNotEmpty()) value = withContext(Dispatchers.Default) { toneOf(bitmaps) }
    }
    return tone
}

private const val TONE_COVERS = 8

private fun toneOf(bitmaps: List<Bitmap>): CoverTone? {
    val hsv = FloatArray(3)
    // Per cover: the direction its coloured pixels point on the colour wheel (with how much they
    // agree among themselves as its length), how saturated they are, and how much of it is coloured.
    var x = 0.0
    var y = 0.0
    var satSum = 0.0
    var colourSum = 0.0
    var covers = 0
    val steps = 20
    for (source in bitmaps) {
        val bmp = if (source.config == Bitmap.Config.HARDWARE) source.copy(Bitmap.Config.ARGB_8888, false) ?: continue else source
        var cx = 0.0
        var cy = 0.0
        var cSat = 0.0
        var cWeight = 0.0
        for (i in 0 until steps) for (j in 0 until steps) {
            val px = bmp.getPixel(i * (bmp.width - 1) / (steps - 1), j * (bmp.height - 1) / (steps - 1))
            android.graphics.Color.colorToHSV(px, hsv)
            val weight = (hsv[1] * hsv[2]).toDouble().let { it * it }
            if (weight < 0.01) continue
            val angle = Math.toRadians(hsv[0].toDouble())
            cx += kotlin.math.cos(angle) * weight
            cy += kotlin.math.sin(angle) * weight
            cSat += hsv[1] * weight
            cWeight += weight
        }
        covers++
        if (cWeight < 0.5 / TONE_COVERS) continue
        val colourful = cWeight / (steps * steps)
        x += cx / cWeight * colourful
        y += cy / cWeight * colourful
        satSum += cSat / cWeight * colourful
        colourSum += colourful
    }
    if (colourSum <= 0.0 || covers == 0) return null
    val hue = ((Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0).toFloat()
    val agreement = (kotlin.math.sqrt(x * x + y * y) / colourSum).toFloat().coerceIn(0f, 1f)
    return CoverTone(hue, (satSum / colourSum).toFloat(), (colourSum / covers).toFloat(), agreement)
}

/** The tone of a single cover (the one playing, say). */
@Composable
fun rememberCoverTone(uri: android.net.Uri?): CoverTone? {
    val bitmap = rememberAlbumArtBitmap(uri)
    val tone by produceState<CoverTone?>(null, bitmap) {
        value = bitmap?.let { withContext(Dispatchers.Default) { toneOf(listOf(it)) } }
    }
    return tone
}

/** A surface of the current theme leaning [amount] of the way towards [tone] — the expressive
 * style's tonal cards. Just the surface for black-and-white covers. */
@Composable
fun tonedSurface(tone: CoverTone?, amount: Float = 0.22f, base: Color = PlayerColors.Surface): Color {
    if (tone == null) return base
    val dark = PlayerColors.Background.luminance() < 0.5f
    val color = Color.hsv(tone.hue, tone.saturation.coerceIn(0.3f, 0.8f), if (dark) 0.6f else 0.88f)
    return lerp(base, color, amount)
}


/**
 * Up to three colours the cover really has: its colourful pixels sorted by hue into 24 steps, the strongest step first, then
 * the strongest one at least 45° away from it, then a third away from both. A cover of one colour (red, black and white)
 * gets that colour plus a paler and a deeper one of it. Null for a cover with no colour at all (grey, black and white).
 */
@Composable
fun rememberCoverSwatches(uri: android.net.Uri?): List<Color>? {
    val bitmap = rememberAlbumArtBitmap(uri)
    val swatches by produceState<List<Color>?>(null, bitmap) {
        value = bitmap?.let { withContext(Dispatchers.Default) { swatchesOf(it) } }
    }
    return swatches
}

private fun swatchesOf(bitmap: android.graphics.Bitmap): List<Color>? {
    val steps = 24
    val weight = FloatArray(steps)
    val satSum = FloatArray(steps)
    val valSum = FloatArray(steps)
    val hsv = FloatArray(3)
    val stride = maxOf(1, minOf(bitmap.width, bitmap.height) / 48)
    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            android.graphics.Color.colorToHSV(bitmap.getPixel(x, y), hsv)
            // Greys, blacks and whites are no colour of the cover's; and a pixel counts as it is strong.
            if (hsv[1] >= 0.25f && hsv[2] >= 0.25f) {
                val bucket = ((hsv[0] / 360f * steps).toInt()).coerceIn(0, steps - 1)
                val w = hsv[1] * hsv[2]
                weight[bucket] += w
                satSum[bucket] += hsv[1] * w
                valSum[bucket] += hsv[2] * w
            }
            x += stride
        }
        y += stride
    }
    val total = weight.sum()
    if (total <= 0f) return null
    fun hueOf(bucket: Int) = (bucket + 0.5f) * 360f / steps
    fun distance(a: Int, b: Int): Float {
        val d = kotlin.math.abs(hueOf(a) - hueOf(b))
        return minOf(d, 360f - d)
    }
    val chosen = mutableListOf<Int>()
    val order = (0 until steps).sortedByDescending { weight[it] }
    for (b in order) {
        if (weight[b] <= 0f) break
        if (chosen.isEmpty() || (weight[b] >= weight[chosen[0]] * 0.15f && chosen.all { distance(it, b) >= 45f })) chosen += b
        if (chosen.size == 3) break
    }
    fun colourOf(b: Int) = Color.hsv(hueOf(b), (satSum[b] / weight[b]).coerceIn(0f, 1f), (valSum[b] / weight[b]).coerceIn(0f, 1f))
    val colours = chosen.map(::colourOf).toMutableList()
    // Fewer than three colours: the missing ones are the first's paler and deeper selves (a touch to either side), not new hues.
    val first = chosen[0]
    val s0 = (satSum[first] / weight[first]).coerceIn(0f, 1f)
    val v0 = (valSum[first] / weight[first]).coerceIn(0f, 1f)
    if (colours.size < 2) colours += Color.hsv((hueOf(first) + 14f) % 360f, s0 * 0.55f, minOf(1f, v0 * 1.15f))
    if (colours.size < 3) colours += Color.hsv((hueOf(first) + 346f) % 360f, minOf(1f, s0 * 1.05f), v0 * 0.7f)
    return colours.take(3)
}
