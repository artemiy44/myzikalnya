package com.artemiy.player.ui.library

import androidx.compose.ui.res.pluralStringResource
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.inAppFont
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.artemiy.player.ui.components.BlurredCollageArt
import com.artemiy.player.ui.components.COLLAGE_HERO_HEIGHT
import com.artemiy.player.ui.components.rememberBlurredCollage
import com.artemiy.player.ui.components.PlayPillButton
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HeroOverArt(
            // The drawn picture is always deep and dark: a white arrow reads on it.
            topTint = androidx.compose.ui.graphics.Color.White,
            onBack = onBack,
            height = COLLAGE_HERO_HEIGHT,
            art = { ArtistGenreArt(artist, songs) },
        ) {
            Text(
                text = artist,
                color = Color.White,
                fontSize = 30.sp,
                style = TextStyle(shadow = HeroTextShadow).inAppFont(),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.songs_count, songs.size, songs.size) + " · " + pluralStringResource(R.plurals.albums_count, albums.size, albums.size),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                style = TextStyle(shadow = HeroTextShadow).inAppFont(),
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(icon = AppIcons.Shuffle, description = stringResource(R.string.shuffle)) { onShuffleAll(songs) }
                PlayPillButton(onClick = { onPlayAll(songs) }, modifier = Modifier.padding(horizontal = 14.dp))
                CircleIconButton(icon = AppIcons.AddToQueue, description = stringResource(R.string.add_to_play_queue), showCheck = queuedFlash.visible) {
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
                Text(text = stringResource(R.string.albums), color = PlayerColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                albums.chunked(2).forEach { rowAlbums ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowAlbums.forEach { (album, albumSongs) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onAlbumClick(album) },
                            ) {
                                AlbumArt(
                                    uri = albumSongs.firstOrNull()?.uri,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)),
                                )
                                Text(
                                    text = album.ifBlank { stringResource(R.string.no_album) },
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
            Text(text = stringResource(R.string.tracks), color = PlayerColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
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
                    AlbumArt(uri = song.uri, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(text = song.title, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    com.artemiy.player.ui.components.AppDialog(onDismiss = onDismiss) {
        com.artemiy.player.ui.components.DialogTitle(stringResource(R.string.add_to_play_queue_q))
        com.artemiy.player.ui.components.DialogMessage(
            stringResource(R.string.add_artist_to_queue_msg, songCount),
        )
        com.artemiy.player.ui.components.DialogButtons(dismissLabel = stringResource(R.string.no), onDismiss = onDismiss, confirmLabel = stringResource(R.string.add), onConfirm = onConfirm)
    }
}
