package com.artemiy.player.ui.library

import com.artemiy.player.ui.icons.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.Song
import com.artemiy.player.ui.components.ART_SIZE_FULL
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.HeroOverArt
import com.artemiy.player.ui.components.rememberAlbumArtBitmap
import com.artemiy.player.ui.components.HeroTextShadow
import com.artemiy.player.ui.components.rememberArrowTint
import com.artemiy.player.ui.components.CircleIconButton
import com.artemiy.player.ui.components.SongActionsMenuPopup
import com.artemiy.player.ui.components.songLongPressTrigger
import com.artemiy.player.ui.theme.PlayerColors

@Composable
fun AlbumDetailScreen(
    album: String,
    artist: String,
    songs: List<Song>,
    onBack: () -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAddAllClick: (List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        val coverUri = songs.firstOrNull()?.uri
        val coverThumb = rememberAlbumArtBitmap(coverUri, ART_SIZE_THUMB)
        val arrowTint = rememberArrowTint(listOf(coverThumb))
        HeroOverArt(
            topTint = arrowTint,
            onBack = onBack,
            art = {
                // Single cover, not a collage — this is one album, unlike the artist page which
                // has to represent several.
                AlbumArt(uri = coverUri, size = ART_SIZE_FULL, modifier = Modifier.fillMaxSize())
            },
        ) {
            Text(
                text = album.ifBlank { "Без альбома" },
                color = Color.White,
                fontSize = 26.sp,
                style = TextStyle(shadow = HeroTextShadow),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$artist · ${songs.size} песен",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                style = TextStyle(shadow = HeroTextShadow),
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(icon = AppIcons.Shuffle, description = "Перемешать") { onShuffleAll(songs) }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(PlayerColors.Accent)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPlayAll(songs) }
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = AppIcons.Play, contentDescription = null, tint = PlayerColors.OnAccent, modifier = Modifier.size(16.dp))
                    Text(text = "Слушать", color = PlayerColors.OnAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                }
                CircleIconButton(icon = AppIcons.Add, description = "Добавить в плейлист") { onAddAllClick(songs) }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            songs.forEach { song ->
                var menuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .songLongPressTrigger(
                            onClick = { onSongClick(song, songs) },
                            onLongPress = { menuExpanded = true },
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(uri = song.uri, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(7.dp)))
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(text = song.title, color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = song.artist, color = PlayerColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SongActionsMenuPopup(
                        song = song,
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        onGoToArtist = onGoToArtist,
                    )
                }
            }
        }
    }
}
