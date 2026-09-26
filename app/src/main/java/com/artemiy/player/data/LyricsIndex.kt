package com.artemiy.player.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * A copy of each song's lyrics text, read out of the file's tags once, so lyrics can be searched
 * without opening every file on each query. The files themselves are only ever read.
 *
 * [signature] is the file's modified-time + size at the moment it was read: when it no longer
 * matches, the file changed and its lyrics are read again. Songs without lyrics are stored too
 * (empty [text]) so they aren't re-read on every scan.
 */
@Entity(tableName = "indexed_lyrics")
data class IndexedLyrics(
    @PrimaryKey val songId: Long,
    val signature: String,
    /** Lyric lines as shown, one per line — used to cut out the matching line for results. */
    val text: String,
    /** [text] run through [normalizeForSearch] — what queries are actually matched against. */
    val searchable: String,
)

data class IndexedSignature(val songId: Long, val signature: String)

data class LyricsMatch(val songId: Long, val text: String)

/** Case-insensitive for every alphabet (SQLite's own LIKE only folds ASCII), "ё" = "е", and
 * curly apostrophes = straight ones. Keeps the string length unchanged for the characters it
 * touches, so a match position in the normalized text is also valid in the original. */
fun normalizeForSearch(text: String): String =
    text.lowercase().replace('ё', 'е').replace('’', '\'').replace('‘', '\'')

@Dao
interface LyricsIndexDao {
    @Query("SELECT songId, signature FROM indexed_lyrics")
    suspend fun signatures(): List<IndexedSignature>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: IndexedLyrics)

    @Query("DELETE FROM indexed_lyrics WHERE songId IN (:songIds)")
    suspend fun delete(songIds: List<Long>)

    /** [pattern] must already be normalized and have `%`, `_` and `\` escaped with `\`. */
    @Query("SELECT songId, text FROM indexed_lyrics WHERE searchable LIKE '%' || :pattern || '%' ESCAPE '\\' LIMIT :limit")
    suspend fun search(pattern: String, limit: Int): List<LyricsMatch>
}

/** Separate from [AppDatabase] on purpose: this is a rebuildable cache, so a schema change can
 * just wipe it — while the main database holds playlists and play history that must survive. */
@Database(entities = [IndexedLyrics::class], version = 1, exportSchema = false)
abstract class LyricsIndexDatabase : RoomDatabase() {
    abstract fun dao(): LyricsIndexDao

    companion object {
        @Volatile private var instance: LyricsIndexDatabase? = null

        fun get(context: Context): LyricsIndexDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, LyricsIndexDatabase::class.java, "lyrics_index.db")
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
