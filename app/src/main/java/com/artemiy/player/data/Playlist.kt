package com.artemiy.player.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val songCount: Int,
)

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query(
        "SELECT p.id as id, p.name as name, COUNT(ps.songId) as songCount " +
            "FROM playlists p LEFT JOIN playlist_songs ps ON ps.playlistId = p.id " +
            "GROUP BY p.id ORDER BY p.name COLLATE NOCASE ASC"
    )
    suspend fun getPlaylistsWithCount(): List<PlaylistWithCount>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getSongIds(playlistId: Long): List<Long>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Insert
    suspend fun insertPlaylistSong(entry: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistRow(playlistId: Long)

    @Transaction
    suspend fun deletePlaylist(playlistId: Long) {
        clearPlaylist(playlistId)
        deletePlaylistRow(playlistId)
    }
}
