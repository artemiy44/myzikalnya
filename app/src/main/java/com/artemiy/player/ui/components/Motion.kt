package com.artemiy.player.ui.components

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.artemiy.player.ui.icons.AppIcons

/** Sinks slightly while held and springs back on release — a button that feels pressed. Pass
 * the same [interactionSource] the button's clickable uses. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.86f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Play/pause that flows into the other one — the old icon shrinks away while the new one grows
 * in — instead of swapping in a single frame. */
@Composable
fun PlayPauseIcon(isPlaying: Boolean, tint: Color, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (fadeIn(tween(160)) + scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.5f))
                .togetherWith(fadeOut(tween(110)) + scaleOut(tween(140), targetScale = 0.5f))
        },
        modifier = modifier,
        label = "playPause",
    ) { playing ->
        Icon(
            imageVector = if (playing) AppIcons.Pause else AppIcons.Play,
            contentDescription = stringResource(if (playing) R.string.cd_pause else R.string.cd_play),
            tint = tint,
            modifier = Modifier.fillMaxSize(),
        )
    }
}


/**
 * The radius of the phone's screen corners in pixels (Android 12+ reports it; 0 = square corners,
 * an older Android, or not known yet). The window may not know its insets in the very first
 * frames, so it asks a few times.
 */
@Composable
fun rememberScreenCornerRadiusPx(): Float {
    val view = androidx.compose.ui.platform.LocalView.current
    val radius by androidx.compose.runtime.produceState(0f, view) {
        repeat(6) {
            val r = screenCornerRadiusPx(view)
            if (r > 0f) { value = r; return@produceState }
            kotlinx.coroutines.delay(300)
        }
    }
    return radius
}

private fun screenCornerRadiusPx(view: android.view.View): Float {
    if (android.os.Build.VERSION.SDK_INT < 31) return 0f
    val insets = view.rootWindowInsets ?: return 0f
    return listOf(
        android.view.RoundedCorner.POSITION_TOP_LEFT,
        android.view.RoundedCorner.POSITION_TOP_RIGHT,
        android.view.RoundedCorner.POSITION_BOTTOM_LEFT,
        android.view.RoundedCorner.POSITION_BOTTOM_RIGHT,
    ).maxOf { insets.getRoundedCorner(it)?.radius ?: 0 }.toFloat()
}


/**
 * [androidx.compose.animation.core.CubicBezierEasing], without its one flaw: for some curves the
 * search for the answer fails very close to the end of the way (0.9999999) and it throws — which
 * took the whole app down (seen in the bouncing-dots loading mark). Same curve, same look; the very
 * ends answer 0 and 1 at once, and a failed search answers with the straight line instead.
 */
class SafeCubicBezier(a: Float, b: Float, c: Float, d: Float) : androidx.compose.animation.core.Easing {
    private val curve = androidx.compose.animation.core.CubicBezierEasing(a, b, c, d)

    override fun transform(fraction: Float): Float {
        if (fraction <= 0.0001f) return 0f
        if (fraction >= 0.9999f) return 1f
        return try {
            curve.transform(fraction)
        } catch (e: IllegalArgumentException) {
            fraction
        }
    }
}
