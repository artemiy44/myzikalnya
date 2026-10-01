package com.artemiy.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.R
import com.artemiy.player.data.Song
import com.artemiy.player.ui.components.SoftPress
import com.artemiy.player.ui.components.groupedCard
import com.artemiy.player.ui.components.tonalAccent
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.PlayerColors

/** A folder of music: its songs, and the folders inside it with how many songs they hold in all. */
internal class FolderNode(val name: String, val path: List<String>) {
    val songs = mutableListOf<Song>()
    val children = LinkedHashMap<String, FolderNode>()

    /** Songs in this folder and everything below it. */
    var total = 0
        private set

    fun countUp(): Int {
        total = songs.size + children.values.sumOf { it.countUp() }
        return total
    }

    /** Every song in this folder and below, folder by folder. */
    fun allSongs(): List<Song> = songs.sortedBy(::folderOrder) + children.values.flatMap { it.allSongs() }
}

private fun folderOrder(song: Song): String = song.title.lowercase()

/** The folder tree of [songs] by their paths; its root has no name — it's the storage itself. */
internal fun buildFolderTree(songs: List<Song>): FolderNode {
    val root = FolderNode("", emptyList())
    for (song in songs) {
        var node = root
        for (segment in song.pathSegments) node = node.children.getOrPut(segment) { FolderNode(segment, node.path + segment) }
        node.songs += song
    }
    root.countUp()
    return root
}

/** Where the Folders section opens: down from the top for as long as there's only one way to go
 * (storage → emulated → 0 → "muzikaoffline"), so the first thing shown is the music itself. */
internal fun startFolder(root: FolderNode): FolderNode {
    var node = root
    while (node.songs.isEmpty() && node.children.size == 1) node = node.children.values.first()
    return node
}

internal fun FolderNode.find(path: List<String>): FolderNode? {
    var node = this
    for (segment in path) node = node.children[segment] ?: return null
    return node
}

/** How many folders in all hold songs (for the count on the Library page). */
internal fun FolderNode.folderCount(): Int = (if (songs.isNotEmpty()) 1 else 0) + children.values.sumOf { it.folderCount() }

/**
 * One folder's page: play or shuffle everything in it (and below), the folders inside it, then
 * the songs lying directly in it.
 */
@Composable
internal fun FolderPage(
    node: FolderNode,
    state: LazyListState,
    onOpenFolder: (FolderNode) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
) {
    val here = remember(node) { node.songs.sortedWith(compareBy({ it.trackNumber ?: Int.MAX_VALUE }, { it.title.lowercase() })) }
    val everything = remember(node) { node.allSongs() }
    SongList(
        songs = here,
        state = state,
        onSongClick = { song -> onSongClick(song, here) },
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onAddToPlaylist = onAddToPlaylist,
        onGoToAlbum = onGoToAlbum,
        onGoToArtist = onGoToArtist,
        header = {
            Column {
                if (everything.isNotEmpty()) {
                    PlayShuffleRow(
                        onPlay = { onPlayAll(everything) },
                        onShuffle = { onPlayAll(everything.shuffled()) },
                    )
                }
                val folders = node.children.values.toList()
                Column(modifier = Modifier.padding(horizontal = com.artemiy.player.ui.components.pageGutter, vertical = 6.dp)) {
                    folders.forEachIndexed { i, child ->
                        FolderRow(child, Modifier.groupedCard(i, folders.size)) { onOpenFolder(child) }
                    }
                }
                if (folders.isNotEmpty() && here.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.folder_here),
                        color = PlayerColors.TextPrimary,
                        style = com.artemiy.player.ui.theme.sectionTitleStyle,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 6.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun FolderRow(folder: FolderNode, modifier: Modifier, onClick: () -> Unit) {
    val expressive = com.artemiy.player.ui.theme.expressiveUi
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = SoftPress, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(if (expressive) 16.dp else 10.dp))
                .background(tonalAccent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Folder, null, tint = if (expressive) PlayerColors.AccentStandalone else PlayerColors.TextPrimary, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(folder.name, color = PlayerColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = pluralStringResource(R.plurals.songs_count, folder.total, folder.total),
                color = PlayerColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
        Icon(AppIcons.ChevronRight, null, tint = PlayerColors.TextTertiary, modifier = Modifier.size(20.dp).padding(end = 2.dp))
    }
}
