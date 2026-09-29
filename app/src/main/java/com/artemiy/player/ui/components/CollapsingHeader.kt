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
    Column(modifier = modifier.fillMaxSize().nestedScroll(connection)) {
        Layout(
            modifier = Modifier.fillMaxWidth().clipToBounds(),
            content = {
                // Some air above the big title: it has room to leave, and looks settled at rest.
                Box(modifier = Modifier.padding(top = EXTRA_TOP)) { bigHeader() }
                // The slim bar: back arrow, the title small in the middle, the page's own button.
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
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
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(if (fade) PlayerColors.Border else Color.Transparent))
            },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val big = measurables[0].measure(constraints.copy(minHeight = 0))
            val compactH = compactPx.roundToInt()
            range[0] = (big.height - compactH).coerceAtLeast(0).toFloat()
            val height = (big.height - folded.coerceIn(0f, range[0])).roundToInt().coerceAtLeast(minOf(compactH, big.height))
            val bar = measurables[1].measure(Constraints.fixed(width, compactH))
            val line = measurables[2].measure(Constraints.fixed(width, 1.dp.roundToPx()))
            layout(width, height) {
                // The big header rises faster than the page, shrinking a little and fading, as the
                // small title comes in above it — plainly moving even though the header is short.
                big.placeWithLayer(0, height - big.height) {
                    val p = progress()
                    alpha = (1f - p * 1.4f).coerceIn(0f, 1f)
                    translationY = -p * TITLE_RISE.toPx()
                    scaleX = 1f - p * 0.18f
                    scaleY = 1f - p * 0.18f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                }
                bar.placeWithLayer(0, 0) { alpha = ((progress() - 0.55f) / 0.45f).coerceIn(0f, 1f) }
                line.placeWithLayer(0, height - line.height) { alpha = progress() * 0.8f }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxSize(), content = content)
            // The content fades into the bar at its top edge — no hard cut where it passes under.
            if (fade) Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FADE_HEIGHT)
                    .graphicsLayer { alpha = progress() }
                    .background(Brush.verticalGradient(listOf(PlayerColors.Background, Color.Transparent))),
            )
        }
    }
}

private val COMPACT_HEIGHT = 52.dp
private val FADE_HEIGHT = 20.dp
private val EXTRA_TOP = 8.dp
private val TITLE_RISE = 28.dp
