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
        synced.maxByOrNull { it.lines.count { line -> line.words != null } }
            ?: unsyncedFallback?.let { ParsedLyrics.Unsynced(it) }
    }

    private fun addText(raw: String, synced: MutableList<ParsedLyrics.Synced>, onUnsynced: (String) -> Unit) {
        if (raw.isBlank()) return
        val lines = parsePlainLrc(raw)
        if (lines.isNotEmpty()) synced.add(ParsedLyrics.Synced(lines)) else onUnsynced(raw)
    }
}
