package com.artemiy.player.ui.library

import com.artemiy.player.ui.icons.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.window.Dialog
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.HeroOverArt
import com.artemiy.player.ui.components.rememberAlbumArtBitmap
import com.artemiy.player.ui.components.HeroTextShadow
import com.artemiy.player.ui.components.rememberArrowTint
import com.artemiy.player.ui.components.CircleIconButton
import com.artemiy.player.ui.components.rememberCheckFlash
import com.artemiy.player.ui.components.SongActionsMenuPopup
import com.artemiy.player.ui.components.songLongPressTrigger
import com.artemiy.player.ui.theme.PlayerColors

@Composable
fun ArtistDetailScreen(
    artist: String,
    songs: List<Song>,
    albums: List<Pair<String, List<Song>>>,
    onBack: () -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddAllToQueue: (List<Song>) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
) {
    val queuedFlash = rememberCheckFlash()
    var showAddToQueueDialog by remember { mutableStateOf(false) }
    val heroArts = remember(songs) {
        val distinct = songs.groupBy { it.album }.values.map { it.first() }
        if (distinct.isEmpty()) emptyList()
        else (0 until 4).map { distinct[it % distinct.size] }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Thumbnail of the top-left cover, only to judge how bright the art is behind the back arrow.
        val arrowTint = rememberArrowTint(listOf(heroArts.firstOrNull()?.let { rememberAlbumArtBitmap(it.uri, ART_SIZE_THUMB) }))
        HeroOverArt(
            topTint = arrowTint,
            onBack = onBack,
            art = {
                if (heroArts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().background(com.artemiy.player.ui.components.placeholderArtBrush()))
                } else {
                    // 2×2 collage stretched to fill the whole (taller than wide) header.
                    Column(modifier = Modifier.fillMaxSize()) {
                        heroArts.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                row.forEach { song ->
                                    AlbumArt(
                                        uri = song.uri,
                                        size = com.artemiy.player.ui.components.ART_SIZE_FULL,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) {
            Text(
                text = artist,
                color = Color.White,
                fontSize = 30.sp,
                style = TextStyle(shadow = HeroTextShadow),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${songs.size} песен · ${albums.size} альбомов",
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
                CircleIconButton(icon = AppIcons.AddToQueue, description = "Добавить в очередь проигрывания", showCheck = queuedFlash.visible) {
                    showAddToQueueDialog = true
                }
            }
        }

        if (showAddToQueueDialog) {
            AddArtistToQueueDialog(
                songCount = songs.size,
                onDismiss = { showAddToQueueDialog = false },
                onConfirm = {
                    showAddToQueueDialog = false
                    onAddAllToQueue(songs)
                    queuedFlash.flash()
                },
            )
        }

        if (albums.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(text = "Альбомы", color = PlayerColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                albums.chunked(2).forEach { rowAlbums ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowAlbums.forEach { (album, albumSongs) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAlbumClick(album) },
                            ) {
                                AlbumArt(
                                    uri = albumSongs.firstOrNull()?.uri,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)),
                                )
                                Text(
                                    text = album.ifBlank { "Без альбома" },
                                    color = PlayerColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                        if (rowAlbums.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(text = "Треки", color = PlayerColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
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
                        Text(text = song.album.ifBlank { artist }, color = PlayerColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SongActionsMenuPopup(
                        song = song,
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        onGoToAlbum = onGoToAlbum,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddArtistToQueueDialog(songCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(PlayerColors.SurfaceDim)
                .padding(20.dp),
        ) {
            Text(
                text = "Добавить в очередь проигрывания?",
                color = PlayerColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Все песни артиста ($songCount) встанут в конец очереди. Та, что играет сейчас, и уже стоящие в очереди не продублируются.",
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "Нет",
                    color = PlayerColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
                        .padding(end = 20.dp),
                )
                Text(
                    text = "Да",
                    color = PlayerColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onConfirm() },
                )
            }
        }
    }
}
