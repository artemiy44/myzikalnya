package com.artemiy.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.R
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.expressiveUi
import kotlin.math.roundToInt

/**
 * A page's big title that folds away as the page scrolls — Apple Music's large title. Scrolling
 * down, the big header ([bigHeader]) slides up out of sight first, and a slim bar takes its place:
 * [title] small and centred, the back arrow ([onBack]) and [trailing] on either side, a hairline
 * under it. The page's own content ([content], anything that scrolls) then runs on under the bar,
 * fading softly into it rather than being cut off. Back at the top, the big title unfolds again.
 */
@Composable
fun CollapsingHeader(
    title: String,
    bigHeader: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    /** Off for pages whose top holds a fixed search field: the soft fade (and the line) would lie
     * over it like a shadow. */
    fade: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val compactPx = with(density) { COMPACT_HEIGHT.toPx() }
    // How far the header has folded, in px, and how far it can (its full height less the bar's).
    // It moves with the finger, one to one.
    var folded by remember { mutableFloatStateOf(0f) }
    val range = remember { floatArrayOf(0f) }
    val progress = { if (range[0] > 0f) (folded / range[0]).coerceIn(0f, 1f) else 0f }
    val connection = remember {
        object : NestedScrollConnection {
            // Scrolling down: the header folds before the list moves.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                val before = folded
                folded = (folded - available.y).coerceIn(0f, range[0])
                return Offset(0f, before - folded)
            }

            // Scrolling back up: only once the list is at its top does the header unfold.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f) return Offset.Zero
                val before = folded
                folded = (folded - available.y).coerceIn(0f, range[0])
                return Offset(0f, before - folded)
            }

            // Let go (or a fling runs out) halfway: the header settles to whichever end is nearer —
            // a fling up to the top opens it all the way — instead of stopping mid-fold.
            override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                val full = range[0]
                if (full <= 0f || folded <= 0f || folded >= full) return androidx.compose.ui.unit.Velocity.Zero
                val target = when {
                    available.y > 0f -> 0f
                    available.y < 0f -> full
                    folded < full / 2 -> 0f
                    else -> full
                }
                androidx.compose.animation.core.animate(folded, target, animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) { value, _ -> folded = value }
                return available
            }
        }
    }
    val expressive = expressiveUi
    val compactH = compactPx.roundToInt()
    Box(modifier = modifier.fillMaxSize().nestedScroll(connection)) {
      androidx.compose.runtime.CompositionLocalProvider(LocalHeaderFold provides progress) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The big header: it goes up with the page, all of its height, one to one — on pages
            // with a fixed search field only down to the bar's height, so the field stays clear.
            Layout(
                modifier = Modifier.fillMaxWidth().clipToBounds(),
                content = { Box(modifier = Modifier.padding(top = EXTRA_TOP)) { bigHeader() } },
            ) { measurables, constraints ->
                val big = measurables[0].measure(constraints.copy(minHeight = 0))
                val floor = if (fade) 0 else minOf(compactH, big.height)
                range[0] = (big.height - floor).coerceAtLeast(0).toFloat()
                val height = (big.height - folded.coerceIn(0f, range[0])).roundToInt().coerceAtLeast(floor)
                layout(constraints.maxWidth, height) {
                    big.placeWithLayer(0, height - big.height) {
                        val p = progress()
                        alpha = (1f - p * 1.25f).coerceIn(0f, 1f)
                        scaleX = 1f - p * 0.12f
                        scaleY = 1f - p * 0.12f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) { Column(modifier = Modifier.fillMaxSize(), content = content) }
        }
      }
        // The slim bar, over the page: it comes in as the big title is nearly gone under it —
        // back arrow, the title small in the middle, the page's own button, a hairline — and the
        // page fades softly into it rather than being cut off.
        // Once it's there it's solid to the touch too: what's gone under it (a search field) can't
        // be tapped through it. (Read through derivedStateOf — the fold moves every frame.)
        val barShown by remember { androidx.compose.runtime.derivedStateOf { progress() > 0.6f } }
        Column(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = ((progress() - 0.6f) / 0.4f).coerceIn(0f, 1f) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(COMPACT_HEIGHT)
                    .then(
                        if (barShown) {
                            Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        } else {
                            Modifier
                        },
                    )
                    .background(PlayerColors.Background)
                    .padding(horizontal = 20.dp),
            ) {
                if (onBack != null) {
                    Icon(
                        imageVector = AppIcons.Back,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = PlayerColors.TextPrimary,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(24.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() },
                    )
                }
                Text(
                    text = title,
                    color = PlayerColors.TextPrimary,
                    fontSize = if (expressive) 17.sp else 16.sp,
                    fontWeight = if (expressive) FontWeight.ExtraBold else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 44.dp),
                )
                if (trailing != null) Box(modifier = Modifier.align(Alignment.CenterEnd)) { trailing() }
            }
            if (fade) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PlayerColors.Border))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FADE_HEIGHT)
                        .background(Brush.verticalGradient(listOf(PlayerColors.Background, Color.Transparent))),
                )
            }
        }
    }
}

/** How far the page's header has folded, 0..1 — for what sits just under it (a search field)
 * to fade away with it instead of being cut off by the bar. */
val LocalHeaderFold = androidx.compose.runtime.staticCompositionLocalOf<() -> Float> { { 0f } }

/**
 * Leaves with the page's folding header: fades, and gives up its height as it goes — so what's
 * below comes right up under the bar, with no empty band where it used to be.
 */
@Composable
fun Modifier.fadesWithHeader(): Modifier {
    val fold = LocalHeaderFold.current
    return this.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val f = fold()
            val height = (placeable.height * (1f - f)).roundToInt()
            layout(placeable.width, height) {
                // Slides up as it shrinks, fading out.
                placeable.placeWithLayer(0, height - placeable.height) { alpha = (1f - f * 1.6f).coerceIn(0f, 1f) }
            }
        }
}

private val COMPACT_HEIGHT = 52.dp
private val FADE_HEIGHT = 20.dp
private val EXTRA_TOP = 8.dp
