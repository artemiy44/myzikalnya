package com.artemiy.player.ui.components

import com.artemiy.player.ui.theme.LocalPlayerPalette
import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Thumbnail size for lists/grids/mini-player — small and fast. */
const val ART_SIZE_THUMB = 300

/** Larger size for full-screen art (Now Playing) — sharper, costs more memory. */
const val ART_SIZE_FULL = 720

/**
 * Covers are decoded a few at a time, not with every IO thread at once: flinging through a long
 * list used to start dozens of decodes together, and they took the CPU away from drawing the
 * list itself. A decode for a row that has already scrolled away is cancelled.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private val ArtDecoding = Dispatchers.IO.limitedParallelism(3)

private const val ART_LOAD_DELAY_MS = 60L

private object AlbumArtCache {
    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 6).toInt()
    private val cache = object : LruCache<Pair<Long, Int>, Bitmap>(maxKb) {
        override fun sizeOf(key: Pair<Long, Int>, value: Bitmap) = value.byteCount / 1024
    }

    fun get(id: Long, size: Int): Bitmap? = cache.get(id to size)
    fun put(id: Long, size: Int, bitmap: Bitmap) = cache.put(id to size, bitmap)
}

/** Loads (and caches) the raw thumbnail bitmap behind [AlbumArt] — reusable wherever the pixels
 * themselves are needed too, e.g. to extract a color palette for a background effect. */
@Composable
fun rememberAlbumArtBitmap(uri: Uri?, size: Int = ART_SIZE_THUMB): Bitmap? {
    val context = LocalContext.current
    val id = remember(uri) { uri?.let { runCatching { ContentUris.parseId(it) }.getOrNull() } }
    // A big cover not decoded yet starts as its small version, if that's at hand — never a blank
    // frame (it flashed when the player's cover came back from the lyrics or the queue).
    fun cachedOrSmaller(id: Long): Bitmap? = AlbumArtCache.get(id, size) ?: if (size > ART_SIZE_THUMB) AlbumArtCache.get(id, ART_SIZE_THUMB) else null
    val bitmap by produceState<Bitmap?>(initialValue = id?.let(::cachedOrSmaller), key1 = uri, key2 = size) {
        if (uri == null || id == null || Build.VERSION.SDK_INT < 29) {
            value = null
            return@produceState
        }
        val cached = AlbumArtCache.get(id, size)
        if (cached != null) {
            value = cached
            return@produceState
        }
        // Whatever's at hand straight away — but never nothing: until the new cover is decoded the
        // previous one stays (a blank frame flashed the cover and the blurred background).
        cachedOrSmaller(id)?.let { value = it }
        // A row only flashing past during a fling never starts decoding (the big player's cover
        // isn't in any list, so it doesn't wait).
        if (size <= ART_SIZE_THUMB) delay(ART_LOAD_DELAY_MS)
        val cancel = android.os.CancellationSignal()
        coroutineContext.job.invokeOnCompletion { cancel.cancel() }
        value = withContext(ArtDecoding) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(size, size), cancel)
            }.getOrNull()?.also { bmp -> AlbumArtCache.put(id, size, bmp) } ?: value
        }
    }
    return bitmap
}

/** Stand-in for missing cover art — a soft gradient in the current theme's tones. */
@Composable
fun placeholderArtBrush(): Brush =
    if (LocalPlayerPalette.current.isLight) {
        Brush.linearGradient(listOf(Color(0xFFD9D9DC), Color(0xFFC7C7CB)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF3A3A3C), Color(0xFF232325)))
    }

/**
 * A song's cover, or [MissingArt] without one. [crossfade]: the next song's cover eases in over
 * the previous one instead of replacing it at once (the big player's cover).
 */
@Composable
fun AlbumArt(uri: Uri?, modifier: Modifier = Modifier, size: Int = ART_SIZE_THUMB, crossfade: Boolean = false) {
    val bmp = rememberAlbumArtBitmap(uri, size)
    if (crossfade) {
        androidx.compose.animation.Crossfade(targetState = bmp, animationSpec = androidx.compose.animation.core.tween(COVER_FADE_MS), modifier = modifier, label = "cover") { shown ->
            if (shown != null) {
                Image(bitmap = shown.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                MissingArt(Modifier.fillMaxSize())
            }
        }
        return
    }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        MissingArt(modifier)
    }
}

/**
 * A song without a cover: plain white with a black "♫" in a light theme, black with a white one
 * in a dark theme — whatever the size, list row to the big player.
 */
@Composable
fun MissingArt(modifier: Modifier = Modifier) {
    val light = LocalPlayerPalette.current.isLight
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.background(if (light) Color.White else Color.Black),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = com.artemiy.player.ui.icons.TablerSongs,
            contentDescription = null,
            tint = if (light) Color.Black else Color.White,
            modifier = androidx.compose.ui.Modifier.size(minOf(maxWidth, maxHeight) * 0.4f),
        )
    }
}

private const val COVER_FADE_MS = 280
