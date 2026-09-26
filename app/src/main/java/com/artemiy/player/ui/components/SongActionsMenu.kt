package com.artemiy.player.ui.components

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.artemiy.player.data.Song
import com.artemiy.player.ui.theme.PlayerColors
import java.util.concurrent.TimeUnit

/**
 * The one per-song menu used everywhere a song is listed (Home, Library, Artist/Album detail,
 * Search) — same actions, same order, same look, wherever it shows up. Share and the info dialog
 * are self-contained here since their behavior never varies by screen; the rest route through
 * callbacks because different screens plug them into different destinations (playback queue,
 * playlist dialog, library navigation).
 *
 * Two trigger styles, both sharing [SongActionsMenuItems]:
 * - [SongActionsMenu]: a visible "⋮" icon you tap — used in Search and wherever a plain row list
 *   is showing (dense text rows have room for an icon and benefit from a visible affordance).
 * - [SongActionsMenuPopup]: no icon at all — the caller long-presses its own card/row (via
 *   `Modifier.combinedClickable`) and controls `expanded` itself. Used in grids/cards, where a
 *   floating icon would clutter the artwork.
 */
@Composable
fun SongActionsMenu(
    song: Song,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: ((Song) -> Unit)? = null,
    onGoToArtist: ((Song) -> Unit)? = null,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Действия с треком",
            tint = PlayerColors.TextSecondary,
            modifier = Modifier
                .size(iconSize)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = true },
        )
        SongActionsMenuItems(
            song = song,
            expanded = expanded,
            onDismiss = { expanded = false },
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onAddToPlaylist = onAddToPlaylist,
            onGoToAlbum = onGoToAlbum,
            onGoToArtist = onGoToArtist,
            onRemoveFromPlaylist = onRemoveFromPlaylist,
        )
    }
}

/** Long-press variant with no visible icon — see the class doc above. */
@Composable
fun SongActionsMenuPopup(
    song: Song,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: ((Song) -> Unit)? = null,
    onGoToArtist: ((Song) -> Unit)? = null,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
) {
    SongActionsMenuItems(
        song = song,
        expanded = expanded,
        onDismiss = onDismiss,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onAddToPlaylist = onAddToPlaylist,
        onGoToAlbum = onGoToAlbum,
        onGoToArtist = onGoToArtist,
        onRemoveFromPlaylist = onRemoveFromPlaylist,
    )
}

@Composable
private fun SongActionsMenuItems(
    song: Song,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: ((Song) -> Unit)?,
    onGoToArtist: ((Song) -> Unit)?,
    onRemoveFromPlaylist: ((Song) -> Unit)?,
) {
    var showInfo by remember { mutableStateOf(false) }
    val context = LocalContext.current

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Играть следующим") },
            leadingIcon = { Icon(Icons.Filled.QueuePlayNext, contentDescription = null) },
            onClick = { onDismiss(); onPlayNext(song) },
        )
        DropdownMenuItem(
            text = { Text("Добавить в очередь") },
            leadingIcon = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
            onClick = { onDismiss(); onAddToQueue(song) },
        )
        DropdownMenuItem(
            text = { Text("Добавить в плейлист") },
            leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
            onClick = { onDismiss(); onAddToPlaylist(song) },
        )
        if (onGoToAlbum != null) {
            DropdownMenuItem(
                text = { Text("Перейти к альбому") },
                leadingIcon = { Icon(Icons.Filled.Album, contentDescription = null) },
                onClick = { onDismiss(); onGoToAlbum(song) },
            )
        }
        if (onGoToArtist != null) {
            DropdownMenuItem(
                text = { Text("Перейти к исполнителю") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                onClick = { onDismiss(); onGoToArtist(song) },
            )
        }
        if (onRemoveFromPlaylist != null) {
            DropdownMenuItem(
                text = { Text("Убрать из плейлиста") },
                leadingIcon = { Icon(Icons.Filled.RemoveCircleOutline, contentDescription = null) },
                onClick = { onDismiss(); onRemoveFromPlaylist(song) },
            )
        }
        DropdownMenuItem(
            text = { Text("Поделиться") },
            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
            onClick = {
                onDismiss()
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, song.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, song.title))
            },
        )
        DropdownMenuItem(
            text = { Text("Информация") },
            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
            onClick = { onDismiss(); showInfo = true },
        )
    }

    if (showInfo) {
        SongInfoDialog(song = song, onDismiss = { showInfo = false })
    }
}

/** The tap-plays/long-press-opens-menu pattern used on cards and grid cells, paired with
 * [SongActionsMenuPopup] which the caller places as a sibling to actually show the menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.songLongPressTrigger(onClick: () -> Unit, onLongPress: () -> Unit): Modifier =
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
        onLongClick = onLongPress,
    )

@Composable
private fun SongInfoDialog(song: Song, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(PlayerColors.Surface)
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Информация о треке",
                    color = PlayerColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Закрыть",
                    tint = PlayerColors.TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
                )
            }
            Column(modifier = Modifier.padding(top = 14.dp)) {
                InfoRow("Название", song.title)
                InfoRow("Исполнитель", song.artist)
                InfoRow("Альбом", song.album.ifBlank { "—" })
                InfoRow("Жанр", song.genre ?: "—")
                InfoRow("Год выпуска", song.year?.toString() ?: "—")
                InfoRow("Длительность", formatDuration(song.durationMs))
                InfoRow("Папка", song.folder ?: "—")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label,
            color = PlayerColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = PlayerColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.4f),
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
