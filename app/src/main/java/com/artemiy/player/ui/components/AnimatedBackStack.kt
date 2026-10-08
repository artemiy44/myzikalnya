package com.artemiy.player.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Shows the top of [stack] and animates between pages: a new page slides in from the right, going
 * back slides it away to the right with the previous page coming out from under it. The system
 * back gesture drives that same "going back" animation with the finger (Android's predictive
 * back): the page follows the swipe with the previous one already visible underneath, and
 * letting go either finishes it ([onBack]) or springs it back. The back button / arrow just pops
 * the stack and gets the same animation, played on its own.
 */
@Composable
fun <T : Any> AnimatedBackStack(
    stack: List<T>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Pages that open as a card growing out of the tapped item and shrink back into it on the way
     * out; null = the ordinary slide. */
    expandFrom: (T) -> ExpandSource? = { null },
    content: @Composable (T) -> Unit,
) {
    val current = stack.last()
    // Where a card-page grows from is read once, when the page is first asked about (the list it
    // grows out of is still on screen then), and kept until the page has left — the list is
    // taken apart behind it once it's open.
    val expandMemo = remember { HashMap<T, ExpandSource?>() }
    val expandOf: (T) -> ExpandSource? = { page ->
        if (expandMemo.containsKey(page)) expandMemo[page] else expandFrom(page).also { expandMemo[page] = it }
    }
    // How deep each page sits, to tell "going deeper" from "going back" — kept for pages that
    // have already left the stack too, since a pop animates *from* one of those.
    val depth = remember { mutableMapOf<Any, Int>() }
    stack.forEachIndexed { index, page -> depth[page] = index }

    val seekState = remember { SeekableTransitionState(current) }
    var gestureActive by remember { mutableStateOf(false) }

    // Ordinary pushes and pops (taps, the back arrow).
    LaunchedEffect(current) {
        if (!gestureActive && seekState.targetState != current) {
            // A page that grows out of a card is built first, hidden, before anything moves: building
            // it is the long frame (a tenth of a second or so), and a long frame in the middle of
            // an animation makes it jump ahead by that much — felt as a stutter. Built before the
            // clock starts, it costs only a short wait.
            if (expandOf(current) != null && (depth[current] ?: 0) > (depth[seekState.currentState] ?: 0)) {
                seekState.seekTo(0f, current)
                withFrameNanos { }
                withFrameNanos { }
                withFrameNanos { }
            }
            seekState.animateTo(current)
        }
    }

    val previous = stack.getOrNull(stack.lastIndex - 1)
    PredictiveBackHandler(enabled = previous != null) { progress ->
        val target = previous ?: return@PredictiveBackHandler
        gestureActive = true
        // A page that closes into its card takes longer than an ordinary one; finish the gesture
        // at the same pace as the back button would.
        val expanding = expandOf(current) != null
        val fullMs = if (expanding) CLOSE_MS else PAGE_MS
        // The card's own closing already eases along its curve, so finishing the gesture must not
        // ease again on top (it finished faster than the button and with a jerk): a straight line.
        val glideEase: androidx.compose.animation.core.Easing = if (expanding) androidx.compose.animation.core.LinearEasing else FastOutSlowInEasing
        val start = seekState.currentState
        var fraction = 0f
        try {
            progress.collect { event ->
                fraction = event.progress
                seekState.seekTo(fraction, targetState = target)
            }
            // Finish the same movement the finger started, then make it official.
            seekState.glide(fraction, 1f, target, fullMs, glideEase)
            seekState.snapTo(target)
            onBack()
        } catch (e: CancellationException) {
            // Let go without going back: run the same movement backwards to where it started —
            // not a fresh "forward" transition, which made the page jump out and back in.
            withContext(NonCancellable) {
                seekState.glide(fraction, 0f, target, fullMs, glideEase)
                seekState.snapTo(start)
            }
        } finally {
            gestureActive = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(seekState.currentState, stack.size) {
        expandMemo.keys.retainAll { it in stack || it == seekState.currentState }
    }
    val transition = rememberTransition(seekState, label = "backStack")
    // What shows around a shrunken page: the page colour darkened further than the page itself,
    // fading in as soon as the stack is deeper than its first page (invisible once a page fills it).
    val backdrop by transition.animateFloat(transitionSpec = { tween(PAGE_MS) }, label = "backdrop") { state ->
        if ((depth[state] ?: 0) > 0) 1f else 0f
    }
    val backdropColor = PlayerColors.Background
    transition.AnimatedContent(
        modifier = modifier.drawBehind {
            if (backdrop > 0f) {
                drawRect(backdropColor)
                drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f * backdrop))
            }
        },
        transitionSpec = {
            val from = depth[initialState] ?: 0
            val to = depth[targetState] ?: 0
            // Every page's layer is its depth, always — the deeper page is on top whichever way
            // things go. (A fixed "-1 for the page underneath" stuck to a page after one back
            // gesture, so going back again from it drew the page underneath over it.)
            val layer = to.toFloat()
            // Same depth: the page was swapped in place (e.g. a renamed playlist) — no movement.
            if (from == to) {
                ContentTransform(EnterTransition.None, ExitTransition.None, targetContentZIndex = layer)
            } else if (expandOf(if (to > from) targetState else initialState) != null) {
                // A card growing into its page (or back): the page animates itself, below.
                ContentTransform(EnterTransition.None, ExitTransition.None, targetContentZIndex = layer)
            } else if (to < from) {
                // The page leaving pulls away from the middle, shrinking in place and rounded like
                // the screen, with the page beneath already there; it only fades in the last
                // stretch, so the one beneath is never seen through it before that (the page
                // underneath itself stays opaque).
                ContentTransform(
                    targetContentEnter = slideInHorizontally(tween(PAGE_MS)) { -it / 8 },
                    initialContentExit = scaleOut(tween(PAGE_MS), targetScale = 0.88f, transformOrigin = BOTTOM_CENTER) +
                        fadeOut(tween(PAGE_MS * 3 / 5, delayMillis = PAGE_MS * 2 / 5)),
                    // The page leaving stays on top, uncovering the one underneath.
                    targetContentZIndex = layer,
                )
            } else {
                ContentTransform(
                    targetContentEnter = slideInHorizontally(tween(PAGE_MS)) { it / 3 } + fadeIn(tween(PAGE_MS)),
                    initialContentExit = slideOutHorizontally(tween(PAGE_MS)) { -it / 8 } + fadeOut(tween(PAGE_MS), targetAlpha = 0.5f),
                    targetContentZIndex = layer,
                )
            }
        },
        contentKey = { it },
    ) { page ->
        // A page coming back into view from under the one being left is just there already —
        // its rows don't play their entrance.
        val revealed = remember { (depth[page] ?: 0) < (depth[seekState.currentState] ?: 0) }
        // Depth: a page with another one on top of it (or on its way there) is a little smaller,
        // dimmed; it grows back to full as the page above leaves. Follows the finger during the back gesture, like the rest.
        val covered by transition.animateFloat(transitionSpec = { tween(PAGE_MS) }, label = "covered") { state ->
            if ((depth[state] ?: 0) > (depth[page] ?: 0)) 1f else 0f
        }
        // 1 while this page is the one being left (the stack is heading to a shallower page).
        val leaving by transition.animateFloat(transitionSpec = { tween(PAGE_MS) }, label = "leaving") { state ->
            if ((depth[state] ?: 0) < (depth[page] ?: 0)) 1f else 0f
        }
        val cornerPx = rememberScreenCornerRadiusPx()
        // The page as a card growing from where it was tapped to the whole screen: a rounded
        // window that opens (and closes again), the page showing in it and fading in as it grows.
        val expand = expandOf(page)
        val expandRect = expand?.bounds
        // 0 = the card, 1 = the page — on the same kind of spring as the player opening from the
        // mini bar (settling without overshoot): the colour grows out, the page shows on it
        // part-way through, and the colour melts into / out of the real card at the very start / end.
        val revealState = if (expand == null) null else transition.animateFloat(
            transitionSpec = {
                // Closing is gentler than opening — and on a timed curve, not a spring: the back
                // gesture follows the finger along this very curve, and a spring packs nearly
                // all its movement into the first instant, which showed as a jerk.
                val closing = (depth[targetState] ?: 0) < (depth[initialState] ?: 0)
                if (closing) tween(CLOSE_MS, easing = FastOutSlowInEasing)
                else androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = EXPAND_STIFFNESS, visibilityThreshold = 0.001f)
            },
            label = "reveal",
        ) { state -> if ((depth[state] ?: 0) >= (depth[page] ?: 0)) 1f else 0f }
        var origin by remember { mutableStateOf(Offset.Zero) }
        val cardCornerPx = with(androidx.compose.ui.platform.LocalDensity.current) {
            if (expand == null) 0f else maxOf(expand.cornerDp.toPx(), expand.cornerFraction * minOf(expand.bounds.width, expand.bounds.height))
        }
        // Every page opaque, so two pages mid-transition never show through each other.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (expandRect == null) Modifier else Modifier
                        .onGloballyPositioned { origin = it.positionInWindow() }
                        .graphicsLayer {
                            val e = (revealState?.value ?: 1f).coerceIn(0f, 1f)
                            alpha = (e / 0.2f).coerceIn(0f, 1f)
                            val left = expandRect!!.left - origin.x
                            val top = expandRect.top - origin.y
                            val right = expandRect.right - origin.x
                            val bottom = expandRect.bottom - origin.y
                            if (e < 1f) {
                                this.shape = RevealShape(
                                    left = left + (0f - left) * e,
                                    top = top + (0f - top) * e,
                                    right = right + (size.width - right) * e,
                                    bottom = bottom + (size.height - bottom) * e,
                                    radius = cardCornerPx * (1f - e),
                                )
                                clip = true
                            }
                        },
                )
                .graphicsLayer {
                    val s = 1f - 0.06f * covered
                    transformOrigin = BOTTOM_CENTER
                    scaleX = s
                    scaleY = s
                    // Only the top corners: the bottom edge stays put, resting on the bars below.
                    // (Also the page being uncovered underneath, so both read as rounded screens.)
                    val radius = cornerPx * maxOf(if (expandRect != null) 0f else leaving, covered)
                    if (radius > 0.5f) {
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = radius, topEnd = radius)
                        clip = true
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (covered > 0f) drawRect(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f * covered))
                }
                .background(PlayerColors.Background),
        ) {
            if (expand == null) {
                androidx.compose.runtime.CompositionLocalProvider(LocalPageRevealed provides revealed) {
                    content(page)
                }
            } else {
                // The colour the card grows with; the page shows over it, appearing once the colour is
                // mostly out.
                Box(modifier = Modifier.fillMaxSize().background(expand.fill))
                // (Built at once: the stack builds it before it starts the movement, see above.)
                run {
                    Box(
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            alpha = (((revealState?.value ?: 1f) - 0.35f) / 0.4f).coerceIn(0f, 1f)
                        },
                    ) {
                        androidx.compose.runtime.CompositionLocalProvider(LocalPageRevealed provides revealed) {
                            content(page)
                        }
                    }
                }
            }
        }
    }
}

