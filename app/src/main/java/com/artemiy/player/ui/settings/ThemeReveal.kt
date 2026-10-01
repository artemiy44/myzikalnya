package com.artemiy.player.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Picking a background colour doesn't just switch it: the new colour grows from the circle you
 * touched until it fills the screen, and only then is it really applied (the page underneath
 * already looks the same, so the overlay just melts away).
 */
class ThemeReveal internal constructor(private val scope: CoroutineScope) {
    internal var color by mutableStateOf(Color.Transparent)
    internal var center by mutableStateOf(Offset.Zero)
    internal val progress = Animatable(0f)
    internal val alpha = Animatable(0f)
    private var running = false

    fun play(from: Offset, to: Color, apply: () -> Unit) {
        if (running) return
        running = true
        scope.launch {
            color = to
            center = from
            progress.snapTo(0f)
            alpha.snapTo(1f)
            progress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
            apply()
            delay(120) // lets the new theme reach the page
            alpha.animateTo(0f, tween(200))
            running = false
        }
    }
}

val LocalThemeReveal = compositionLocalOf<ThemeReveal?> { null }

@Composable
internal fun rememberThemeReveal(): ThemeReveal {
    val scope = rememberCoroutineScope()
    return remember { ThemeReveal(scope) }
}

/** Draws the growing circle over everything else of the settings. */
@Composable
internal fun ThemeRevealOverlay(reveal: ThemeReveal) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow(); size = it.size }
            .graphicsLayer { alpha = reveal.alpha.value },
    ) {
        if (reveal.alpha.value <= 0f) return@Canvas
        val c = reveal.center - origin
        val far = maxOf(
            (c - Offset(0f, 0f)).getDistance(),
            (c - Offset(size.width.toFloat(), 0f)).getDistance(),
            (c - Offset(0f, size.height.toFloat())).getDistance(),
            (c - Offset(size.width.toFloat(), size.height.toFloat())).getDistance(),
        )
        drawCircle(reveal.color, radius = far * reveal.progress.value, center = c)
    }
}
