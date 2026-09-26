package com.artemiy.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.delay

/** [showCheck] briefly swaps the icon for a ✓ — see [rememberCheckFlash]. */
@Composable
fun CircleIconButton(icon: ImageVector, description: String, showCheck: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(46.dp)
            .pressScale(interaction)
            .clip(CircleShape)
            .background(PlayerColors.Surface)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = showCheck,
            transitionSpec = {
                (fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.4f))
                    .togetherWith(fadeOut(tween(120)) + scaleOut(tween(150), targetScale = 0.4f))
            },
            label = "circleCheck",
        ) { check ->
            Icon(
                imageVector = if (check) AppIcons.Check else icon,
                contentDescription = description,
                tint = PlayerColors.TextPrimary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/** A "done!" moment for a button: [flash] turns [visible] on for a second and a bit. */
class CheckFlash internal constructor() {
    var visible by mutableStateOf(false)
        internal set
    internal var trigger by mutableIntStateOf(0)
    fun flash() { trigger++ }
}

@Composable
fun rememberCheckFlash(): CheckFlash {
    val flash = remember { CheckFlash() }
    LaunchedEffect(flash.trigger) {
        if (flash.trigger == 0) return@LaunchedEffect
        flash.visible = true
        delay(1300)
        flash.visible = false
    }
    return flash
}