/** A page that grows out of a tapped card: where the card is (window coordinates) and its colour. */
class ExpandSource(
    val bounds: Rect,
    val fill: androidx.compose.ui.graphics.Brush,
    /** The corner radius of what was tapped… */
    val cornerDp: androidx.compose.ui.unit.Dp = 18.dp,
    /** …or a rounder one, as a share of its size (0.5 = a circle). */
    val cornerFraction: Float = 0f,
)

private const val PAGE_MS = 340

/** The spring that opens a card into its page (the player's own is 320; this is a touch quicker). */
private const val EXPAND_STIFFNESS = 380f

/** …and how long the way back takes. */
private const val CLOSE_MS = 400

/** A rounded window with the given edges (in the page's own pixels) — the part of the page that shows. */
private class RevealShape(
    val left: Float, val top: Float, val right: Float, val bottom: Float, val radius: Float,
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ) = androidx.compose.ui.graphics.Outline.Rounded(
        androidx.compose.ui.geometry.RoundRect(left, top, right, bottom, androidx.compose.ui.geometry.CornerRadius(radius, radius)),
    )
}

/** Pages shrink toward their bottom middle: the bottom edge stays where the bars below begin. */
private val BOTTOM_CENTER = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)

/** True inside a page that's being uncovered by going back (see [staggeredEntrance]). */
val LocalPageRevealed = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Moves a gesture-driven transition from [from] to [to] frame by frame, taking as long as that
 * stretch would take in a full page animation. */
private suspend fun <T> SeekableTransitionState<T>.glide(from: Float, to: Float, target: T, fullMs: Int, ease: androidx.compose.animation.core.Easing) {
    val duration = (kotlin.math.abs(to - from) * fullMs).coerceAtLeast(1f)
    val startNs = withFrameNanos { it }
    while (true) {
        val elapsed = (withFrameNanos { it } - startNs) / 1_000_000f
        val p = (elapsed / duration).coerceAtMost(1f)
        seekTo(from + (to - from) * ease.transform(p), targetState = target)
        if (p >= 1f) break
    }
}
