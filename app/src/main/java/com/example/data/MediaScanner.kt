package com.example.data

import android.content.Context
import android.provider.MediaStore
import java.io.File

object MediaScanner {

    fun scanVideos(context: Context): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Unknown Video"
                    val path = cursor.getString(pathCol) ?: ""
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)

                    // Only add existing files
                    if (path.isNotEmpty() && File(path).exists()) {
                        list.add(VideoItem(id, name, path, duration, size, dateAdded))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun scanAudios(context: Context): List<AudioItem> {
        val list = mutableListOf<AudioItem>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Unknown Audio"
                    val path = cursor.getString(pathCol) ?: ""
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val artist = cursor.getString(artistCol)
                    val album = cursor.getString(albumCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val dateAdded = cursor.getLong(dateCol)

                    if (path.isNotEmpty() && File(path).exists()) {
                        val isWhatsAppVoice = path.contains("WhatsApp", ignoreCase = true) || 
                                              path.contains("Sent", ignoreCase = true) || 
                                              path.contains("Voice Notes", ignoreCase = true)
                        if (!isWhatsAppVoice) {
                            list.add(AudioItem(id, name, path, duration, size, artist, album, albumId, dateAdded))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getFolders(videos: List<VideoItem>, audios: List<AudioItem>): List<FolderItem> {
        val foldersMap = mutableMapOf<String, Int>()
        val folderIsVideo = mutableMapOf<String, Boolean>()

        videos.forEach { video ->
            val parent = File(video.path).parent ?: "Stockage externe"
            foldersMap[parent] = (foldersMap[parent] ?: 0) + 1
            folderIsVideo[parent] = true
        }

        audios.forEach { audio ->
            val parent = File(audio.path).parent ?: "Stockage externe"
            foldersMap[parent] = (foldersMap[parent] ?: 0) + 1
            if (folderIsVideo[parent] == null) {
                folderIsVideo[parent] = false
            }
        }

        return foldersMap.map { (path, count) ->
            val name = File(path).name
            FolderItem(name, path, count, folderIsVideo[path] ?: false)
        }.sortedBy { it.name }
    }
}
