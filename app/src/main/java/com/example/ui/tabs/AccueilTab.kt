package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.AudioItem
import com.example.data.VideoItem
import com.example.player.AudioPlaybackManager
import com.example.ui.formatTime
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AccueilTab(
    viewModel: MainViewModel,
    onNavigateToTab: (Int) -> Unit,
    onRequestScan: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()

    val totalMediaCount = videos.size + audios.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("accueil_tab"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Status Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nova Player",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isScanning) "Analyse du stockage en cours..."
                            else "Bibliothèque : $totalMediaCount éléments locaux",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(
                            onClick = onRequestScan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Scan",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (totalMediaCount == 0 && !isScanning) {
            // Immersive Empty State (Mandated: "Bibliothèque vide au premier lancement")
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucun fichier multimédia",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nova Player scanne vos vidéos et musiques locales de manière sécurisée et 100% privée, sans publicité.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onRequestScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyser mon stockage", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Recently Played Section (History)
            if (history.isNotEmpty()) {
                item {
                    Text(
                        text = "Récents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(history.take(3)) { record ->
                    val isVideo = record.filePath.endsWith(".mp4", ignoreCase = true) ||
                            record.filePath.endsWith(".mkv", ignoreCase = true) ||
                            record.filePath.endsWith(".avi", ignoreCase = true)

                    val file = File(record.filePath)
                    if (file.exists()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable {
                                    if (isVideo) {
                                        viewModel.playVideo(
                                            VideoItem(
                                                id = 0L,
                                                title = file.name,
                                                path = record.filePath,
                                                duration = record.duration,
                                                size = file.length(),
                                                dateAdded = file.lastModified(),
                                                progress = record.position
                                            )
                                        )
                                    } else {
                                        // Play audio
                                        AudioPlaybackManager.playTrack(
                                            AudioItem(
                                                id = 0L,
                                                title = file.name,
                                                path = record.filePath,
                                                duration = record.duration,
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
                                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Reprendre à ${formatTime(record.position)} / ${formatTime(record.duration)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    viewModel.repository.deletePlaybackPosition(record.filePath)
                                    Toast.makeText(context, "Supprimé des récents", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Favorites quick overview
            if (favorites.isNotEmpty()) {
                item {
                    Text(
                        text = "Favoris",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(favorites.take(3)) { fav ->
                    val file = File(fav.filePath)
                    if (file.exists()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable {
                                    if (fav.isVideo) {
                                        viewModel.playVideo(
                                            VideoItem(
                                                id = 0L,
                                                title = fav.title,
                                                path = fav.filePath,
                                                duration = 0L,
                                                size = file.length(),
                                                dateAdded = file.lastModified(),
                                                isFavorite = true
                                            )
                                        )
                                    } else {
                                        AudioPlaybackManager.playTrack(
                                            AudioItem(
                                                id = 0L,
                                                title = fav.title,
                                                path = fav.filePath,
                                                duration = 0L,
                                                size = file.length(),
                                                artist = "Artiste",
                                                album = "Album",
                                                albumId = 0L,
                                                dateAdded = file.lastModified(),
                                                isFavorite = true
                                            )
                                        )
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (fav.isVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = fav.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                viewModel.toggleFavorite(fav.filePath, fav.isVideo, fav.title)
                            }) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "Favori",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }

            // Category cards layout (Videos, Musics)
            item {
                Text(
                    text = "Catégories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Videos category card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { onNavigateToTab(1) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    "Vidéos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${videos.size} fichiers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Music category card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { onNavigateToTab(2) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                Icons.Default.MusicVideo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    "Musique",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${audios.size} morceaux",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
