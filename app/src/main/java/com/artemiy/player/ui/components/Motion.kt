package com.artemiy.player.ui.components

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
            contentDescription = if (playing) "Пауза" else "Играть",
            tint = tint,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
