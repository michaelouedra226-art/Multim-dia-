package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    // Playback History
    @Query("SELECT * FROM playback_history WHERE filePath = :filePath LIMIT 1")
    suspend fun getHistoryItem(filePath: String): PlaybackHistoryEntity?

    @Query("SELECT * FROM playback_history ORDER BY lastPlayed DESC")
    fun getAllHistory(): Flow<List<PlaybackHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE filePath = :filePath")
    suspend fun deleteHistory(filePath: String)

    // Favorites
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE filePath = :filePath LIMIT 1)")
    fun isFavorite(filePath: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE filePath = :filePath LIMIT 1)")
    suspend fun isFavoriteSync(filePath: String): Boolean

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE filePath = :filePath")
    suspend fun deleteFavorite(filePath: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId LIMIT 1")
    fun getPlaylistById(playlistId: Long): Flow<PlaylistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deletePlaylistItems(playlistId: Long)

    // Playlist Items
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    fun getItemsForPlaylist(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND filePath = :filePath")
    suspend fun deletePlaylistItem(playlistId: Long, filePath: String)
}
