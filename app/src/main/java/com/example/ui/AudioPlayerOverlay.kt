package com.example.ui

import android.content.Context
import android.media.audiofx.BassBoost
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.player.AudioPlaybackManager
import kotlinx.coroutines.launch

@Composable
fun AudioPlayerOverlay(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTrack by AudioPlaybackManager.currentTrack.collectAsState()
    val isPlaying by AudioPlaybackManager.isPlaying.collectAsState()
    val position by AudioPlaybackManager.position.collectAsState()
    val duration by AudioPlaybackManager.duration.collectAsState()

    var showFullDeck by remember { mutableStateOf(false) }

    if (currentTrack == null) return

    Box(modifier = modifier) {
        if (!showFullDeck) {
            // Compact Floating Player Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { showFullDeck = true }
                    .testTag("compact_audio_bar"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom vector icon mimicking album art
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title / Artist Info
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = currentTrack!!.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentTrack!!.artist ?: "Artiste inconnu",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Play/Pause Action
                        IconButton(onClick = { AudioPlaybackManager.playPause() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Next Action
                        IconButton(onClick = { AudioPlaybackManager.next() }) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Progress bar
                    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        } else {
            // Full Screen Deck Panel Modal
            FullScreenAudioDeck(
                viewModel = viewModel,
                onDismiss = { showFullDeck = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenAudioDeck(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentTrack by AudioPlaybackManager.currentTrack.collectAsState()
    val isPlaying by AudioPlaybackManager.isPlaying.collectAsState()
    val position by AudioPlaybackManager.position.collectAsState()
    val duration by AudioPlaybackManager.duration.collectAsState()
    val playbackSpeed by AudioPlaybackManager.playbackSpeed.collectAsState()
    val sleepTimer by AudioPlaybackManager.sleepTimerRemaining.collectAsState()
    val crossfade by AudioPlaybackManager.crossfadeEnabled.collectAsState()
    val bassBoost by AudioPlaybackManager.bassBoostEnabled.collectAsState()

    val eqLow by AudioPlaybackManager.eqLow.collectAsState()
    val eqMid by AudioPlaybackManager.eqMid.collectAsState()
    val eqHigh by AudioPlaybackManager.eqHigh.collectAsState()

    var showTimerDialog by remember { mutableStateOf(false) }

    if (currentTrack == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lecture en cours", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize")
                    }
                },
                actions = {
                    // Favorite Toggle
                    IconButton(onClick = {
                        viewModel.toggleFavorite(currentTrack!!.path, false, currentTrack!!.title)
                        Toast.makeText(
                            context,
                            if (currentTrack!!.isFavorite) "Retiré des favoris" else "Ajouté aux favoris",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            imageVector = if (currentTrack!!.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (currentTrack!!.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize().testTag("full_audio_deck")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Album Art Hero Section
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(96.dp)
                    )
                }
            }

            // Info Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTrack!!.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTrack!!.artist ?: "Artiste inconnu",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar & Sliders
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = position.toFloat(),
                    onValueChange = { AudioPlaybackManager.seekTo(it.toLong()) },
                    valueRange = 0f..maxOf(1f, duration.toFloat()),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(position),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main playback controls (Shuffle, Prev, Play, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed control
                IconButton(onClick = {
                    val nextSpeed = when (playbackSpeed) {
                        0.5f -> 0.75f
                        0.75f -> 1.0f
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        else -> 0.5f
                    }
                    AudioPlaybackManager.setSpeed(nextSpeed)
                }) {
                    Text(
                        text = "${playbackSpeed}x",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }

                IconButton(onClick = { AudioPlaybackManager.previous() }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Prev",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = { AudioPlaybackManager.playPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { AudioPlaybackManager.next() }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Sleep Timer icon trigger
                IconButton(onClick = { showTimerDialog = true }) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimer > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-control dashboard (Equalizer sliders, crossfade, and Bass Boost toggle)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Effets & Égaliseur",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bass Boost & Crossfade toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bass Boost", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = bassBoost,
                                onCheckedChange = { AudioPlaybackManager.toggleBassBoost() },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Transition (Crossfade)", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = crossfade,
                                onCheckedChange = { AudioPlaybackManager.toggleCrossfade() },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated 3-Band Equalizer (Low, Mid, High)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Grave (Low)", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = eqLow.toFloat(),
                                onValueChange = { AudioPlaybackManager.setEq(it.toInt(), eqMid, eqHigh) },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Médium (Mid)", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = eqMid.toFloat(),
                                onValueChange = { AudioPlaybackManager.setEq(eqLow, it.toInt(), eqHigh) },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Aigu (High)", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = eqHigh.toFloat(),
                                onValueChange = { AudioPlaybackManager.setEq(eqLow, eqMid, it.toInt()) },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Timer dialog
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("Minuteur de mise en veille") },
            text = {
                Column {
                    Text("Choisissez la durée après laquelle suspendre la lecture :")
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(0, 5, 15, 30, 45, 60).forEach { mins ->
                        val label = if (mins == 0) "Désactiver" else "$mins minutes"
                        Button(
                            onClick = {
                                AudioPlaybackManager.startSleepTimer(mins)
                                showTimerDialog = false
                                Toast.makeText(
                                    context,
                                    if (mins == 0) "Minuteur désactivé" else "Veille programmée dans $mins min",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mins == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(label, color = Color.Black)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
