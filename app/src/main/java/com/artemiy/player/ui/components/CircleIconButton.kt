package com.artemiy.player.ui.components

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import com.artemiy.player.ui.theme.expressiveUi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.delay

/** [showCheck] briefly swaps the icon for a ✓ — see [rememberCheckFlash]. */
@Composable
fun CircleIconButton(icon: ImageVector, description: String, showCheck: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val expressive = expressiveUi
    Box(
        modifier = Modifier
            .size(if (expressive) 52.dp else 46.dp)
            .pressScale(interaction, pressedScale = if (expressive) 0.92f else 0.86f)
            // Expressive: the circle squares up a little under the finger.
            .clip(if (expressive) morphingShape(interaction, restPercent = 50, pressedPercent = 30) else CircleShape)
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
                modifier = Modifier.size(if (expressive) 21.dp else 19.dp),
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

/** The page's main button — "Слушать" on album/artist/mix pages, deliberately bigger than the
 * round buttons beside it. */
@Composable
fun PlayPillButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.93f)
            .clip(RoundedCornerShape(30.dp))
            .background(PlayerColors.Accent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = AppIcons.Play, contentDescription = null, tint = PlayerColors.OnAccent, modifier = Modifier.size(20.dp))
        Text(text = stringResource(R.string.action_listen), color = PlayerColors.OnAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

/** A rounded shape whose corners spring from [restPercent] to [pressedPercent] while pressed — the
 * expressive style's buttons change shape under the finger. */
@Composable
fun morphingShape(interaction: InteractionSource, restPercent: Int, pressedPercent: Int): Shape =
    morphingShape(interaction, restPercent, restPercent, pressedPercent)

/** The same, with different [startPercent]/[endPercent] corners at rest (a button inside a
 * connected group: round on its outer side, tight where it meets its neighbour). While pressed
 * both sides go to [pressedPercent]. */
@Composable
fun morphingShape(interaction: InteractionSource, startPercent: Int, endPercent: Int, pressedPercent: Int): Shape {
    val pressed by interaction.collectIsPressedAsState()
    val spec = spring<Float>(dampingRatio = 0.6f, stiffness = 600f)
    val start by animateFloatAsState(if (pressed) pressedPercent.toFloat() else startPercent.toFloat(), spec, label = "shapeStart")
    val end by animateFloatAsState(if (pressed) pressedPercent.toFloat() else endPercent.toFloat(), spec, label = "shapeEnd")
    return RoundedCornerShape(
        topStartPercent = start.toInt(),
        bottomStartPercent = start.toInt(),
        topEndPercent = end.toInt(),
        bottomEndPercent = end.toInt(),
    )
}

/**
 * A page header's buttons — shuffle, "Listen", and one more ([trailingIcon]: add, queue, a menu…;
 * [trailingOverlay] is drawn anchored to it, for a dropdown). Classic: two round buttons around the
 * pill. Expressive: one wide connected group of symbols like the expressive player's controls —
 * the pressed button swells and pushes its neighbours aside, its corners tightening, all on a
 * bouncy spring.
 */
@Composable
fun HeroButtons(
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
    trailingIcon: ImageVector,
    trailingDescription: String,
    onTrailing: () -> Unit,
    trailingCheck: Boolean = false,
    trailingOverlay: @Composable () -> Unit = {},
) {
    if (!expressiveUi) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(icon = AppIcons.Shuffle, description = stringResource(R.string.shuffle), onClick = onShuffle)
            PlayPillButton(onClick = onPlay, modifier = Modifier.padding(horizontal = 14.dp))
            Box {
                CircleIconButton(icon = trailingIcon, description = trailingDescription, showCheck = trailingCheck, onClick = onTrailing)
                trailingOverlay()
            }
        }
        return
    }
    val shuffleInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val trailingInteraction = remember { MutableInteractionSource() }
    val shufflePressed by shuffleInteraction.collectIsPressedAsState()
    val playPressed by playInteraction.collectIsPressedAsState()
    val trailingPressed by trailingInteraction.collectIsPressedAsState()
    val bounce = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val shuffleWeight by animateFloatAsState(if (shufflePressed) 1.4f else 1f, bounce, label = "heroShuffle")
    val playWeight by animateFloatAsState(if (playPressed) 1.75f else 1.45f, bounce, label = "heroPlay")
    val trailingWeight by animateFloatAsState(if (trailingPressed) 1.4f else 1f, bounce, label = "heroTrailing")
    val sideFill = tonalAccent
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        HeroGroupButton(shuffleWeight, pressed = shufflePressed, outerStart = true, fill = sideFill, interaction = shuffleInteraction, onClick = onShuffle) {
            Icon(AppIcons.Shuffle, contentDescription = stringResource(R.string.shuffle), tint = PlayerColors.TextPrimary, modifier = Modifier.size(28.dp))
        }
        HeroGroupButton(playWeight, pressed = playPressed, outerStart = null, fill = PlayerColors.Accent, interaction = playInteraction, onClick = onPlay) {
            Icon(AppIcons.Play, contentDescription = stringResource(R.string.action_listen), tint = PlayerColors.OnAccent, modifier = Modifier.size(34.dp))
        }
        HeroGroupButton(trailingWeight, pressed = trailingPressed, outerStart = false, fill = sideFill, interaction = trailingInteraction, onClick = onTrailing) {
            AnimatedContent(
                targetState = trailingCheck,
                transitionSpec = {
                    (fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.4f))
                        .togetherWith(fadeOut(tween(120)) + scaleOut(tween(150), targetScale = 0.4f))
                },
                label = "heroCheck",
            ) { check ->
                Icon(if (check) AppIcons.Check else trailingIcon, contentDescription = trailingDescription, tint = PlayerColors.TextPrimary, modifier = Modifier.size(28.dp))
            }
            trailingOverlay()
        }
    }
}

/** One button of [HeroButtons]' group. [outerStart]: which side faces outwards (rounder) — null
 * for the middle one, tight on both sides. */
@Composable
private fun RowScope.HeroGroupButton(
    weight: Float,
    pressed: Boolean,
    outerStart: Boolean?,
    fill: Color,
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val inner by animateDpAsState(if (pressed) 14.dp else 20.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "heroInner")
    val outer by animateDpAsState(
        when {
            outerStart == null -> if (pressed) 14.dp else 20.dp
            pressed -> 16.dp
            else -> 36.dp
        },
        spring(stiffness = Spring.StiffnessMediumLow),
        label = "heroOuter",
    )
    val start = if (outerStart == true) outer else inner
    val end = if (outerStart == false) outer else inner
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end))
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
