package com.artemiy.player.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.R
import com.artemiy.player.data.Backup
import com.artemiy.player.data.Song
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.barsInset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup file (playlists, listening stats, settings) and M3U playlist import. [onChanged] runs
 * after anything was put back, so the lists on screen read the new data.
 */
@Composable
internal fun BackupContent(songs: List<Song>, onChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<List<String>>(emptyList()) }
    val badFile = stringResource(R.string.backup_bad_file)
    val saved = stringResource(R.string.backup_saved)
    val settingsBack = stringResource(R.string.backup_done_settings)

    fun report(r: Backup.Report): List<String> = buildList {
        if (r.playlists > 0) add(context.getString(R.string.backup_done_lists, r.playlists, r.songsPlaced, r.songsMissing))
        if (r.plays > 0 || r.skips > 0) add(context.getString(R.string.backup_done_stats, r.plays, r.skips))
        if (r.settings) add(settingsBack)
    }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            result = runCatching {
                withContext(Dispatchers.IO) {
                    val text = Backup.export(context, songs)
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
                }
                listOf(saved)
            }.getOrElse { listOf(badFile) }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            result = runCatching {
                val r = withContext(Dispatchers.IO) { Backup.import(context, songs, readText(context, uri)) }
                onChanged()
                report(r)
            }.getOrElse { listOf(badFile) }
        }
    }
    val importM3u = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            result = runCatching {
                val r = withContext(Dispatchers.IO) {
                    val name = displayName(context, uri).substringBeforeLast('.').ifBlank { "M3U" }
                    Backup.importM3u(context, songs, name, readText(context, uri))
                }
                onChanged()
                report(r)
            }.getOrElse { listOf(badFile) }
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState()).barsInset()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            SettingsRow(
                icon = AppIcons.Download,
                title = stringResource(R.string.backup_save),
                subtitle = stringResource(R.string.backup_save_sub),
                onClick = {
                    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    save.launch("Lumine-backup-$stamp.json")
                },
            )
            CategoryDivider()
            SettingsRow(
                icon = AppIcons.Upload,
                title = stringResource(R.string.backup_restore),
                subtitle = stringResource(R.string.backup_restore_sub),
                onClick = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCard {
            SettingsRow(
                icon = AppIcons.Playlist,
                title = stringResource(R.string.backup_m3u_import),
                subtitle = stringResource(R.string.backup_m3u_import_sub),
                onClick = { importM3u.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "application/vnd.apple.mpegurl", "text/plain", "*/*")) },
            )
        }
        if (result.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard {
                result.forEach { Text(text = it, color = PlayerColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp)) }
            }
        }
    }
}

private fun readText(context: Context, uri: Uri): String =
    context.contentResolver.openInputStream(uri)!!.use { String(it.readBytes(), Charsets.UTF_8) }

private fun displayName(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }.orEmpty()
