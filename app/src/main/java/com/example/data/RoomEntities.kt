package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val filePath: String,
    val position: Long,
    val duration: Long,
    val lastPlayed: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val filePath: String,
    val isVideo: Boolean,
    val title: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_items")
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playlistId: Long,
    val filePath: String,
    val isVideo: Boolean,
    val title: String,
    val addedAt: Long = System.currentTimeMillis()
)
