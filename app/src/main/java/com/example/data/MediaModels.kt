package com.example.data

import java.io.Serializable

data class VideoItem(
    val id: Long,
    val title: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val progress: Long = 0L,
    val isFavorite: Boolean = false
) : Serializable

data class AudioItem(
    val id: Long,
    val title: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val artist: String?,
    val album: String?,
    val albumId: Long,
    val dateAdded: Long,
    val isFavorite: Boolean = false
) : Serializable

data class FolderItem(
    val name: String,
    val path: String,
    val fileCount: Int,
    val isVideo: Boolean
) : Serializable
