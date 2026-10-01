package com.artemiy.player.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup file and M3U playlists. A song is never stored by its id (that changes with every
 * rescan or a new phone) but by where it lives and what it's called — folder path, file name,
 * title, artist, length — and found again among the library's songs by those.
 */
object Backup {

    /** What an import did, for the card shown afterwards. */
    data class Report(
        val playlists: Int = 0,
        val songsPlaced: Int = 0,
        val songsMissing: Int = 0,
        val plays: Int = 0,
        val skips: Int = 0,
        val settings: Boolean = false,
    )

    private fun pathOf(song: Song): String =
        (song.pathSegments + song.fileName).filter { it.isNotEmpty() }.joinToString("/")

    /** Finds library songs from the traces a backup or an M3U keeps. */
    class Matcher(songs: List<Song>) {
        private val byPath = HashMap<String, Song>()
        private val byName = HashMap<String, MutableList<Song>>()
        private val byTitle = HashMap<String, MutableList<Song>>()

        init {
            for (s in songs) {
                byPath[pathOf(s).lowercase()] = s
                if (s.fileName.isNotEmpty()) byName.getOrPut(s.fileName.lowercase()) { mutableListOf() }.add(s)
                byTitle.getOrPut(titleKey(s.title, s.artist)) { mutableListOf() }.add(s)
            }
        }

        private fun titleKey(title: String, artist: String) = GenreNames.key(title) + "|" + GenreNames.key(artist)

        fun find(path: String?, title: String?, artist: String?, durationMs: Long?): Song? {
            val clean = path?.replace('\\', '/')?.trim()?.removePrefix("file://")?.trim('/')?.lowercase().orEmpty()
            if (clean.isNotEmpty()) {
                byPath[clean]?.let { return it }
                // The file name, and of several of that name the one whose folders agree most.
                val name = clean.substringAfterLast('/')
                val candidates = byName[name]
                if (!candidates.isNullOrEmpty()) {
                    val parts = clean.split('/')
                    return candidates.maxByOrNull { c ->
                        val mine = pathOf(c).lowercase().split('/')
                        parts.asReversed().zip(mine.asReversed()).takeWhile { it.first == it.second }.size
                    }
                }
            }
            if (!title.isNullOrBlank()) {
                val list = byTitle[titleKey(title, artist.orEmpty())] ?: byTitle.entries.firstOrNull { artist.isNullOrBlank() && it.key.startsWith(GenreNames.key(title) + "|") }?.value
                if (!list.isNullOrEmpty()) {
                    if (durationMs != null && durationMs > 0) {
                        list.minByOrNull { kotlin.math.abs(it.durationMs - durationMs) }?.let { return it }
                    }
                    return list.first()
                }
            }
            return null
        }
    }

    // ---------------------------------------------------------------- backup file

    suspend fun export(context: Context, songs: List<Song>): String {
        val db = AppDatabase.get(context)
        val byId = songs.associateBy { it.id }
        val table = ArrayList<JSONObject>()
        val index = HashMap<Long, Int>()
        fun ref(id: Long): Int? {
            index[id]?.let { return it }
            val s = byId[id] ?: return null
            table.add(
                JSONObject().put("p", pathOf(s)).put("t", s.title).put("a", s.artist).put("d", s.durationMs),
            )
            return (table.size - 1).also { index[id] = it }
        }
        val playlists = JSONArray()
        for (p in db.playlistDao().allPlaylists()) {
            val ids = JSONArray()
            db.playlistDao().getSongIds(p.id).forEach { id -> ref(id)?.let(ids::put) }
            playlists.put(JSONObject().put("name", p.name).put("createdAt", p.createdAt).put("songs", ids))
        }
        fun events(list: List<SongEvent>) = JSONArray().also { arr ->
            for (e in list) ref(e.songId)?.let { arr.put(JSONArray().put(it).put(e.at)) }
        }
        val plays = events(db.playHistoryDao().allPlays())
        val skips = events(db.playHistoryDao().allSkips())
        return JSONObject()
            .put("app", "Lumine")
            .put("format", 1)
            .put("createdAt", System.currentTimeMillis())
            .put("songs", JSONArray(table))
            .put("playlists", playlists)
            .put("plays", plays)
            .put("skips", skips)
            .put("settings", SettingsRepository(context).dump())
            .toString()
    }

