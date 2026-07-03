package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.AudioItem
import com.example.data.PlaylistEntity
import com.example.player.AudioPlaybackManager
import com.example.ui.formatTime
import java.io.File

@Composable
fun MusicTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val audios by viewModel.filteredAudios.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) } // 0: Tracks, 1: Playlists, 2: Favorites
    var showCreateDialog by remember { mutableStateOf(false) }

    if (selectedPlaylist != null) {
        // Render playlist drill-down detail screen
        PlaylistDetailScreen(viewModel = viewModel, playlist = selectedPlaylist!!)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("music_tab")
    ) {
        // Tab Row selector
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Morceaux") }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Playlists") }
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = { Text("Favoris") }
            )
        }

        when (activeSubTab) {
            0 -> {
                // Morceaux list
                if (audios.isEmpty()) {
                    MusicEmptyState(message = "Aucun morceau audio détecté.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(audios, key = { _, audio -> audio.path }) { index, audio ->
                            AudioTrackRow(
                                audio = audio,
                                onClick = { AudioPlaybackManager.setPlaylist(audios, index) },
                                onAddToPlaylist = { playlistId ->
                                    viewModel.addMediaToPlaylist(playlistId, audio.path, false, audio.title)
                                    Toast.makeText(context, "Ajouté à la playlist !", Toast.LENGTH_SHORT).show()
                                },
                                onToggleFavorite = {
                                    viewModel.toggleFavorite(audio.path, false, audio.title)
                                },
                                playlists = playlists,
                                onCreatePlaylist = { name ->
                                    viewModel.createPlaylist(name) { id ->
                                        viewModel.addMediaToPlaylist(id, audio.path, false, audio.title)
                                        Toast.makeText(context, "Playlist créée avec ce titre !", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Playlists custom category
                Column(modifier = Modifier.fillMaxSize()) {
                    // Create playlist button trigger
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Créer une playlist personnalisée")
                    }

                    if (playlists.isEmpty()) {
                        MusicEmptyState(message = "Aucune playlist personnalisée créée.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlists, key = { it.playlistId }) { playlist ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp)
                                        .clickable { viewModel.selectPlaylist(playlist) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.PlaylistPlay,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        IconButton(onClick = {
                                            viewModel.deletePlaylist(playlist.playlistId)
                                            Toast.makeText(context, "Playlist supprimée", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Supprimer",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Favorites subtab list
                val favoriteAudios = audios.filter { it.isFavorite }
                if (favoriteAudios.isEmpty()) {
                    MusicEmptyState(message = "Aucun titre audio en favoris.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(favoriteAudios, key = { _, audio -> audio.path }) { index, audio ->
                            AudioTrackRow(
                                audio = audio,
                                onClick = { AudioPlaybackManager.setPlaylist(favoriteAudios, index) },
                                onAddToPlaylist = { playlistId ->
                                    viewModel.addMediaToPlaylist(playlistId, audio.path, false, audio.title)
                                    Toast.makeText(context, "Ajouté à la playlist !", Toast.LENGTH_SHORT).show()
                                },
                                onToggleFavorite = {
                                    viewModel.toggleFavorite(audio.path, false, audio.title)
                                },
                                playlists = playlists,
                                onCreatePlaylist = { name ->
                                    viewModel.createPlaylist(name) { id ->
                                        viewModel.addMediaToPlaylist(id, audio.path, false, audio.title)
                                        Toast.makeText(context, "Playlist créée avec ce titre !", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Playlist Creation dialog
    if (showCreateDialog) {
        var playlistNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nouvelle playlist") },
            text = {
                TextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    placeholder = { Text("Ex: Ma Musique Zen") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            viewModel.createPlaylist(playlistNameInput)
                            showCreateDialog = false
                            Toast.makeText(context, "Playlist créée !", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Créer", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun AudioTrackRow(
    audio: AudioItem,
    onClick: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    playlists: List<PlaylistEntity>,
    onCreatePlaylist: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom Music Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Tracks info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${audio.artist ?: "Artiste inconnu"} • ${formatTime(audio.duration)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // Action menu icon
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Favoris") },
                leadingIcon = {
                    Icon(
                        imageVector = if (audio.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (audio.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    onToggleFavorite()
                    showMenu = false
                }
            )

            Divider()

            if (playlists.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Créer une playlist") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onCreatePlaylist("Ma Playlist")
                    }
                )
            } else {
                playlists.forEach { p ->
                    DropdownMenuItem(
                        text = { Text("Ajouter à ${p.name}") },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            onAddToPlaylist(p.playlistId)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(viewModel: MainViewModel, playlist: PlaylistEntity) {
    val items by viewModel.selectedPlaylistItems.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Detail top header navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aucun fichier dans cette playlist.\nRestez appuyé sur une vidéo ou un titre pour l'ajouter !",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val file = File(item.filePath)
                val exists = file.exists()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable(enabled = exists) {
                            if (item.isVideo) {
                                // play video
                                viewModel.playVideo(
                                    com.example.data.VideoItem(
                                        id = 0L,
                                        title = item.title,
                                        path = item.filePath,
                                        duration = 0L,
                                        size = file.length(),
                                        dateAdded = file.lastModified()
                                    )
                                )
                            } else {
                                // play audio track
                                AudioPlaybackManager.playTrack(
                                    AudioItem(
                                        id = 0L,
                                        title = item.title,
                                        path = item.filePath,
                                        duration = 0L,
                                        size = file.length(),
                                        artist = "Artiste",
                                        album = "Album",
                                        albumId = 0L,
                                        dateAdded = file.lastModified()
                                    )
                                )
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!exists) {
                            Text(
                                "Fichier introuvable",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.removeMediaFromPlaylist(playlist.playlistId, item.filePath)
                        Toast.makeText(context, "Supprimé de la playlist", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            Icons.Default.RemoveCircleOutline,
                            contentDescription = "Retirer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MusicEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MusicOff,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
