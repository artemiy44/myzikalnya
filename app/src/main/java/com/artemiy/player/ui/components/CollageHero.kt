package com.artemiy.player.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.artemiy.player.data.Song

/**
 * Header art for pages that gather many albums (an artist, a playlist): their covers in a 2×3
 * collage, blurred as a whole into a soft wash of their colors. Single-album pages keep the sharp
 * cover instead.
 */
private const val COLLAGE_COVERS = 6

/** Taller than an album's header: room for the collage's three rows above the title. */
val COLLAGE_HERO_HEIGHT = 560.dp

/** The blurred collage for [songs] (one cover per album; fewer albums repeat), built from small
 * cached thumbnails. Null until at least one cover has loaded. */
@Composable
fun rememberBlurredCollage(songs: List<Song>): Bitmap? {
    val covers = remember(songs) {
        val distinct = songs.groupBy { it.album }.values.map { it.first() }
        if (distinct.isEmpty()) emptyList() else (0 until COLLAGE_COVERS).map { distinct[it % distinct.size] }
    }
    val thumbs = covers.map { rememberAlbumArtBitmap(it.uri, ART_SIZE_THUMB) }
    return remember(thumbs) { if (thumbs.all { it == null }) null else blurredCollage(thumbs) }
}

/** Fills the header with [collage] (fading in once it's ready) over the usual placeholder. */
@Composable
fun BoxScope.BlurredCollageArt(collage: Bitmap?) {
    Box(modifier = Modifier.fillMaxSize().background(placeholderArtBrush()))
    Crossfade(targetState = collage, animationSpec = tween(400), label = "collageBlur") { bitmap ->
        if (bitmap != null) {
            Image(
                bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
