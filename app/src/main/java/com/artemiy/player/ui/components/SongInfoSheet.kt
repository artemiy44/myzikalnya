package com.artemiy.player.ui.components

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.AppDatabase
import com.artemiy.player.data.PlaylistWithCount
import com.artemiy.player.data.Song
import com.artemiy.player.data.artists
import com.artemiy.player.ui.theme.LocalAppPalette
import com.artemiy.player.ui.theme.PaletteScope
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Everything known about a track, in a sheet that slides up from the bottom (swipe it down or go
 * back to close): the cover with title, artists and album at the top, then the details — tags,
 * the file's format and where it lives — and which playlists it's in. All the text can be
 * selected and copied. Always in the app's own theme, even when opened from the player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoSheet(song: Song, onDismiss: () -> Unit) {
    PaletteScope(LocalAppPalette.current) {
        val context = LocalContext.current
        val details by produceState<FileDetails?>(null, song.id) { value = loadFileDetails(context, song) }
        val playlists by produceState<List<PlaylistWithCount>?>(null, song.id) {
            value = withContext(Dispatchers.IO) { AppDatabase.get(context).playlistDao().playlistsContaining(song.id) }
        }
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            // Never under the status bar, however tall the sheet gets.
            modifier = Modifier.statusBarsPadding(),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PlayerColors.Background,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(PlayerColors.TextTertiary),
                )
            },
        ) {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp)
                        .padding(top = 8.dp, bottom = 24.dp)
                        .navigationBarsPadding(),
                ) {
                    Header(song)
                    SectionTitle(stringResource(R.string.info_details))
                    InfoRow(stringResource(if (song.artists().size > 1) R.string.info_artists else R.string.info_artist), song.artists().joinToString(", "))
                    InfoRow(stringResource(R.string.album), song.album.ifBlank { "—" })
                    song.discNumber?.let { InfoRow(stringResource(R.string.info_disc), it.toString()) }
                    InfoRow(stringResource(R.string.info_track_number), song.trackNumber?.toString() ?: "—")
                    InfoRow(stringResource(R.string.info_genre), song.genre ?: "—")
                    InfoRow(stringResource(R.string.info_year), song.year?.toString() ?: "—")
                    InfoRow(stringResource(R.string.info_length), formatLength(song.durationMs))

                    SectionTitle(stringResource(R.string.info_file))
                    InfoRow(stringResource(R.string.info_bitrate), details?.bitrateKbps?.let { stringResource(R.string.unit_kbps, it) } ?: dash(details))
                    details?.sampleRateHz?.let { InfoRow(stringResource(R.string.info_sample_rate), stringResource(R.string.unit_khz, it / 1000f)) }
                    InfoRow(stringResource(R.string.info_mime), details?.mime ?: dash(details))
                    InfoRow(stringResource(R.string.info_size), formatSize(details?.sizeBytes ?: song.sizeBytes))
                    InfoRow(stringResource(R.string.info_path), details?.path ?: dash(details), wide = true)

                    SectionTitle(stringResource(R.string.info_in_playlists))
                    PlaylistChips(playlists)
                }
            }
        }
    }
}

@Composable
private fun Header(song: Song) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AlbumArt(
            uri = song.uri,
            size = ART_SIZE_THUMB,
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(14.dp)),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                text = song.artists().joinToString(", "),
                color = PlayerColors.AccentStandalone,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Text(
                text = song.title,
                color = PlayerColors.TextPrimary,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 3,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (song.album.isNotBlank()) {
                Text(
                    text = song.album,
                    color = PlayerColors.TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = PlayerColors.AccentStandalone,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 22.dp, bottom = 4.dp),
    )
}

/** A label and its value side by side; [wide] puts the value on its own line under the label
 * (a file path is far too long to share a row). */
@Composable
private fun InfoRow(label: String, value: String, wide: Boolean = false) {
    if (wide) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(text = label, color = PlayerColors.TextSecondary, fontSize = 14.sp)
            Text(
                text = value,
                color = PlayerColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        return
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = label, color = PlayerColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = PlayerColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaylistChips(playlists: List<PlaylistWithCount>?) {
    when {
        playlists == null -> Unit
        playlists.isEmpty() -> Text(
            text = stringResource(R.string.info_no_playlists),
            color = PlayerColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        else -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            playlists.forEach { playlist ->
                Text(
                    text = playlist.name,
                    color = PlayerColors.OnAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PlayerColors.Accent)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/** What the file itself says, beyond the library's tags. Only read, never changed. */
private class FileDetails(
    val mime: String?,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val path: String?,
    val sizeBytes: Long?,
)

private suspend fun loadFileDetails(context: Context, song: Song): FileDetails = withContext(Dispatchers.IO) {
    var mime: String? = null
    var bitrate: Int? = null
    var path: String? = null
    var size: Long? = null
    val columns = buildList {
        add(MediaStore.Audio.Media.MIME_TYPE)
        @Suppress("DEPRECATION") add(MediaStore.Audio.Media.DATA)
        add(MediaStore.Audio.Media.SIZE)
        if (Build.VERSION.SDK_INT >= 30) add(MediaStore.Audio.Media.BITRATE)
    }.toTypedArray()
    runCatching {
        context.contentResolver.query(song.uri, columns, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                mime = c.getString(0)
                path = c.getString(1)
                size = c.getLong(2).takeIf { it > 0 }
                if (columns.size > 3) bitrate = c.getLong(3).takeIf { it > 0 }?.let { (it / 1000).toInt() }
            }
        }
    }
    var sampleRate: Int? = null
    runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, song.uri)
            if (bitrate == null) {
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.let { (it / 1000).toInt() }
            }
            if (mime == null) mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (Build.VERSION.SDK_INT >= 31) {
                sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
            }
        } finally {
            retriever.release()
        }
    }
    FileDetails(mime, bitrate, sampleRate, path, size)
}

/** "…" while the file is still being read, "—" once it's known there's nothing. */
private fun dash(details: FileDetails?): String = if (details == null) "…" else "—"

private fun formatLength(ms: Long): String {
    val total = ms / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

@Composable
private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "—"
    bytes >= 1024L * 1024 -> stringResource(R.string.unit_mb, bytes / 1024f / 1024f)
    else -> stringResource(R.string.unit_kb, (bytes / 1024).toInt())
}
