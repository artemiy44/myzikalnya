package com.artemiy.player.ui.components

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
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
            imageVector = AppIcons.MoreVertical,
            contentDescription = stringResource(R.string.track_actions),
            tint = PlayerColors.TextSecondary,
            modifier = Modifier
                .size(iconSize)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { expanded = true },
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

    AppDropdownMenu(expanded = expanded, onDismiss = onDismiss) {
        AppMenuItem(
            text = stringResource(R.string.play_next),
            icon = AppIcons.PlayNext,
            onClick = { onDismiss(); onPlayNext(song) },
        )
        AppMenuItem(
            text = stringResource(R.string.add_to_queue),
            icon = AppIcons.Queue,
            onClick = { onDismiss(); onAddToQueue(song) },
        )
        AppMenuItem(
            text = stringResource(R.string.add_to_playlist),
            icon = AppIcons.AddToPlaylist,
            onClick = { onDismiss(); onAddToPlaylist(song) },
        )
        if (onGoToAlbum != null) {
            AppMenuItem(
            text = stringResource(R.string.go_to_album),
            icon = AppIcons.Album,
            onClick = { onDismiss(); onGoToAlbum(song) },
            )
        }
        if (onGoToArtist != null) {
            AppMenuItem(
            text = stringResource(R.string.go_to_artist),
            icon = AppIcons.Artist,
            onClick = { onDismiss(); onGoToArtist(song) },
            )
        }
        if (onRemoveFromPlaylist != null) {
            AppMenuItem(
            text = stringResource(R.string.remove_from_playlist),
            icon = AppIcons.RemoveFromPlaylist,
            onClick = { onDismiss(); onRemoveFromPlaylist(song) },
            )
        }
        AppMenuItem(
            text = stringResource(R.string.share),
            icon = AppIcons.Share,
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
        AppMenuItem(
            text = stringResource(R.string.info),
            icon = AppIcons.Info,
            onClick = { onDismiss(); showInfo = true },
        )
    }

    if (showInfo) {
        SongInfoSheet(song = song, onDismiss = { showInfo = false })
    }
}

/** The tap-plays/long-press-opens-menu pattern used on cards and grid cells, paired with
 * [SongActionsMenuPopup] which the caller places as a sibling to actually show the menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.songLongPressTrigger(onClick: () -> Unit, onLongPress: () -> Unit): Modifier =
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = com.artemiy.player.ui.components.SoftPress,
        onClick = onClick,
        onLongClick = onLongPress,
    )
