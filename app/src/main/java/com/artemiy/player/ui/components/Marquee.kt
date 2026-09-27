package com.artemiy.player.ui.components

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp

/** How wide the text really is, measured under the marquee (which lets it be as wide as it likes). */
private class MarqueeWidth { var content = 0 }

/**
 * For one-line titles that may not fit: instead of cutting them off with "…", a long one waits a
 * moment, then glides left to show the rest, pauses, and goes round again (Apple Music style), its
 * right edge fading out softly instead of chopping letters. Text that fits just sits still, with
 * no fade. [enabled] off = no movement (e.g. while it's hidden, so it doesn't run unseen; turning
 * it back on starts over from the beginning).
 */
@Composable
fun Modifier.marquee(enabled: Boolean = true): Modifier {
    val width = remember { MarqueeWidth() }
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            // Only when the text is actually longer than the space it has.
            if (width.content > size.width + 1f) {
                val fade = 18.dp.toPx().coerceAtMost(size.width / 3)
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Black,
                        (1f - fade / size.width).coerceIn(0f, 1f) to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
        .then(
            if (enabled) {
                Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 2500,
                    repeatDelayMillis = 2500,
                    spacing = MarqueeSpacing(40.dp),
                    velocity = 30.dp,
                )
            } else Modifier,
        )
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            // Without the marquee (disabled) the text is squeezed to the box, so ask it how wide
            // it would like to be instead.
            width.content = maxOf(placeable.width, measurable.maxIntrinsicWidth(constraints.maxHeight))
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
}
