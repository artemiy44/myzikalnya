package com.artemiy.player.ui.search

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artemiy.player.data.IndexedLyrics
import com.artemiy.player.data.LyricsIndexDatabase
import com.artemiy.player.data.Song
import com.artemiy.player.data.normalizeForSearch
import com.artemiy.player.lyrics.LyricsExtractor
import com.artemiy.player.lyrics.ParsedLyrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "LyricsSearch"

/** A song whose lyrics contain the query, plus the line it was found in. */
data class LyricsHit(val song: Song, val line: String, val marks: List<IntRange> = emptyList())

class LyricsSearchViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = LyricsIndexDatabase.get(app).dao()
    private var syncJob: Job? = null

    /** How many songs have lyrics so far (the welcome screens show it growing). */
    val withLyrics: kotlinx.coroutines.flow.Flow<Int> = dao.countWithLyrics()

    /** Files read so far / files that needed reading in the current pass; null when idle. */
    var indexProgress by mutableStateOf<Pair<Int, Int>?>(null)
        private set

    /**
     * Brings the lyrics cache in line with [songs]: drops songs that are gone, reads lyrics for
     * new ones and for files that changed since they were read. Unchanged songs aren't touched,
     * so after the first full pass this is nearly instant. One file at a time with pauses — and
     * much slower while music is playing, so tag reading never competes with playback.
     */
    fun sync(songs: List<Song>, isPlaying: () -> Boolean) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            val known = dao.signatures().associate { it.songId to it.signature }
            val current = songs.associateBy { it.id }
            val gone = known.keys - current.keys
            gone.chunked(500).forEach { dao.delete(it) }

            val stale = songs.filter { known[it.id] != signatureOf(it) }
            if (stale.isEmpty()) return@launch
            Log.d(TAG, "Indexing lyrics: ${stale.size} to read, ${gone.size} removed")
            stale.forEachIndexed { i, song ->
                indexProgress = i to stale.size
                delay(if (isPlaying()) 1000L else 60L)
                val lyrics = try {
                    LyricsExtractor.extract(getApplication(), song.uri)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Could not read lyrics of ${song.title}", e)
                    null
                }
                val text = lyrics?.let(::plainText).orEmpty()
                // The same lines in Latin letters, so a song in Japanese, Korean... is found by typing it as it sounds.
                val roman = if (text.isEmpty()) "" else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    runCatching { com.artemiy.player.lyrics.LyricsRomanizer.romanizedLines(text.lines()).joinToString("\n") }.getOrDefault("")
                }
                dao.put(IndexedLyrics(song.id, signatureOf(song), text, normalizeForSearch(text), roman, normalizeForSearch(com.artemiy.player.data.stripMarks(roman))))
                if ((i + 1) % 200 == 0) Log.d(TAG, "Indexed ${i + 1} of ${stale.size}")
            }
            indexProgress = null
            Log.d(TAG, "Lyrics index up to date")
        }
    }

    suspend fun search(query: String, songs: List<Song>): List<LyricsHit> {
        val needle = normalizeForSearch(query.trim())
        if (!com.artemiy.player.data.lyricsQueryLongEnough(needle)) return emptyList()
        fun escape(text: String) = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val escaped = escape(needle)
        val romanNeedle = com.artemiy.player.data.stripMarks(needle)
        val byId = songs.associateBy { it.id }
        return dao.search(escaped, escape(romanNeedle), limit = 100).mapNotNull { match ->
            val song = byId[match.songId] ?: return@mapNotNull null
            val textLines = match.text.lines()
            val found = textLines.firstOrNull { normalizeForSearch(it).contains(needle) }
            if (found != null) return@mapNotNull LyricsHit(song, found.trim())
            // Not in the words as written: found by how it sounds — the line it belongs to, with the characters that sound like it.
            val index = match.roman.lines().indexOfFirst { normalizeForSearch(com.artemiy.player.data.stripMarks(it)).contains(romanNeedle) }
            if (index < 0) return@mapNotNull null
            val line = textLines.getOrNull(index) ?: return@mapNotNull null
            val marks = runCatching {
                com.artemiy.player.lyrics.LyricsRomanizer.soundsLike(textLines, index, romanNeedle)
            }.getOrDefault(emptyList())
            val lead = line.length - line.trimStart().length
            LyricsHit(song, line.trim(), marks.map { (it.first - lead).coerceAtLeast(0)..(it.last - lead).coerceAtLeast(0) })
        }
    }

    // Lyrics files beside the songs count once the app may read them: the index is read again when that changes.
    private fun signatureOf(song: Song) = "${song.modifiedAtS}:${song.sizeBytes}:r2:" + (if (com.artemiy.player.lyrics.LyricsExtractor.canReadSidecars(getApplication())) "s" else "")

    /** The lyrics as plain lines — main lines plus translations, the other singer's part and
     * background vocals, since any of them is something a person might search for. */
    private fun plainText(lyrics: ParsedLyrics): String = when (lyrics) {
        is ParsedLyrics.Unsynced -> lyrics.text
        is ParsedLyrics.Synced -> lyrics.lines.flatMap { line ->
            listOf(line.text) + line.secondary.map { it.text } + listOfNotNull(line.background?.text)
        }.joinToString("\n")
    }
}
