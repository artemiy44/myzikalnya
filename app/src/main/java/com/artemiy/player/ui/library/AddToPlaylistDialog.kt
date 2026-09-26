package com.artemiy.player.ui.library

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

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistWithCount>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
) {
    var newName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(PlayerColors.SurfaceDim)
                .padding(20.dp),
        ) {
            Text(
                text = "Добавить в плейлист",
                color = PlayerColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            if (playlists.isNotEmpty()) {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(playlists, key = { it.id }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onAddToPlaylist(playlist.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = AppIcons.Playlist,
                                contentDescription = null,
                                tint = PlayerColors.TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(text = playlist.name, color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "${playlist.songCount} треков", color = PlayerColors.TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PlayerColors.Surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (newName.isEmpty()) {
                        Text(text = "Новый плейлист", color = PlayerColors.TextTertiary, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        textStyle = TextStyle(color = PlayerColors.TextPrimary, fontSize = 14.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(PlayerColors.TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = "Создать плейлист",
                    tint = PlayerColors.TextPrimary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (newName.isNotBlank()) {
                                onCreatePlaylist(newName)
                                newName = ""
                            }
                        },
                )
            }

            Text(
                text = "Отмена",
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 16.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
            )
        }
    }
}
