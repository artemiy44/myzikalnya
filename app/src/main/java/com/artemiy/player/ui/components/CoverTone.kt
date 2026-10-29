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
