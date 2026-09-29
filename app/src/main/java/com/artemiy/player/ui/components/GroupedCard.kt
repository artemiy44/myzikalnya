package com.artemiy.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.expressiveUi

/**
 * Expressive style: rows of a list become one stack of cards — little gaps between them, the
 * first and last rounder than the ones in between — each [inner] padded at the sides. Does
 * nothing in the classic style.
 */
@Composable
fun Modifier.groupedCard(index: Int, count: Int, inner: Dp = 12.dp): Modifier =
    groupedCard(first = index == 0, last = index == count - 1, inner = inner)

/** The same, for a group that isn't a whole list (one disc of an album, say). */
@Composable
fun Modifier.groupedCard(first: Boolean, last: Boolean, inner: Dp = 12.dp): Modifier {
    if (!expressiveUi) return this
    return this
        .padding(bottom = if (last) 0.dp else GROUP_GAP)
        .clip(
            RoundedCornerShape(
                topStart = if (first) OUTER else INNER,
                topEnd = if (first) OUTER else INNER,
                bottomStart = if (last) OUTER else INNER,
                bottomEnd = if (last) OUTER else INNER,
            ),
        )
        .background(PlayerColors.Surface)
        .padding(horizontal = inner)
}

/** The side margin of a page's content: grouped cards sit a little closer to the edges. */
val pageGutter: Dp @Composable get() = if (expressiveUi) 12.dp else 20.dp

/** A tonal accent fill for small shapes on cards (icon tiles and such); plain surface in classic. */
val tonalAccent: Color
    @Composable get() = if (expressiveUi) lerp(PlayerColors.Surface, PlayerColors.AccentStandalone, 0.2f) else PlayerColors.Surface

private val OUTER = 22.dp
private val INNER = 6.dp
private val GROUP_GAP = 3.dp

/** When a list first appeared — rows showing up right then play the entrance, later ones don't. */
class Entrance internal constructor(internal val enabled: Boolean) {
    internal val born = android.os.SystemClock.uptimeMillis()
}

/** Off on a page uncovered by going back — that one is simply already there. */
@Composable
fun rememberEntrance(): Entrance {
    val revealed = LocalPageRevealed.current
    return androidx.compose.runtime.remember { Entrance(enabled = !revealed) }
}

/**
 * Expressive style: the first rows of a freshly opened list rise into place one after another with
 * a little spring. Rows scrolled in later just appear. Nothing in the classic style.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int, entrance: Entrance): Modifier {
    if (!expressiveUi) return this
    val animate = androidx.compose.runtime.remember {
        entrance.enabled && index < ENTRANCE_ROWS && android.os.SystemClock.uptimeMillis() - entrance.born < ENTRANCE_WINDOW_MS
    }
    if (!animate) return this
    val progress = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * ENTRANCE_STEP_MS)
        progress.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 320f))
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 28.dp.toPx()
    }
}

private const val ENTRANCE_ROWS = 14
private const val ENTRANCE_WINDOW_MS = 500L
private const val ENTRANCE_STEP_MS = 30L
