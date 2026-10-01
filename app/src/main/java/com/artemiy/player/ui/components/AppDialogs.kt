package com.artemiy.player.ui.components

import androidx.compose.animation.core.Animatable
import com.artemiy.player.ui.theme.inAppFont
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.PlayerColors

// The app's own dialogs and popup menus — one look for all of them: big round corners, the
// theme's colors and accent, pill buttons, the app's icons.

/** A dialog card that pops in with a little spring. */
@Composable
fun AppDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val pop = remember { Animatable(0.9f) }
        LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 600f)) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(RoundedCornerShape(DIALOG_CORNER))
                .background(PlayerColors.SurfaceDim)
                .padding(22.dp),
            content = content,
        )
    }
}

@Composable
fun DialogTitle(text: String) {
    Text(text = text, color = PlayerColors.TextPrimary, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
fun DialogMessage(text: String) {
    Text(text = text, color = PlayerColors.TextSecondary, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
}

/**
 * The dialog's two answers side by side as pills: backing out on the left, going ahead on the
 * right in the accent color (red when it deletes something).
 */
@Composable
fun DialogButtons(
    dismissLabel: String,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: () -> Unit = {},
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DialogButton(dismissLabel, fill = PlayerColors.Surface, textColor = PlayerColors.TextPrimary, onClick = onDismiss)
        if (confirmLabel != null) {
            DialogButton(
                confirmLabel,
                fill = if (destructive) DestructiveRed else PlayerColors.Accent,
                textColor = if (destructive) Color.White else PlayerColors.OnAccent,
                enabled = confirmEnabled,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
private fun RowScope.DialogButton(label: String, fill: Color, textColor: Color, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .pressScale(interaction, pressedScale = 0.95f)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** A text box for a dialog; focused (keyboard up) as soon as the dialog opens when [autoFocus]. */
@Composable
fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PlayerColors.Surface)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            HintTextField(
                value = value,
                onValueChange = onValueChange,
                hint = placeholder,
                fontSize = 16.sp,
                cursorColor = PlayerColors.AccentStandalone,
                modifier = Modifier.focusRequester(focus),
            )
        }
        trailing?.invoke()
    }
}

/** One choice in a dialog's list: an icon in a soft circle, a title and maybe a line under it. */
@Composable
fun DialogListRow(icon: ImageVector, title: String, subtitle: String? = null, trailing: ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(PlayerColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(text = title, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(text = subtitle, color = PlayerColors.TextSecondary, fontSize = 12.sp, maxLines = 1)
        }
        if (trailing != null) Icon(trailing, contentDescription = null, tint = PlayerColors.TextTertiary, modifier = Modifier.size(18.dp))
    }
}

/**
 * A popup menu in the app's look: rounded, in the same color as the app's dialogs — and always
 * in the app's own theme, even when opened inside the player (which has a dark palette of its own).
 */
@Composable
fun AppDropdownMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (!expanded) return
    val app = com.artemiy.player.ui.theme.LocalAppPalette.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val provider = remember(density) {
        MenuPositionProvider(
            side = with(density) { MENU_SHADOW_SIDE.roundToPx() },
            top = with(density) { MENU_SHADOW_TOP.roundToPx() },
            bottom = with(density) { MENU_SHADOW_BOTTOM.roundToPx() },
            margin = with(density) { 8.dp.roundToPx() },
        )
    }
    val maxHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp - 64.dp
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(140)) }
    // Its own window rather than Material's DropdownMenu: that one is exactly the menu's size and
    // cut its shadow off at the edges. This one is bigger by a transparent margin the shadow falls
    // into, and the position below counts only the visible menu.
    androidx.compose.ui.window.Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
    ) {
        com.artemiy.player.ui.theme.PaletteScope(app) {
            val shape = RoundedCornerShape(MENU_CORNER)
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = appear.value
                        val scale = 0.92f + 0.08f * appear.value
                        scaleX = scale
                        scaleY = scale
                    }
                    .padding(start = MENU_SHADOW_SIDE, end = MENU_SHADOW_SIDE, top = MENU_SHADOW_TOP, bottom = MENU_SHADOW_BOTTOM)
                    .shadow(10.dp, shape)
                    .clip(shape)
                    .background(app.surfaceDim),
            ) {
                Column(
                    modifier = Modifier
                        .width(androidx.compose.foundation.layout.IntrinsicSize.Max)
                        .widthIn(min = 112.dp, max = 280.dp)
                        .heightIn(max = maxHeight)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(vertical = 8.dp),
                    content = content,
                )
            }
        }
    }
}

/** Puts the visible menu under its button (above it when there's no room), inside the screen; [side], [top] and [bottom] are the shadow margin around it. */
private class MenuPositionProvider(val side: Int, val top: Int, val bottom: Int, val margin: Int) : androidx.compose.ui.window.PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ): androidx.compose.ui.unit.IntOffset {
        val w = popupContentSize.width - 2 * side
        val h = popupContentSize.height - top - bottom
        var x = anchorBounds.left
        if (x + w > windowSize.width - margin) x = anchorBounds.right - w
        x = x.coerceIn(margin, maxOf(margin, windowSize.width - margin - w))
        var y = anchorBounds.bottom
        if (y + h > windowSize.height - margin) {
            val above = anchorBounds.top - h
            y = if (above >= margin) above else maxOf(margin, windowSize.height - margin - h)
        }
        return androidx.compose.ui.unit.IntOffset(x - side, y - top)
    }
}

/** One line of an [AppDropdownMenu]; [selected] shows a check at the end (e.g. the current sort). */
@Composable
fun AppMenuItem(text: String, icon: ImageVector? = null, selected: Boolean = false, destructive: Boolean = false, onClick: () -> Unit) {
    val color = if (destructive) DestructiveRed else PlayerColors.TextPrimary
    DropdownMenuItem(
        text = { Text(text = text, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) } },
        trailingIcon = if (selected) {
            { Icon(AppIcons.Check, contentDescription = null, tint = PlayerColors.AccentStandalone, modifier = Modifier.size(18.dp)) }
        } else null,
        colors = MenuDefaults.itemColors(textColor = color, leadingIconColor = color),
        contentPadding = PaddingValues(horizontal = 18.dp),
        onClick = onClick,
    )
}

/** For the one action that deletes something. */
val DestructiveRed = Color(0xFFE5383B)

private val DIALOG_CORNER = 28.dp
private val MENU_CORNER = 18.dp
// Room for the shadow around the menu: it falls mostly downwards, so the most is needed there.
private val MENU_SHADOW_SIDE = 28.dp
private val MENU_SHADOW_TOP = 18.dp
private val MENU_SHADOW_BOTTOM = 48.dp