    /** Merges a backup into the app: playlists by name, plays and skips not already there, settings as saved. */
    suspend fun import(context: Context, songs: List<Song>, text: String): Report {
        val root = JSONObject(text)
        require(root.optString("app") == "Lumine") { "not a Lumine backup" }
        val db = AppDatabase.get(context)
        val matcher = Matcher(songs)
        val table = root.optJSONArray("songs") ?: JSONArray()
        val resolved = arrayOfNulls<Song>(table.length())
        for (i in 0 until table.length()) {
            val o = table.getJSONObject(i)
            resolved[i] = matcher.find(o.optString("p"), o.optString("t"), o.optString("a"), o.optLong("d"))
        }
        var placed = 0
        var missing = 0
        var playlistCount = 0
        val dao = db.playlistDao()
        val existing = dao.allPlaylists().associateBy { it.name.trim().lowercase() }
        val lists = root.optJSONArray("playlists") ?: JSONArray()
        for (i in 0 until lists.length()) {
            val o = lists.getJSONObject(i)
            val name = o.getString("name")
            val ids = o.getJSONArray("songs")
            val songIds = (0 until ids.length()).mapNotNull { resolved.getOrNull(ids.getInt(it))?.id }
            missing += ids.length() - songIds.size
            placed += addToPlaylist(dao, existing[name.trim().lowercase()], name, o.optLong("createdAt"), songIds)
            playlistCount++
        }
        val history = db.playHistoryDao()
        val havePlays = history.allPlays().mapTo(HashSet()) { it.songId to it.at }
        val haveSkips = history.allSkips().mapTo(HashSet()) { it.songId to it.at }
        var plays = 0
        var skips = 0
        root.optJSONArray("plays")?.let { arr ->
            for (i in 0 until arr.length()) {
                val e = arr.getJSONArray(i)
                val id = resolved.getOrNull(e.getInt(0))?.id ?: continue
                if (havePlays.add(id to e.getLong(1))) { history.insert(PlayHistoryEntity(songId = id, playedAt = e.getLong(1))); plays++ }
            }
        }
        root.optJSONArray("skips")?.let { arr ->
            for (i in 0 until arr.length()) {
                val e = arr.getJSONArray(i)
                val id = resolved.getOrNull(e.getInt(0))?.id ?: continue
                if (haveSkips.add(id to e.getLong(1))) { history.insertSkip(SkipEventEntity(songId = id, skippedAt = e.getLong(1))); skips++ }
            }
        }
        val settings = root.optJSONObject("settings")
        if (settings != null) SettingsRepository(context).restore(settings)
        return Report(playlistCount, placed, missing, plays, skips, settings != null)
    }

    /** Adds [songIds] to [playlist] (made first when null), skipping what's already in it. Returns how many went in. */
    private suspend fun addToPlaylist(dao: PlaylistDao, playlist: PlaylistEntity?, name: String, createdAt: Long, songIds: List<Long>): Int {
        val id = playlist?.id ?: dao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()))
        val have = dao.getSongIds(id).toHashSet()
        var position = dao.nextPosition(id)
        var added = 0
        for (songId in songIds.distinct()) {
            if (!have.add(songId)) continue
            dao.insertPlaylistSong(PlaylistSongEntity(id, songId, position++))
            added++
        }
        return added
    }

    // ---------------------------------------------------------------- M3U

    fun m3u(songs: List<Song>): String = buildString {
        append("#EXTM3U\n")
        for (s in songs) {
            append("#EXTINF:").append(s.durationMs / 1000).append(',')
            if (s.artist.isNotBlank()) append(s.artist).append(" - ")
            append(s.title).append('\n')
            append(pathOf(s)).append('\n')
        }
    }

    /** Reads an M3U / M3U8 into a new playlist called [name] (or adds to a playlist of that name). */
    suspend fun importM3u(context: Context, songs: List<Song>, name: String, text: String): Report {
        val matcher = Matcher(songs)
        val found = ArrayList<Long>()
        var missing = 0
        var title: String? = null
        var artist: String? = null
        var duration: Long? = null
        for (raw in text.removePrefix("﻿").lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val body = line.substringAfter(':')
                duration = body.substringBefore(',').trim().toLongOrNull()?.takeIf { it > 0 }?.times(1000)
                val label = body.substringAfter(',', "")
                if (" - " in label) { artist = label.substringBefore(" - ").trim(); title = label.substringAfter(" - ").trim() } else { artist = null; title = label.trim() }
                continue
            }
            if (line.startsWith("#")) continue
            val song = matcher.find(line, title, artist, duration)
            if (song != null) found.add(song.id) else missing++
            title = null; artist = null; duration = null
        }
        val dao = AppDatabase.get(context).playlistDao()
        val existing = dao.allPlaylists().firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) }
        val placed = addToPlaylist(dao, existing, name, System.currentTimeMillis(), found)
        return Report(playlists = 1, songsPlaced = placed, songsMissing = missing)
    }
}
