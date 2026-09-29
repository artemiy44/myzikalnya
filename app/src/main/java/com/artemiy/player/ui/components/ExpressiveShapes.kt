package com.artemiy.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.artemiy.player.ui.theme.expressiveUi
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A round shape with [lobes] soft bumps around its edge, [depth] (0..1 of the radius) deep —
 * Material 3 Expressive's cookie / flower / clover family.
 */
class WavyShape(private val lobes: Int, private val depth: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy)
        val path = Path()
        val steps = 144
        for (i in 0..steps) {
            // A bump points straight up.
            val angle = 2.0 * PI * i / steps - PI / 2
            val wave = (1.0 - cos(lobes * (angle + PI / 2))) / 2.0
            val r = radius * (1f - depth * wave.toFloat())
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Which of the playful shapes an artist gets — always the same one for the same name. */
private val ARTIST_SHAPES = listOf(
    9 to 0.07f, // cookie
    6 to 0.13f, // flower
    4 to 0.18f, // clover
    12 to 0.05f, // sunny
)

/**
 * An artist's picture outline: a circle in the classic style; in the expressive one a playful
 * shape picked by [name], whose bumps smooth out towards a circle while [interaction] is pressed.
 */
@Composable
fun artistShape(name: String, interaction: InteractionSource? = null): Shape {
    if (!expressiveUi) return CircleShape
    val (lobes, depth) = remember(name) { ARTIST_SHAPES[(name.hashCode() and Int.MAX_VALUE) % ARTIST_SHAPES.size] }
    val pressed = interaction?.collectIsPressedAsState()?.value ?: false
    val amount by animateFloatAsState(if (pressed) 0.25f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "artistShape")
    return remember(lobes, depth, amount) { WavyShape(lobes, depth * amount) }
}
