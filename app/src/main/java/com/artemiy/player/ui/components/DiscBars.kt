package com.artemiy.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.R
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.Disc
import com.artemiy.player.ui.theme.PlayerColors

private const val DIAL_SPIN = 60f

/**
 * The "Диск" mini player: a wide rounded tile — a small spinning plate where the cover would be, the
 * title and artist, a tile with play in the accent colour and a plain one for the next song. It
 * belongs with the Dial player (shown only when the Now Playing style is "Диск" too), so it is
 * only ever used together with it.
 */
@Composable
fun DiscMiniPlayer(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    /** Filled with where this tile sits on screen, for the player to open out of it. */
    anchors: MiniPlayerAnchors? = null,
    /** Off while the big player is open — so the title starts scrolling afresh once it's closed. */
    textScrolls: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // A solid colour (the player opens out of it): the text colour's tint laid over the page.
    val panel = lerp(PlayerColors.Background, PlayerColors.TextPrimary, 0.10f)
    SideEffect { anchors?.color = panel }
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .onGloballyPositioned { anchors?.let { a -> a.bar = a.keep(a.bar, it.boundsInRoot()) } }
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(MINI_CARD_CORNER))
            .background(panel)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpen() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(Disc.smallCorner)).background(Disc.tileFill),
            contentAlignment = Alignment.Center,
        ) {
            SpinningPlate(isPlaying = isPlaying, size = 32.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = com.artemiy.player.ui.theme.lyricAnnotated(title),
                color = PlayerColors.TextPrimary,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier.marquee(textScrolls),
            )
            Text(
                text = com.artemiy.player.ui.theme.lyricAnnotated(artist),
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                modifier = Modifier.marquee(textScrolls),
            )
        }
        val playInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(46.dp)
                .pressScale(playInteraction, pressedScale = 0.92f)
                .clip(RoundedCornerShape(Disc.smallCorner))
                .background(PlayerColors.Accent)
                .clickable(interactionSource = playInteraction, indication = null) { onTogglePlayPause() },
            contentAlignment = Alignment.Center,
        ) {
            PlayPauseIcon(isPlaying = isPlaying, tint = PlayerColors.OnAccent, modifier = Modifier.size(22.dp))
        }
        val nextInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(46.dp)
                .pressScale(nextInteraction, pressedScale = 0.92f)
                .clip(RoundedCornerShape(Disc.smallCorner))
                .background(Disc.tileFill)
                .clickable(interactionSource = nextInteraction, indication = null) { onSkipNext() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.SkipNext,
                contentDescription = stringResource(R.string.cd_next_track),
                tint = PlayerColors.TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * The turning of the plate, one for the whole app: the Dial player's big plate and the mini
 * player's small one both read it, so they show the same angle. Whichever loop draws the frame
 * first moves it on; the other finds it already moved.
 */
object PlateRotation {
    var angle by mutableFloatStateOf(0f)
    /** A finger is winding the big plate: it sets the angle itself. */
    var dragging by mutableStateOf(false)
    private var spin = 0f
    private var lastNanos = 0L

    /** Moves the plate on to this frame; false once it has wound down to a stop. */
    fun tick(nowNanos: Long, playing: Boolean): Boolean {
        if (nowNanos == lastNanos) return playing || spin != 0f
        val dt = ((nowNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
        lastNanos = nowNanos
        if (dragging) return true
        val goal = if (playing) DIAL_SPIN else 0f
        spin += (goal - spin) * (1f - kotlin.math.exp(-dt * 3f))
        angle = (angle + spin * dt) % 360f
        if (!playing && kotlin.math.abs(spin) < 0.3f) {
            spin = 0f
            return false
        }
        return true
    }
}

/** A disc with a dot to show it turning; follows [PlateRotation]. */
@Composable
fun SpinningPlate(isPlaying: Boolean, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    LaunchedEffect(isPlaying) {
        while (true) {
            val now = withFrameNanos { it }
            if (!PlateRotation.tick(now, isPlaying)) break
        }
    }
    val disc = PlayerColors.TextPrimary
    val mark = PlayerColors.Background
    Canvas(modifier = modifier.size(size).graphicsLayer { rotationZ = PlateRotation.angle }) {
        val r = this.size.minDimension / 2f
        drawCircle(disc, radius = r)
        drawCircle(mark.copy(alpha = 0.12f), radius = r * 0.62f, style = Stroke(1.dp.toPx()))
        drawCircle(mark.copy(alpha = 0.12f), radius = r * 0.44f, style = Stroke(1.dp.toPx()))
        drawCircle(mark, radius = r * 0.12f, center = Offset(center.x, center.y - r * 0.8f))
        drawCircle(mark, radius = r * 0.07f)
    }
}

/** The "Диск" page heading: the title, and under it a second word in the second tone, same size. */
@Composable
fun DiscPageTitle(title: String, sub: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = title, color = PlayerColors.TextPrimary, style = com.artemiy.player.ui.theme.pageTitleStyle, maxLines = 1)
        Text(text = sub, color = PlayerColors.TextTertiary, style = com.artemiy.player.ui.theme.pageTitleStyle, maxLines = 1)
    }
}
