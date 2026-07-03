package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.VideoItem
import com.example.ui.formatTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideosTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val videos by viewModel.filteredVideos.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    if (videos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("videos_empty"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Aucune vidéo trouvée",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Déposez des fichiers vidéo (mp4, mkv, etc.) sur votre stockage interne pour les voir s'afficher.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .testTag("videos_tab"),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(videos, key = { it.path }) { video ->
            var showMenu by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { viewModel.playVideo(video) },
                        onLongClick = { showMenu = true }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Video thumbnail layer representation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = "Lire",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )

                        // Duration pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formatTime(video.duration),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Playback Resume Progress indicator (if exists)
                        if (video.progress > 0) {
                            val ratio = video.progress.toFloat() / video.duration.toFloat()
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }

                    // Metadata details section
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = video.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = formatSize(video.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Dropdown Options Menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Favoris") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (video.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            viewModel.toggleFavorite(video.path, true, video.title)
                            showMenu = false
                            Toast.makeText(
                                context,
                                if (video.isFavorite) "Retiré des favoris" else "Ajouté aux favoris",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    Divider()

                    if (playlists.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Créer une playlist") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.createPlaylist("Ma Playlist Vidéo") { id ->
                                    viewModel.addMediaToPlaylist(id, video.path, true, video.title)
                                    Toast.makeText(context, "Playlist créée avec cette vidéo !", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        playlists.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("Ajouter à ${p.name}") },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                                onClick = {
                                    viewModel.addMediaToPlaylist(p.playlistId, video.path, true, video.title)
                                    showMenu = false
                                    Toast.makeText(context, "Ajouté à la playlist !", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb > 1.0 -> String.format("%.2f Go", gb)
        mb > 1.0 -> String.format("%.2f Mo", mb)
        else -> String.format("%.2f Ko", kb)
    }
}
