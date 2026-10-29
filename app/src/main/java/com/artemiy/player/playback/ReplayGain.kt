package com.artemiy.player.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import com.artemiy.player.data.ReplayGainMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Volume levelling from the ReplayGain tags already in the files (written by foobar2000, MP3Gain, beets…) — nothing is measured
 * here and nothing is ever written to a file. Songs without the tags are left alone, and only loud ones are turned down:
 * turning a quiet one up could push it into distortion.
 */
object ReplayGain {
    /** The gains found in a file, in dB; null where the file has none. */
    data class Tags(val track: Float?, val album: Float?)

    private val none = Tags(null, null)
    private val cache = object : LinkedHashMap<String, Tags>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Tags>?) = size > 300
    }

    /** What is known about [uri] already, without reading the file. */
    fun cached(uri: Uri?): Tags? = uri?.let { synchronized(cache) { cache[it.toString()] } }

    suspend fun read(context: Context, uri: Uri): Tags {
        cached(uri)?.let { return it }
        val tags = withContext(Dispatchers.IO) {
            runCatching { readTags(context, uri) }.getOrDefault(none)
        }
        synchronized(cache) { cache[uri.toString()] = tags }
        return tags
    }

    /** How much to scale the volume by for [mode] (1 = untouched). */
    fun factor(tags: Tags?, mode: ReplayGainMode): Float {
        if (tags == null || mode == ReplayGainMode.OFF) return 1f
        val db = when (mode) {
            ReplayGainMode.ALBUM -> tags.album ?: tags.track
            else -> tags.track ?: tags.album
        } ?: return 1f
        return Math.pow(10.0, db / 20.0).toFloat().coerceIn(0.05f, 1f)
    }

    fun uriOf(item: MediaItem?): Uri? = item?.localConfiguration?.uri

    private suspend fun readTags(context: Context, uri: Uri): Tags {
        @Suppress("DEPRECATION")
        val groups = MetadataRetriever.retrieveMetadata(context, MediaItem.fromUri(uri)).get()
        var track: Float? = null
        var album: Float? = null
        fun take(name: String, value: String?) {
            val gain = number(value) ?: return
            when (name.uppercase()) {
                "REPLAYGAIN_TRACK_GAIN" -> track = gain
                "REPLAYGAIN_ALBUM_GAIN" -> album = gain
            }
        }
        for (g in 0 until groups.length) {
            val group = groups[g]
            for (f in 0 until group.length) {
                val metadata = group.getFormat(f).metadata ?: continue
                for (i in 0 until metadata.length()) {
                    when (val entry = metadata[i]) {
                        is VorbisComment -> take(entry.key, entry.value)
                        is TextInformationFrame -> if (entry.id == "TXXX") take(entry.description.orEmpty(), entry.values.firstOrNull())
                        is InternalFrame -> take(entry.description, entry.text)
                    }
                }
            }
        }
        return Tags(track, album)
    }

    /** "-6.54 dB", "-6,54", "+1.2 dB" → the number. */
    private fun number(text: String?): Float? =
        text?.let { Regex("[-+]?\\d+(?:[.,]\\d+)?").find(it)?.value?.replace(',', '.')?.toFloatOrNull() }
}
