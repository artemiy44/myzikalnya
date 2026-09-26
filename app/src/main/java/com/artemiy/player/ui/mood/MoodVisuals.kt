package com.artemiy.player.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.artemiy.player.data.Mood
import com.artemiy.player.ui.theme.PlayerColors
import kotlin.math.cos
import kotlin.math.sin

val MOOD_COLORS = mapOf(
    Mood.NORMAL to Color(0xFF8E8E93),
    Mood.HAPPY to Color(0xFFFFB020),
    Mood.LOUD to Color(0xFFFF4D6D),
    Mood.SAD to Color(0xFF4B6CB7),
    Mood.CRY to Color(0xFF5B3FAE),
)

// Display order matches the approved mockup, not the enum's declaration order.
val MOOD_DISPLAY_ORDER = listOf(Mood.HAPPY, Mood.LOUD, Mood.NORMAL, Mood.SAD, Mood.CRY)

/** Small hand-drawn glyphs per mood — kept simple on purpose, matches the rest of this pass
 * (mechanics first, visual polish is a separate backlog item). */
@Composable
fun MoodIcon(mood: Mood, modifier: Modifier = Modifier, glyph: Color = PlayerColors.TextPrimary) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.09f
        when (mood) {
            Mood.NORMAL -> {
                val heights = listOf(0.35f, 0.7f, 1f, 0.55f, 0.85f)
                val barWidth = size.width / (heights.size * 1.8f)
                val gap = barWidth * 0.8f
                var x = barWidth / 2
                heights.forEach { hFrac ->
                    drawLine(
                        color = glyph,
                        start = Offset(x, size.height),
                        end = Offset(x, size.height * (1f - hFrac)),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    x += barWidth + gap
                }
            }

            Mood.HAPPY -> {
                val center = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension * 0.22f
                drawCircle(color = glyph, radius = r, center = center, style = Stroke(width = strokeWidth))
                val rayLen = size.minDimension * 0.2f
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble()).toFloat()
                    val innerR = r + strokeWidth * 1.6f
                    val outerR = innerR + rayLen
                    drawLine(
                        color = glyph,
                        start = Offset(center.x + innerR * cos(angle), center.y + innerR * sin(angle)),
                        end = Offset(center.x + outerR * cos(angle), center.y + outerR * sin(angle)),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            Mood.LOUD -> {
                val w = size.width
                val h = size.height
                val bolt = Path().apply {
                    moveTo(w * 0.60f, 0f)
                    lineTo(w * 0.16f, h * 0.58f)
                    lineTo(w * 0.46f, h * 0.58f)
                    lineTo(w * 0.38f, h)
                    lineTo(w * 0.88f, h * 0.38f)
                    lineTo(w * 0.56f, h * 0.38f)
                    close()
                }
                drawPath(bolt, color = glyph)
            }

            Mood.SAD -> {
                val r = size.minDimension * 0.34f
                val center = Offset(size.width * 0.52f, size.height * 0.5f)
                val moon = Path().apply {
                    addOval(Rect(Offset(center.x - r, center.y - r), Size(r * 2, r * 2)))
                    val cutCenter = Offset(center.x + r * 0.62f, center.y - r * 0.3f)
                    val cutR = r * 0.92f
                    addOval(Rect(Offset(cutCenter.x - cutR, cutCenter.y - cutR), Size(cutR * 2, cutR * 2)))
                    fillType = PathFillType.EvenOdd
                }
                drawPath(moon, color = glyph)
            }

            Mood.CRY -> {
                val w = size.width
                val h = size.height
                val drop = Path().apply {
                    moveTo(w * 0.5f, h * 0.04f)
                    cubicTo(w * 0.5f, h * 0.04f, w * 0.14f, h * 0.56f, w * 0.14f, h * 0.74f)
                    cubicTo(w * 0.14f, h * 0.95f, w * 0.31f, h, w * 0.5f, h)
                    cubicTo(w * 0.69f, h, w * 0.86f, h * 0.95f, w * 0.86f, h * 0.74f)
                    cubicTo(w * 0.86f, h * 0.56f, w * 0.5f, h * 0.04f, w * 0.5f, h * 0.04f)
                    close()
                }
                drawPath(drop, color = glyph)
            }
        }
    }
}
