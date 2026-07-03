package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class MediaRepository(private val dao: MediaDao) {

    // Database streams
    val allFavorites = dao.getAllFavorites()
    val allHistory = dao.getAllHistory()
    val allPlaylists = dao.getAllPlaylists()

    // Scanned lists in memory
    val videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val audios = MutableStateFlow<List<AudioItem>>(emptyList())
    val folders = MutableStateFlow<List<FolderItem>>(emptyList())

    fun getPlaylistItems(playlistId: Long) = dao.getItemsForPlaylist(playlistId)

    suspend fun refreshMedia(context: Context) {
        withContext(Dispatchers.IO) {
            val rawVideos = MediaScanner.scanVideos(context)
            val rawAudios = MediaScanner.scanAudios(context)

            // Cross-reference with database to fetch playback position & favorite status
            val updatedVideos = rawVideos.map { video ->
                val progress = dao.getHistoryItem(video.path)?.position ?: 0L
                val isFav = dao.isFavoriteSync(video.path)
                video.copy(progress = progress, isFavorite = isFav)
            }

            val updatedAudios = rawAudios.map { audio ->
                val isFav = dao.isFavoriteSync(audio.path)
                audio.copy(isFavorite = isFav)
            }

            val computedFolders = MediaScanner.getFolders(updatedVideos, updatedAudios)

            videos.value = updatedVideos
            audios.value = updatedAudios
            folders.value = computedFolders
        }
    }

    suspend fun getHistoryPosition(path: String): Long {
        return withContext(Dispatchers.IO) {
            dao.getHistoryItem(path)?.position ?: 0L
        }
    }

    suspend fun savePlaybackPosition(path: String, position: Long, duration: Long) {
        withContext(Dispatchers.IO) {
            dao.insertHistory(PlaybackHistoryEntity(path, position, duration))
        }
    }

    suspend fun deletePlaybackPosition(path: String) {
        withContext(Dispatchers.IO) {
            dao.deleteHistory(path)
        }
    }

    suspend fun toggleFavorite(path: String, isVideo: Boolean, title: String) {
        withContext(Dispatchers.IO) {
            if (dao.isFavoriteSync(path)) {
                dao.deleteFavorite(path)
            } else {
                dao.insertFavorite(FavoriteEntity(path, isVideo, title))
            }
        }
    }

    suspend fun createPlaylist(name: String): Long {
        return withContext(Dispatchers.IO) {
            dao.insertPlaylist(PlaylistEntity(name = name))
        }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        withContext(Dispatchers.IO) {
            dao.deletePlaylist(playlistId)
            dao.deletePlaylistItems(playlistId)
        }
    }

    suspend fun addMediaToPlaylist(playlistId: Long, path: String, isVideo: Boolean, title: String) {
        withContext(Dispatchers.IO) {
            dao.insertPlaylistItem(
                PlaylistItemEntity(
                    playlistId = playlistId,
                    filePath = path,
                    isVideo = isVideo,
                    title = title
                )
            )
        }
    }

    suspend fun removeMediaFromPlaylist(playlistId: Long, path: String) {
        withContext(Dispatchers.IO) {
            dao.deletePlaylistItem(playlistId, path)
        }
    }
}
