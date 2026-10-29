package com.artemiy.player.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.artemiy.player.ui.theme.PlayerColors

/** The classic player's bars in the soft, see-through look (Settings → Player) instead of the thin flat ones. */
val LocalGlassSliders = compositionLocalOf { false }

/** The player's seek / volume bar in whichever look is chosen. */
@Composable
fun PlayerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    /** The soft bar also lifts a little while held (the seek bar does, the volume bar only swells). */
    lifts: Boolean = true,
) {
    if (LocalGlassSliders.current) GlassSlider(value, onValueChange, valueRange, modifier, onValueChangeFinished, lifts)
    else MinimalSlider(value = value, onValueChange = onValueChange, valueRange = valueRange, onValueChangeFinished = onValueChangeFinished, modifier = modifier)
}

/**
 * A thick, slightly see-through bar without a knob: the track lets the background show through a
 * little, and the bar swells and brightens while a finger is on it so it is clear it's being changed.
 * Same touch behaviour as [MinimalSlider]: a tap jumps there, a drag from anywhere follows the finger.
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    lifts: Boolean = true,
) {
    val density = LocalDensity.current
    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)
    val onChange by rememberUpdatedState(onValueChange)
    val onFinished by rememberUpdatedState(onValueChangeFinished)
    val start by rememberUpdatedState(valueRange.start)
    val span by rememberUpdatedState(range)
    var touching by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val big = touching || dragging
    val thickness by animateDpAsState(if (big) 16.dp else 9.dp, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "glassSliderThickness")
    val lift by animateDpAsState(if (big && lifts) (-3).dp else 0.dp, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "glassSliderLift")
    val strength by animateFloatAsState(if (big) 1f else 0f, spring(stiffness = Spring.StiffnessMedium), label = "glassSliderStrength")
    val ink = PlayerColors.TextPrimary

    // The same footprint as the flat bar (12 dp), so everything around it stays where it was; the swollen bar spills over it.
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(12.dp)) {
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        fun valueAt(x: Float): Float = start + (x / widthPx).coerceIn(0f, 1f) * span

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .pointerInput(widthPx) {
                    detectTapGestures(
                        onPress = {
                            touching = true
                            tryAwaitRelease()
                            touching = false
                        },
                    ) { offset ->
                        onChange(valueAt(offset.x))
                        onFinished?.invoke()
                    }
                }
                .pointerInput(widthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> dragging = true; onChange(valueAt(offset.x)) },
                        onDragEnd = { dragging = false; onFinished?.invoke() },
                        onDragCancel = { dragging = false; onFinished?.invoke() },
                    ) { change, _ ->
                        change.consume()
                        onChange(valueAt(change.position.x))
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = lift)
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)
                    .height(thickness)
                    .clip(RoundedCornerShape(thickness / 2))
                    .background(ink.copy(alpha = 0.20f + 0.06f * strength)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(ink.copy(alpha = 0.62f + 0.30f * strength), RoundedCornerShape(thickness / 2)),
                )
            }
        }
    }
}
