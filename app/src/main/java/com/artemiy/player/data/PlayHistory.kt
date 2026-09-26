package com.artemiy.player.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)

/** A song switched away from within its first seconds — "not in the mood for this one". */
@Entity(tableName = "skip_events")
data class SkipEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val skippedAt: Long,
)

/** One play or skip: which song and when. */
data class SongEvent(val songId: Long, val at: Long)

data class SongPlayCount(
    val songId: Long,
    val playCount: Int,
)

@Dao
interface PlayHistoryDao {
    @Insert
    suspend fun insert(entry: PlayHistoryEntity)

    @Query(
        "SELECT songId, COUNT(*) as playCount FROM play_history " +
            "GROUP BY songId ORDER BY playCount DESC LIMIT :limit"
    )
    suspend fun topPlayed(limit: Int): List<SongPlayCount>

    @Query(
        "SELECT songId FROM play_history GROUP BY songId ORDER BY MAX(playedAt) DESC LIMIT :limit"
    )
    suspend fun recentlyPlayed(limit: Int): List<Long>

    /** Every play ever — the raw material for mixes and the weekly recap. */
    @Query("SELECT songId, playedAt AS at FROM play_history")
    suspend fun allPlays(): List<SongEvent>

    @Insert
    suspend fun insertSkip(entry: SkipEventEntity)

    @Query("SELECT songId, skippedAt AS at FROM skip_events")
    suspend fun allSkips(): List<SongEvent>
}
