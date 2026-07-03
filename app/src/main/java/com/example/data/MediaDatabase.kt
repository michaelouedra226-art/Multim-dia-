package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaybackHistoryEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: MediaDatabase? = null

        fun getDatabase(context: Context): MediaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediaDatabase::class.java,
                    "nova_player_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
