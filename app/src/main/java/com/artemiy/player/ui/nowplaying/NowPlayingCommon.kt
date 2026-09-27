package com.artemiy.player.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.artemiy.player.ui.theme.NowPlayingPalette
import com.artemiy.player.ui.theme.PlayerColors
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable

// Bits shared by all of Now Playing's parts.

internal enum class CenterMode { Art, Lyrics, Queue }

/**
 * Whether the player is actually on screen. The classic player stays built while closed (so
 * opening it doesn't stall to build it all again) — while hidden, whatever keeps animating by
 * itself (the living blur, the lyrics' smooth scrolling) stops, so it costs nothing.
 */
val LocalNowPlayingActive = compositionLocalOf { true }

/**
 * What the player was showing — kept by whoever hosts the player (outside it), so closing it and
 * opening it again comes back to the same view instead of the cover every time.
 */
class NowPlayingMemory {
    /** Classic style: cover, lyrics or queue. */
    internal var centerMode by mutableStateOf(CenterMode.Art)
    /** Expressive style: lyrics in place of the cover. */
    internal var expressiveLyrics by mutableStateOf(false)
    /** Expressive style: the queue sheet pulled up. */
    internal var expressiveQueueOpen by mutableStateOf(false)
}

/** The muted gray used for secondary text/icons on this screen. Normally exactly
 * [PlayerColors.TextSecondary]; over an album-art background it's lifted toward white just enough
 * to stay readable when the art behind it is bright (see [readableSecondaryColor]). */
internal val LocalAdaptiveSecondaryColor = compositionLocalOf { NowPlayingPalette.textSecondary }

/** How far (in px, converted from [FADE_SPAN]) before an edge a row starts fading — fixed and the
 * same on all four edges (Lyrics top/bottom, Queue top/bottom), regardless of how tall the
 * floating panel sitting at that edge happens to be. */
internal val FADE_SPAN = 72.dp

/**
 * Fades ONE list row's own alpha down as it nears the top/bottom edges of its LazyColumn's
 * viewport — applied per-item, not as a single mask over the whole list. The whole-list
 * `BlendMode.DstIn` + `CompositingStrategy.Offscreen` mask approach (a technique that works fine
 * in reference code elsewhere) produced zero visible effect on this device/build no matter how it
 * was wired up — the same category of "this normally-reliable technique silently no-ops here"
 * problem this project already hit twice with `Modifier.blur()` on solid shapes. This is the
 * fallback: plain per-item `alpha`, computed from [state]'s own live layout info (so it updates
 * every scroll frame via the draw phase, no recomposition), matched to this item by
 * [absoluteIndex] — its position among ALL items in the LazyColumn, not just within one
 * `item`/`itemsIndexed` block (Compose numbers items sequentially across the whole list in
 * declaration order). [topPx]/[bottomPx] are read lazily for the same reason.
 *
 * [topPx]/[bottomPx] only mark WHERE the edge of the floating panel sits (so a row reaches
 * alpha 0 exactly as it slides under it, never before or after) — the fade's own length is
 * always [FADE_SPAN], not the panel's height. Coupling the two (an earlier version did) made
 * the fade under the tall control panel comically slow/mushy compared to the short, crisp one
 * under the mini-header, even though both used the same formula.
 *
 * IMPORTANT coordinate gotcha that caused the fade to end early/late by a constant offset: a
 * `LazyListItemInfo.offset` of 0 is NOT the top of the viewport — per Compose's own docs it's
 * the point right AFTER `beforeContentPadding` (top content padding), and `visibleItemsInfo`
 * offsets are relative to that same zero point. Comparing that offset directly against
 * [topPx]/[bottomPx] (which are the header/panel's raw pixel heights, i.e. measured from the
 * true top/bottom of the screen) was off by exactly the content padding amount. Anchoring to
 * `info.viewportStartOffset`/`viewportEndOffset` instead — which already bake in
 * `beforeContentPadding` per Compose's own definition — fixes that for good.
 */
internal fun Modifier.fadeInList(
    state: LazyListState,
    absoluteIndex: Int,
    topPx: () -> Int,
    bottomPx: () -> Int,
): Modifier = this.graphicsLayer {
    val info = state.layoutInfo
    val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == absoluteIndex } ?: return@graphicsLayer
    val span = FADE_SPAN.toPx().coerceAtLeast(1f)
    val topEdge = topPx().toFloat() + info.viewportStartOffset
    val bottomEdge = info.viewportEndOffset - bottomPx().toFloat()
    val center = itemInfo.offset + itemInfo.size / 2f
    val topAlpha = ((center - topEdge) / span).coerceIn(0f, 1f)
    val bottomAlpha = ((bottomEdge - center) / span).coerceIn(0f, 1f)
    alpha = minOf(topAlpha, bottomAlpha)
}

internal fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * For the classic player's opening: draws this element flying from [origin] — the matching piece
 * of the mini player, in screen coordinates — to where it's actually laid out, as [progress] goes
 * from 0 to 1. Scaled evenly; lined up on the top-left corner, or on the center.
 */
@Composable
internal fun Modifier.flyFrom(
    origin: Rect?,
    progress: () -> Float,
    fromCenter: Boolean,
    /** How big it starts, when the bounds' heights don't say (text lines are as tall at 13sp as
     * at 20sp — the line height is the same). Null = the height ratio. */
    startScale: Float? = null,
    /** Fade in over the first part of the way (0..this), e.g. while a look-alike fades out. */
    fadeInUntil: Float? = null,
    onTarget: (Rect) -> Unit = {},
): Modifier {
    var target by remember { mutableStateOf<Rect?>(null) }
    // While the player is hidden it's parked off screen — flying this back over the mini player
    // there would put an invisible copy right on top of it, catching its taps.
    val active = LocalNowPlayingActive.current
    return onGloballyPositioned { target = it.boundsInRoot(); onTarget(it.boundsInRoot()) }
        .graphicsLayer {
            if (!active) return@graphicsLayer
            alpha = if (fadeInUntil != null) (progress() / fadeInUntil).coerceIn(0f, 1f) else 1f
            val p = FLIGHT_EASING.transform(progress())
            val to = target
            if (p < 1f && to != null && origin != null && to.height > 0f) {
                val scale = androidx.compose.ui.util.lerp(startScale ?: (origin.height / to.height), 1f, p)
                transformOrigin = if (fromCenter) TransformOrigin.Center else TransformOrigin(0f, 0f)
                val dx = if (fromCenter) origin.center.x - to.center.x else origin.left - to.left
                val dy = if (fromCenter) origin.center.y - to.center.y else origin.top - to.top
                translationX = dx * (1f - p)
                translationY = dy * (1f - p)
                scaleX = scale
                scaleY = scale
            }
        }
}
