package com.artemiy.player.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Where each cover of a list or grid is on the screen, by a key for the page it opens. A page that
 * opens as a card growing out of its cover (see AnimatedBackStack's `expandFrom`) asks for it here
 * when it's pushed — so the list rows, 2- and 3-wide grids all work the same, without every tap
 * handler having to pass its position along.
 */
class ExpandAnchors {
    private class Anchor(val coords: LayoutCoordinates, val cornerDp: Dp, val cornerFraction: Float, val washKey: String?)

    private val map = HashMap<String, Anchor>()

    internal fun put(key: String, coords: LayoutCoordinates, cornerDp: Dp, cornerFraction: Float, washKey: String?) {
        map[key] = Anchor(coords, cornerDp, cornerFraction, washKey)
    }

    /** The card for [key] as it is right now — null when that cover isn't on screen. */
    fun sourceFor(key: String): ExpandSource? {
        val a = map[key] ?: return null
        if (!a.coords.isAttached) return null
        val bounds: Rect = a.coords.boundsInWindow()
        if (bounds.isEmpty) return null
        val tone = a.washKey?.let { peekHeroWash(it) } ?: Color(0xFF1B1B1F)
        return ExpandSource(bounds, SolidColor(tone) as Brush, a.cornerDp, a.cornerFraction)
    }
}

/** The anchors of the Library's lists. */
val LibraryAnchors = ExpandAnchors()

/**
 * Marks this cover as the start of the page [key] opens. [cornerDp] is the cover's corner radius,
 * [cornerFraction] (of its size) a rounder one (0.5 = a circle); [washKey] says which cached
 * colour the growing card takes.
 */
fun Modifier.expandAnchor(
    anchors: ExpandAnchors,
    key: String,
    cornerDp: Dp = 10.dp,
    cornerFraction: Float = 0f,
    washKey: String? = null,
): Modifier = onGloballyPositioned { anchors.put(key, it, cornerDp, cornerFraction, washKey) }

/** Reads a cover's colour ahead of time (and keeps it), so a card growing out of it has it from the first frame. */
@Composable
fun WarmHeroWash(uri: Uri?, key: String) {
    if (uri == null) return
    rememberHeroWash(rememberAlbumArtBitmap(uri, ART_SIZE_THUMB), key)
}
