package com.artemiy.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dateAddedMs: Long,
    val uri: Uri,
    val genre: String? = null,
    /** Name of the folder directly containing the file (e.g. "рок", "спокойствие(?)"),
     * for mood folder whitelisting — genre tags don't reliably encode mood, but a
     * hand-organized folder often does. */
    val folder: String? = null,
    /** All directory names from the storage root down to the file's own folder — used to test
     * "is this file anywhere under one of these whitelisted folders" for the scan whitelist. */
    val pathSegments: List<String> = emptyList(),
    /** Release year from the file's own tag, when present — distinct from [dateAddedMs] (when
     * the file was scanned into the library), used for the "по дате выпуска" sort. */
    val year: Int? = null,
    /** File's last-modified time (seconds) and size — together a cheap "has this file changed"
     * fingerprint, e.g. for knowing when its cached lyrics need re-reading. */
    val modifiedAtS: Long = 0,
    val sizeBytes: Long = 0,
)

/**
 * @param scanFolders When non-empty, a whitelist of folder names (anywhere in a file's path,
 * not just its immediate parent — so picking "МУЗЫКА" also covers its "рок"/"попса"/... subfolders)
 * that a track must sit under to be included. MediaStore's own IS_MUSIC flag turned out to
 * unreliably separate real music from ringtone/notification-pack junk on the user's device, so
 * once they've picked real folders here we trust that instead and drop the IS_MUSIC filter
 * entirely. Empty (the default, before they've configured anything) falls back to IS_MUSIC.
 */
fun querySongs(context: Context, scanFolders: Set<String> = emptySet()): List<Song> {
    val songs = mutableListOf<Song>()

    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    // MediaStore only exposes genre directly on the main track table from API 30 onward; below
    // that it lives in a separate Genres/Members join, queried in genresByTrackIdLegacy().
    val readGenreDirectly = Build.VERSION.SDK_INT >= 30
    val readRelativePath = Build.VERSION.SDK_INT >= 29
    val projection = buildList {
        add(MediaStore.Audio.Media._ID)
        add(MediaStore.Audio.Media.TITLE)
        add(MediaStore.Audio.Media.ARTIST)
        add(MediaStore.Audio.Media.ALBUM)
        add(MediaStore.Audio.Media.DURATION)
        add(MediaStore.Audio.Media.DATE_ADDED)
        add(MediaStore.Audio.Media.DATE_MODIFIED)
        add(MediaStore.Audio.Media.SIZE)
        add(MediaStore.Audio.Media.YEAR)
        if (readGenreDirectly) add(MediaStore.Audio.Media.GENRE)
        if (readRelativePath) add(MediaStore.Audio.Media.RELATIVE_PATH) else add(MediaStore.Audio.Media.DATA)
    }.toTypedArray()
    val selection = if (scanFolders.isEmpty()) "${MediaStore.Audio.Media.IS_MUSIC} != 0" else null
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    val legacyGenres = if (!readGenreDirectly) genresByTrackIdLegacy(context) else emptyMap()

    context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
        val modifiedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
        val genreCol = if (readGenreDirectly) cursor.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
        val pathCol = cursor.getColumnIndex(if (readRelativePath) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val genre = if (genreCol >= 0) cursor.getString(genreCol) else legacyGenres[id]
            val rawPath = if (pathCol >= 0) cursor.getString(pathCol) else null
            val segments = pathSegmentsFrom(rawPath)
            if (scanFolders.isNotEmpty() && segments.none { it in scanFolders }) continue
            songs += Song(
                id = id,
                title = cursor.getString(titleCol) ?: "Без названия",
                artist = cursor.getString(artistCol) ?: "Неизвестный артист",
                album = cursor.getString(albumCol) ?: "",
                durationMs = cursor.getLong(durationCol),
                dateAddedMs = cursor.getLong(dateAddedCol) * 1000L,
                uri = ContentUris.withAppendedId(collection, id),
                genre = genre?.takeIf { it.isNotBlank() },
                folder = segments.lastOrNull(),
                pathSegments = segments,
                year = (if (yearCol >= 0) cursor.getInt(yearCol) else 0).takeIf { it > 0 },
                modifiedAtS = if (modifiedCol >= 0) cursor.getLong(modifiedCol) else 0,
                sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0,
            )
        }
    }

    return songs
}

/** Every distinct folder name across the device's whole audio index (ringtones, notifications,
 * downloads, music — everything, at every depth), so the scan-whitelist picker in Settings can
 * show real candidates even before the user has whitelisted anything (which is what makes
 * querySongs' own folder filtering possible in the first place). */
fun discoverAllAudioFolders(context: Context): List<String> {
    val readRelativePath = Build.VERSION.SDK_INT >= 29
    val pathColumn = if (readRelativePath) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
    val folders = mutableSetOf<String>()
    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        arrayOf(pathColumn),
        null, null, null,
    )?.use { cursor ->
        val pathCol = cursor.getColumnIndex(pathColumn)
        if (pathCol < 0) return@use
        while (cursor.moveToNext()) {
            folders += pathSegmentsFrom(cursor.getString(pathCol))
        }
    }
    return folders.sorted()
}

/** All directory names from a RELATIVE_PATH ("Music/МУЗЫКА/рок/") or full DATA path
 * (pre-API-29), deepest last. DATA includes the filename itself, so a trailing segment with a
 * file extension is dropped; RELATIVE_PATH never has one. */
private fun pathSegmentsFrom(path: String?): List<String> {
    if (path.isNullOrBlank()) return emptyList()
    val segments = path.split('/').filter { it.isNotBlank() }
    return if (segments.isNotEmpty() && segments.last().contains('.')) segments.dropLast(1) else segments
}

/** Pre-API-30 fallback: MediaStore.Audio.Genres has no direct per-track column, only a
 * genre -> member-track-id join, so we walk every genre bucket once and build the map ourselves. */
private fun genresByTrackIdLegacy(context: Context): Map<Long, String> {
    val result = mutableMapOf<Long, String>()
    val genresUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
    context.contentResolver.query(
        genresUri,
        arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
        null, null, null,
    )?.use { genreCursor ->
        val idCol = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
        val nameCol = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
        while (genreCursor.moveToNext()) {
            val genreId = genreCursor.getLong(idCol)
            val genreName = genreCursor.getString(nameCol) ?: continue
            val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
            context.contentResolver.query(
                membersUri,
                arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                null, null, null,
            )?.use { memberCursor ->
                val trackIdCol = memberCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                while (memberCursor.moveToNext()) {
                    result[memberCursor.getLong(trackIdCol)] = genreName
                }
            }
        }
    }
    return result
}
