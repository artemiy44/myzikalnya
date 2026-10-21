package com.artemiy.player.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A song that was played for a while (see QUEUE_HISTORY_MIN_MS) — the "history" above the queue. Kept apart from
 * play_history on purpose: that one feeds mixes and the recap and must survive "clear history".
 */
@Entity(tableName = "queue_history")
data class QueueHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)

/** The most songs the history keeps; older ones are dropped. */
const val QUEUE_HISTORY_LIMIT = 100

/** How long a song must have played to count as heard. */
const val QUEUE_HISTORY_MIN_MS = 10_000L

@Dao
interface QueueHistoryDao {
    @Insert
    suspend fun insert(entry: QueueHistoryEntity)

    /** Song ids, the latest first. */
    @Query("SELECT songId FROM queue_history ORDER BY id DESC LIMIT $QUEUE_HISTORY_LIMIT")
    fun latest(): Flow<List<Long>>

    @Query("DELETE FROM queue_history WHERE id NOT IN (SELECT id FROM queue_history ORDER BY id DESC LIMIT $QUEUE_HISTORY_LIMIT)")
    suspend fun trim()

    @Query("DELETE FROM queue_history")
    suspend fun clear()
}
