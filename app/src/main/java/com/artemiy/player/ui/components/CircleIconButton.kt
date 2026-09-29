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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.delay

/** Where a hero button sits in the expressive style's connected group (Shuffle · Listen · Add):
 * round on its outer side, tight where it meets the next one. */
enum class GroupEdge { Start, Middle, End }

/** The gap between the hero buttons: a connected group in the expressive style. */
val heroButtonGap: androidx.compose.ui.unit.Dp @Composable get() = if (expressiveUi) 4.dp else 14.dp

/** [showCheck] briefly swaps the icon for a ✓ — see [rememberCheckFlash]. [edge] places it in the
 * expressive style's connected group; without one it stays a lone round button. */
@Composable
fun CircleIconButton(icon: ImageVector, description: String, showCheck: Boolean = false, edge: GroupEdge? = null, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val expressive = expressiveUi
    val grouped = expressive && edge != null
    Box(
        modifier = Modifier
            .then(if (grouped) Modifier.size(width = 64.dp, height = 60.dp) else Modifier.size(if (expressive) 52.dp else 46.dp))
            .pressScale(interaction, pressedScale = if (expressive) 0.92f else 0.86f)
            // Expressive: the shape changes under the finger.
            .clip(
                when {
                    grouped -> groupShape(interaction, edge!!)
                    expressive -> morphingShape(interaction, restPercent = 50, pressedPercent = 30)
                    else -> CircleShape
                },
            )
            .background(if (grouped) tonalAccent else PlayerColors.Surface)
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
                modifier = Modifier.size(if (grouped) 24.dp else if (expressive) 21.dp else 19.dp),
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
    val expressive = expressiveUi
    if (expressive) {
        // Expressive: the middle of the connected group — just a big play symbol, no words.
        Box(
            modifier = modifier
                .size(width = 96.dp, height = 60.dp)
                .pressScale(interaction, pressedScale = 0.93f)
                .clip(groupShape(interaction, GroupEdge.Middle))
                .background(PlayerColors.Accent)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = AppIcons.Play, contentDescription = stringResource(R.string.action_listen), tint = PlayerColors.OnAccent, modifier = Modifier.size(30.dp))
        }
        return
    }
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

/** A connected-group button's outline: round outside, tight inside; pressed, it rounds out fully. */
@Composable
private fun groupShape(interaction: InteractionSource, edge: GroupEdge): Shape = when (edge) {
    GroupEdge.Start -> morphingShape(interaction, startPercent = 50, endPercent = GROUP_INNER, pressedPercent = 50)
    GroupEdge.Middle -> morphingShape(interaction, startPercent = GROUP_INNER, endPercent = GROUP_INNER, pressedPercent = 50)
    GroupEdge.End -> morphingShape(interaction, startPercent = GROUP_INNER, endPercent = 50, pressedPercent = 50)
}

private const val GROUP_INNER = 22
