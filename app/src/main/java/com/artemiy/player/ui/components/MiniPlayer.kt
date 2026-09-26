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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
) {
    Row(
        modifier = Modifier
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
                .clip(RoundedCornerShape(10.dp)),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = title,
                color = PlayerColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                color = PlayerColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val playInteraction = remember { MutableInteractionSource() }
        PlayPauseIcon(
            isPlaying = isPlaying,
            tint = PlayerColors.TextPrimary,
            modifier = Modifier
                .size(28.dp)
                .pressScale(playInteraction)
                .clickable(interactionSource = playInteraction, indication = null) { onTogglePlayPause() },
        )
    }
}
