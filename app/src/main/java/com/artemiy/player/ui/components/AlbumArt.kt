package com.artemiy.player.ui.components

import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import kotlinx.coroutines.withContext

/** Thumbnail size for lists/grids/mini-player — small and fast. */
const val ART_SIZE_THUMB = 300

/** Larger size for full-screen art (Now Playing) — sharper, costs more memory. */
const val ART_SIZE_FULL = 720

private object AlbumArtCache {
    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 6).toInt()
    private val cache = object : LruCache<Pair<Long, Int>, Bitmap>(maxKb) {
        override fun sizeOf(key: Pair<Long, Int>, value: Bitmap) = value.byteCount / 1024
    }

    fun get(id: Long, size: Int): Bitmap? = cache.get(id to size)
    fun put(id: Long, size: Int, bitmap: Bitmap) = cache.put(id to size, bitmap)
}

@Composable
fun AlbumArt(uri: Uri?, modifier: Modifier = Modifier, size: Int = ART_SIZE_THUMB) {
    val context = LocalContext.current
    val id = remember(uri) { uri?.let { runCatching { ContentUris.parseId(it) }.getOrNull() } }
    val bitmap by produceState<Bitmap?>(initialValue = id?.let { AlbumArtCache.get(it, size) }, key1 = uri, key2 = size) {
        if (uri == null || id == null || Build.VERSION.SDK_INT < 29) {
            value = null
            return@produceState
        }
        val cached = AlbumArtCache.get(id, size)
        if (cached != null) {
            value = cached
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(size, size), null)
            }.getOrNull()?.also { bmp -> AlbumArtCache.put(id, size, bmp) }
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(listOf(Color(0xFF3A3A3C), Color(0xFF232325)))
            )
        )
    }
}
