package com.artemiy.player.ui.components

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.expressiveUi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    albumArtUri: Uri?,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    /** Given (expressive player style): a "previous" button before play too. */
    onSkipPrevious: (() -> Unit)? = null,
    /** Hidden while the classic player is open — its big cover is this one, flown up there. */
    artVisible: Boolean = true,
    /** Filled with where each piece sits on screen, for the classic player to open out of. */
    anchors: MiniPlayerAnchors? = null,
    /** Off while the big player is open — so the text starts scrolling afresh once it's closed. */
    textScrolls: Boolean = true,
    /** How far into the song, 0..1 — the expressive style's card shows it as a thin line. Read
     * while drawing only, so the ticking position never recomposes the bar. */
    progress: () -> Float = { 0f },
    /** Off while the big player covers the bar: the line then fades back in once it's gone,
     * rather than popping up the moment the closing animation ends. */
    progressShown: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // Expressive: a floating card in the playing cover's tone instead of a flat bar.
    val expressive = expressiveUi
    val tone = if (expressive) rememberCoverTone(albumArtUri) else null
    val cardColor by animateColorAsState(
        if (expressive) tonedSurface(tone, amount = 0.28f) else PlayerColors.SurfaceDim,
        tween(600),
        label = "miniCard",
    )
    androidx.compose.runtime.SideEffect { anchors?.color = cardColor }
    val lineColor = PlayerColors.TextPrimary
    val lineAlpha by androidx.compose.animation.core.animateFloatAsState(
        if (progressShown) 1f else 0f,
        if (progressShown) tween(450, delayMillis = 80) else tween(0),
        label = "miniLine",
    )
    Row(
        modifier = modifier
            .then(if (expressive) Modifier.padding(horizontal = 10.dp).padding(top = 8.dp) else Modifier)
            .onGloballyPositioned { anchors?.bar = it.boundsInRoot() }
            .fillMaxWidth()
            .then(if (expressive) Modifier.clip(RoundedCornerShape(MINI_CARD_CORNER)) else Modifier)
            .background(cardColor)
            .then(if (expressive) Modifier.drawBehind { drawProgressLine(progress(), lineColor.copy(alpha = lineColor.alpha * lineAlpha)) } else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onOpen() }
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = if (expressive) 14.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            uri = albumArtUri,
            modifier = Modifier
                .size(52.dp)
                .onGloballyPositioned { anchors?.art = it.boundsInRoot() }
                .graphicsLayer { alpha = if (artVisible) 1f else 0f }
                .clip(RoundedCornerShape(MINI_ART_CORNER)),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
          Column(modifier = Modifier.onGloballyPositioned { anchors?.text = it.boundsInRoot() }) {
            Text(
                text = title,
                color = PlayerColors.TextPrimary,
                fontSize = 13.sp,
                // Lines spaced like the big player's title at mini size (24sp × 13/20), so the
                // text doesn't jump when the player starts growing out of this bar.
                lineHeight = MINI_TEXT_LINE_HEIGHT,
                // As heavy as the big player's title (like Apple Music's mini player) — only the
                // size differs, so the hand-over while opening is hardly visible.
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier.marquee(textScrolls),
            )
            Text(
                text = artist,
                color = PlayerColors.TextSecondary,
                fontSize = 11.sp,
                lineHeight = MINI_TEXT_LINE_HEIGHT,
                maxLines = 1,
                modifier = Modifier.marquee(textScrolls),
            )
          }
        }

        if (onSkipPrevious != null) {
            val prevInteraction = remember { MutableInteractionSource() }
            Icon(
                imageVector = AppIcons.SkipPrevious,
                contentDescription = stringResource(R.string.cd_previous_track),
                tint = PlayerColors.TextPrimary,
                modifier = Modifier
                    .padding(end = 18.dp)
                    .size(24.dp)
                    .pressScale(prevInteraction)
                    .clickable(interactionSource = prevInteraction, indication = null) { onSkipPrevious() },
            )
        }
        val playInteraction = remember { MutableInteractionSource() }
        PlayPauseIcon(
            isPlaying = isPlaying,
            tint = PlayerColors.TextPrimary,
            modifier = Modifier
                .size(28.dp)
                .onGloballyPositioned { anchors?.play = it.boundsInRoot() }
                .pressScale(playInteraction)
                .clickable(interactionSource = playInteraction, indication = null) { onTogglePlayPause() },
        )
        val nextInteraction = remember { MutableInteractionSource() }
        Icon(
            imageVector = AppIcons.SkipNext,
            contentDescription = stringResource(R.string.cd_next_track),
            tint = PlayerColors.TextPrimary,
            modifier = Modifier
                .padding(start = 18.dp)
                .size(24.dp)
                .onGloballyPositioned { anchors?.next = it.boundsInRoot() }
                .pressScale(nextInteraction)
                .clickable(interactionSource = nextInteraction, indication = null) { onSkipNext() },
        )
    }
}

/** Where the mini player's pieces are on screen (root coordinates). */
class MiniPlayerAnchors {
    var bar by mutableStateOf<Rect?>(null)
    var art by mutableStateOf<Rect?>(null)
    var text by mutableStateOf<Rect?>(null)
    var play by mutableStateOf<Rect?>(null)
    var next by mutableStateOf<Rect?>(null)

    /** The bar's own colour — the players open out of it in this colour. */
    var color by mutableStateOf<androidx.compose.ui.graphics.Color?>(null)
}

private val MINI_TEXT_LINE_HEIGHT = 15.6.sp

/** The mini player cover's corner radius — the classic player's cover starts from it when it
 * flies up. */
val MINI_ART_CORNER = 10.dp

/** The expressive style's floating mini player card corners — the players' sheet starts from them. */
val MINI_CARD_CORNER = 22.dp

/** The expressive mini player's song progress: a thin rounded line along the card's bottom — the
 * faint rest of the song, and the part already played. */
private fun DrawScope.drawProgressLine(progress: Float, color: Color) {
    val inset = 20.dp.toPx()
    val thickness = 3.dp.toPx()
    val y = size.height - 7.dp.toPx()
    val start = Offset(inset, y)
    val end = Offset(size.width - inset, y)
    drawLine(color.copy(alpha = 0.16f), start, end, thickness, StrokeCap.Round)
    val p = progress.coerceIn(0f, 1f)
    if (p > 0f) drawLine(color.copy(alpha = 0.85f), start, Offset(start.x + (end.x - start.x) * p, y), thickness, StrokeCap.Round)
}
