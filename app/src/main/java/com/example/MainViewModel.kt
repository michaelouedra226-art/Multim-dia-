package com.example

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(val repository: MediaRepository) : ViewModel() {

    // Scanner lists
    val videos: StateFlow<List<VideoItem>> = repository.videos.asStateFlow()
    val audios: StateFlow<List<AudioItem>> = repository.audios.asStateFlow()
    val folders: StateFlow<List<FolderItem>> = repository.folders.asStateFlow()

    // Database streams
    val favorites: StateFlow<List<FavoriteEntity>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<PlaybackHistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen / search states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Loading & Refreshing States
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Currently playing video (for video overlay)
    private val _activeVideo = MutableStateFlow<VideoItem?>(null)
    val activeVideo: StateFlow<VideoItem?> = _activeVideo.asStateFlow()

    // Selected folder drill-down state
    private val _selectedFolder = MutableStateFlow<FolderItem?>(null)
    val selectedFolder: StateFlow<FolderItem?> = _selectedFolder.asStateFlow()

    // Selected playlist drill-down state
    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()

    // Playlist items list for selected playlist
    val selectedPlaylistItems = _selectedPlaylist.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else repository.getPlaylistItems(playlist.playlistId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered lists based on search
    val filteredVideos = combine(videos, searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.title.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredAudios = combine(audios, searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.title.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun scanMedia(context: Context) {
        viewModelScope.launch {
            _isScanning.value = true
            repository.refreshMedia(context)
            _isScanning.value = false
        }
    }

    fun playVideo(video: VideoItem) {
        viewModelScope.launch {
            val savedPosition = repository.getHistoryPosition(video.path)
            _activeVideo.value = video.copy(progress = savedPosition)
        }
    }

    fun stopVideo() {
        _activeVideo.value = null
    }

    fun saveVideoProgress(path: String, progress: Long, duration: Long) {
        viewModelScope.launch {
            repository.savePlaybackPosition(path, progress, duration)
            // Trigger refresh in-memory so UI displays the progress bar
            _activeVideo.value?.let { current ->
                if (current.path == path) {
                    _activeVideo.value = current.copy(progress = progress)
                }
            }
        }
    }

    fun selectFolder(folder: FolderItem?) {
        _selectedFolder.value = folder
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
    }

    fun toggleFavorite(path: String, isVideo: Boolean, title: String) {
        viewModelScope.launch {
            repository.toggleFavorite(path, isVideo, title)
            // Re-map memory lists
            repository.videos.value = repository.videos.value.map {
                if (it.path == path) it.copy(isFavorite = !it.isFavorite) else it
            }
            repository.audios.value = repository.audios.value.map {
                if (it.path == path) it.copy(isFavorite = !it.isFavorite) else it
            }
        }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            onCreated(id)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylist.value?.playlistId == playlistId) {
                _selectedPlaylist.value = null
            }
        }
    }

    fun addMediaToPlaylist(playlistId: Long, path: String, isVideo: Boolean, title: String) {
        viewModelScope.launch {
            repository.addMediaToPlaylist(playlistId, path, isVideo, title)
        }
    }

    fun removeMediaFromPlaylist(playlistId: Long, path: String) {
        viewModelScope.launch {
            repository.removeMediaFromPlaylist(playlistId, path)
        }
    }
}
