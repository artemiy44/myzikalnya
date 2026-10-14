package com.artemiy.player.lyrics

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "LyricsExtractor"

object LyricsExtractor {

    /** Reads embedded lyrics from a track's tags (ID3 USLT/SYLT, Vorbis LYRICS, MP4 lyr atom). */
    suspend fun extract(context: Context, uri: Uri): ParsedLyrics? = withContext(Dispatchers.IO) {
        val trackGroups = try {
            @Suppress("DEPRECATION")
            MetadataRetriever.retrieveMetadata(context, MediaItem.fromUri(uri)).get()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read metadata for $uri", e)
            return@withContext null
        }

        var sampleRate = 44100
        val synced = mutableListOf<ParsedLyrics.Synced>()
        var unsyncedFallback: String? = null

        for (groupIndex in 0 until trackGroups.length) {
            val group = trackGroups[groupIndex]
            for (formatIndex in 0 until group.length) {
                val format = group.getFormat(formatIndex)
                if (format.sampleRate > 0) sampleRate = format.sampleRate
                val metadata = format.metadata ?: continue

                for (entryIndex in 0 until metadata.length()) {
                    when (val entry = metadata.get(entryIndex)) {
                        is VorbisComment -> if (entry.key == "LYRICS") {
                            addText(entry.value, synced, onUnsynced = { unsyncedFallback = it })
                        }

                        is BinaryFrame -> when (entry.id) {
                            "SYLT", "SLT" -> {
                                val sylt = runCatching {
                                    UsltFrameDecoder.decodeSylt(ParsableByteArray(entry.data), sampleRate)
                                }.getOrNull()
                                if (sylt != null && sylt.lines.isNotEmpty()) {
                                    synced.add(ParsedLyrics.Synced(buildLinesFromSylt(sylt.lines)))
                                }
                            }

                            "USLT", "ULT" -> {
                                val uslt = runCatching { UsltFrameDecoder.decodeUslt(ParsableByteArray(entry.data)) }.getOrNull()
                                uslt?.text?.let { addText(it, synced, onUnsynced = { text -> unsyncedFallback = text }) }
                            }
                        }

                        is TextInformationFrame -> if (entry.id == "USLT" || entry.id == "©lyr") {
                            addText(entry.values.joinToString("\n"), synced, onUnsynced = { unsyncedFallback = it })
                        }
                    }
                }
            }
        }

        // Prefer word-level synced lyrics, then any synced, then plain unsynced text.
        val embedded = synced.maxByOrNull { it.lines.count { line -> line.words != null } }
            ?: unsyncedFallback?.let { ParsedLyrics.Unsynced(it) }
        if (embedded != null) return@withContext embedded
        // Nothing inside the file: look for a .elrc / .lrc with the song's name next to it (what a lyrics
        // downloader leaves). Always second to the lyrics written into the file.
        val sidecarText = sidecarText(context, uri) ?: return@withContext null
        val sidecarSynced = mutableListOf<ParsedLyrics.Synced>()
        var sidecarPlain: String? = null
        addText(sidecarText, sidecarSynced, onUnsynced = { sidecarPlain = it })
        sidecarSynced.firstOrNull() ?: sidecarPlain?.let { ParsedLyrics.Unsynced(it) }
    }

    /** Whether the app may read files other than media ones (the .lrc next to a song): all-files access on Android 11+, the usual storage permission before. */
    fun canReadSidecars(context: Context): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= 30) android.os.Environment.isExternalStorageManager()
        else androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** The text of the `.elrc` (else `.lrc`) file beside the song, or null when there is none or it can't be read. */
    private fun sidecarText(context: Context, uri: Uri): String? {
        if (!canReadSidecars(context)) return null
        val path = runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.Audio.Media.DATA), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: return null
        val base = path.substringBeforeLast('.', path)
        for (ext in listOf("elrc", "lrc", "ELRC", "LRC")) {
            val file = java.io.File("$base.$ext")
            if (!file.isFile || !file.canRead()) continue
            val text = runCatching { file.readText(Charsets.UTF_8).trimStart('\uFEFF') }.getOrNull()
            if (!text.isNullOrBlank()) return text
        }
        return null
    }

    private fun addText(raw: String, synced: MutableList<ParsedLyrics.Synced>, onUnsynced: (String) -> Unit) {
        if (raw.isBlank()) return
        val lines = parsePlainLrc(raw)
        if (lines.isEmpty()) {
            onUnsynced(raw)
            return
        }
        // Some files put every line at the same moment ("[00:00.000]" on all of them) — synced in
        // form only. Lines sharing a moment count as one line with a translation under it, which
        // turned the whole song into a single giant "line". Fewer distinct moments than a third
        // of the lines (a real translation pairs them up: half) means it's plain text really.
        val all = lines.flatMap { listOf(it) + it.secondary }
        if (all.size >= 4 && all.map { it.timeMs }.distinct().size * 3 < all.size) {
            onUnsynced(all.joinToString("\n") { it.text })
            return
        }
        synced.add(ParsedLyrics.Synced(lines))
    }
}
