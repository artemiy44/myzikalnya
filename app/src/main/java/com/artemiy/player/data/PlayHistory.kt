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
}
