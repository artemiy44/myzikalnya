package com.artemiy.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import com.artemiy.player.ui.theme.LocalPlayerPalette
import kotlinx.coroutines.launch

/**
 * The app's touch feedback for rows and small buttons in the tabs: a soft wash of the text color
 * that comes up quickly under the finger and fades away after — a rounded patch over a row, a
 * circle around a small icon. Quieter than Material's ripple, in the theme's own tone.
 */
object SoftPress : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = SoftPressNode(interactionSource)
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = javaClass.hashCode()
}

private class SoftPressNode(private val source: InteractionSource) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val shown = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> launch { shown.animateTo(1f, tween(90)); invalidateDraw() }
                    is PressInteraction.Release, is PressInteraction.Cancel -> launch {
                        // Held at least briefly even on a quick tap, so the flash is seen.
                        shown.animateTo(1f, tween(60))
                        shown.animateTo(0f, tween(320))
                    }
                }
            }
        }
        coroutineScope.launch {
            androidx.compose.runtime.snapshotFlow { shown.value }.collect { invalidateDraw() }
        }
    }

    override fun ContentDrawScope.draw() {
        val strength = shown.value
        if (strength > 0f) {
            val color = currentValueOf(LocalPlayerPalette).textPrimary.copy(alpha = 0.08f * strength)
            if (size.minDimension < 44.dp.toPx()) {
                drawCircle(color, radius = size.maxDimension / 2f + 8.dp.toPx())
            } else {
                drawRoundRect(color, cornerRadius = CornerRadius(12.dp.toPx()))
            }
        }
        drawContent()
    }
}
