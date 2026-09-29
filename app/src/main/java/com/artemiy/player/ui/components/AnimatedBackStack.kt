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
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.CancellationException
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
    content: @Composable (T) -> Unit,
) {
    val current = stack.last()
    // How deep each page sits, to tell "going deeper" from "going back" — kept for pages that
    // have already left the stack too, since a pop animates *from* one of those.
    val depth = remember { mutableMapOf<Any, Int>() }
    stack.forEachIndexed { index, page -> depth[page] = index }

    val seekState = remember { SeekableTransitionState(current) }
    var gestureActive by remember { mutableStateOf(false) }

    // Ordinary pushes and pops (taps, the back arrow).
    LaunchedEffect(current) {
        if (!gestureActive && seekState.targetState != current) seekState.animateTo(current)
    }

    val previous = stack.getOrNull(stack.lastIndex - 1)
    PredictiveBackHandler(enabled = previous != null) { progress ->
        val target = previous ?: return@PredictiveBackHandler
        gestureActive = true
        val start = seekState.currentState
        var fraction = 0f
        try {
            progress.collect { event ->
                fraction = event.progress
                seekState.seekTo(fraction, targetState = target)
            }
            // Finish the same movement the finger started, then make it official.
            seekState.glide(fraction, 1f, target)
            seekState.snapTo(target)
            onBack()
        } catch (e: CancellationException) {
            // Let go without going back: run the same movement backwards to where it started —
            // not a fresh "forward" transition, which made the page jump out and back in.
            withContext(NonCancellable) {
                seekState.glide(fraction, 0f, target)
                seekState.snapTo(start)
            }
        } finally {
            gestureActive = false
        }
    }

    val transition = rememberTransition(seekState, label = "backStack")
    transition.AnimatedContent(
        modifier = modifier,
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
            } else if (to < from) {
                // Both pages stay fully opaque: fading them (the one leaving out, the one underneath
                // in) let them show through each other half the way, which read as a strange
                // see-through page — noticeable or not depending on how fast the swipe was.
                ContentTransform(
                    targetContentEnter = slideInHorizontally(tween(PAGE_MS)) { -it / 8 },
                    initialContentExit = slideOutHorizontally(tween(PAGE_MS)) { it } +
                        scaleOut(tween(PAGE_MS), targetScale = 0.94f),
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
        // Every page opaque, so two pages mid-transition never show through each other.
        Box(modifier = Modifier.fillMaxSize().background(PlayerColors.Background)) {
            androidx.compose.runtime.CompositionLocalProvider(LocalPageRevealed provides revealed) {
                content(page)
            }
        }
    }
}

private const val PAGE_MS = 300

/** True inside a page that's being uncovered by going back (see [staggeredEntrance]). */
val LocalPageRevealed = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Moves a gesture-driven transition from [from] to [to] frame by frame, taking as long as that
 * stretch would take in a full page animation. */
private suspend fun <T> SeekableTransitionState<T>.glide(from: Float, to: Float, target: T) {
    val duration = (kotlin.math.abs(to - from) * PAGE_MS).coerceAtLeast(1f)
    val startNs = withFrameNanos { it }
    while (true) {
        val elapsed = (withFrameNanos { it } - startNs) / 1_000_000f
        val p = (elapsed / duration).coerceAtMost(1f)
        seekTo(from + (to - from) * FastOutSlowInEasing.transform(p), targetState = target)
        if (p >= 1f) break
    }
}
