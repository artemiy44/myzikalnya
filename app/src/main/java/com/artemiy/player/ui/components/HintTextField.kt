package com.artemiy.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.inAppFont

/**
 * A one-line text field with a grey hint while it's empty. The hint and the typed text share one
 * style — same font, size and line height, centred in the line — so the cursor sits exactly
 * where the hint's letters are; a hint too long for the field ends in "…" instead of wrapping
 * onto a second line and making the field taller.
 */
@Composable
fun HintTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    cursorColor: Color = PlayerColors.TextPrimary,
) {
    val style = TextStyle(
        fontSize = fontSize,
        lineHeight = fontSize * 1.3f,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    ).inAppFont()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style.copy(color = PlayerColors.TextPrimary),
        cursorBrush = SolidColor(cursorColor),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { field ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(text = hint, style = style, color = PlayerColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                field()
            }
        },
    )
}
