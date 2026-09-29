package com.artemiy.player.ui.library

import androidx.compose.ui.res.pluralStringResource
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.artemiy.player.data.PlaylistWithCount
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.components.AppDialog
import com.artemiy.player.ui.components.DialogButtons
import com.artemiy.player.ui.components.DialogListRow
import com.artemiy.player.ui.components.DialogTextField
import com.artemiy.player.ui.components.DialogTitle
import androidx.compose.foundation.shape.CircleShape

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistWithCount>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    AppDialog(onDismiss = onDismiss) {
        DialogTitle(stringResource(R.string.add_to_playlist))
        if (playlists.isNotEmpty()) {
            LazyColumn(modifier = Modifier.padding(top = 12.dp).heightIn(max = 280.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    DialogListRow(
                        icon = AppIcons.Playlist,
                        title = playlist.name,
                        subtitle = pluralStringResource(R.plurals.tracks_count, playlist.songCount, playlist.songCount),
                        onClick = { onAddToPlaylist(playlist.id) },
                    )
                }
            }
        }
        // Or straight into a new one.
        DialogTextField(
            value = newName,
            onValueChange = { newName = it },
            placeholder = stringResource(R.string.new_playlist),
            autoFocus = playlists.isEmpty(),
            modifier = Modifier.padding(top = 14.dp),
            trailing = {
                val ready = newName.isNotBlank()
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (ready) PlayerColors.Accent else PlayerColors.SurfaceDim)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress, enabled = ready) {
                            onCreatePlaylist(newName)
                            newName = ""
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = stringResource(R.string.create_playlist),
                        tint = if (ready) PlayerColors.OnAccent else PlayerColors.TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        )
        DialogButtons(dismissLabel = stringResource(R.string.cancel), onDismiss = onDismiss)
    }
}
