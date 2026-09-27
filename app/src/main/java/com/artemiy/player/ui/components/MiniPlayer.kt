package com.artemiy.player.ui.components

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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.ui.theme.PlayerColors

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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .onGloballyPositioned { anchors?.bar = it.boundsInRoot() }
            .fillMaxWidth()
            .background(PlayerColors.SurfaceDim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onOpen() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
                contentDescription = "Предыдущий трек",
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
            contentDescription = "Следующий трек",
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
}

private val MINI_TEXT_LINE_HEIGHT = 15.6.sp

/** The mini player cover's corner radius — the classic player's cover starts from it when it
 * flies up. */
val MINI_ART_CORNER = 10.dp
