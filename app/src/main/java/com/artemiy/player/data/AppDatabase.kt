package com.artemiy.player.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds skip tracking; everything already stored stays untouched. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `skip_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `songId` INTEGER NOT NULL, `skippedAt` INTEGER NOT NULL)",
        )
    }
}

@Database(
    entities = [PlayHistoryEntity::class, PlaylistEntity::class, PlaylistSongEntity::class, SkipEventEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "player.db",
                )
                    // Real data lives here now (play history, playlists): schema changes get a
                    // proper migration, never a wipe.
                    .addMigrations(MIGRATION_2_3)
                    .build().also { instance = it }
            }
    }
}
