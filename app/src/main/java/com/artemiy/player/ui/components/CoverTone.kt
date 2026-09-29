package com.artemiy.player.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.artemiy.player.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The main colour of a set of covers: hue in degrees and saturation, 0..1. */
class CoverTone(val hue: Float, val saturation: Float)

/**
 * The shared colour of [songs]' covers (up to four different albums): the hue their most colourful
 * pixels agree on. Null while the covers load, and for black-and-white ones.
 */
@Composable
fun rememberCoverTone(songs: List<Song>): CoverTone? {
    val picks = remember(songs) { songs.distinctBy { it.album.lowercase() }.take(4) }
    // A fixed number of calls, so the composable's shape never changes.
    val b0 = rememberAlbumArtBitmap(picks.getOrNull(0)?.uri)
    val b1 = rememberAlbumArtBitmap(picks.getOrNull(1)?.uri)
    val b2 = rememberAlbumArtBitmap(picks.getOrNull(2)?.uri)
    val b3 = rememberAlbumArtBitmap(picks.getOrNull(3)?.uri)
    val tone by produceState<CoverTone?>(null, b0, b1, b2, b3) {
        val bitmaps = listOfNotNull(b0, b1, b2, b3)
        if (bitmaps.isNotEmpty()) value = withContext(Dispatchers.Default) { toneOf(bitmaps) }
    }
    return tone
}

private fun toneOf(bitmaps: List<Bitmap>): CoverTone? {
    val hsv = FloatArray(3)
    var x = 0.0
    var y = 0.0
    var satSum = 0.0
    var weightSum = 0.0
    val steps = 20
    for (source in bitmaps) {
        val bmp = if (source.config == Bitmap.Config.HARDWARE) source.copy(Bitmap.Config.ARGB_8888, false) ?: continue else source
        for (i in 0 until steps) for (j in 0 until steps) {
            val px = bmp.getPixel(i * (bmp.width - 1) / (steps - 1), j * (bmp.height - 1) / (steps - 1))
            android.graphics.Color.colorToHSV(px, hsv)
            val weight = (hsv[1] * hsv[2]).toDouble().let { it * it }
            if (weight < 0.01) continue
            val angle = Math.toRadians(hsv[0].toDouble())
            x += kotlin.math.cos(angle) * weight
            y += kotlin.math.sin(angle) * weight
            satSum += hsv[1] * weight
            weightSum += weight
        }
    }
    if (weightSum < 0.5) return null
    val hue = ((Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0).toFloat()
    return CoverTone(hue, (satSum / weightSum).toFloat())
}
