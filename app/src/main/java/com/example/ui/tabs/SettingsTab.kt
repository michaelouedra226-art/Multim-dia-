package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.player.AudioPlaybackManager
import kotlinx.coroutines.launch

@Composable
fun SettingsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()

    var resumeVideoAutomatically by remember { mutableStateOf(true) }
    var autoPlayNextTrack by remember { mutableStateOf(true) }
    var useHardwareDecoding by remember { mutableStateOf(true) }
    var subtitleSize by remember { mutableStateOf("Moyen") }

    val crossfadeEnabled by AudioPlaybackManager.crossfadeEnabled.collectAsState()
    val sleepTimerRemaining by AudioPlaybackManager.sleepTimerRemaining.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_tab"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Info & Statistics Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "À propos de Nova Player",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Nova Player est un lecteur multimédia 100% local, ultra-rapide, respectueux de votre vie privée et sans publicité.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Vidéos indexées", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("${videos.size} fichiers", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("Musiques indexées", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("${audios.size} morceaux", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }

        // Help: Gestures Guide Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gesture, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Guide des gestes (Lecteur vidéo)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• Swipe vertical (gauche) : Luminosité", style = MaterialTheme.typography.bodySmall)
                    Text("• Swipe vertical (droite) : Volume", style = MaterialTheme.typography.bodySmall)
                    Text("• Double-tap gauche/droite : Reculer/Avancer de 10s", style = MaterialTheme.typography.bodySmall)
                    Text("• Appui long n'importe où : Accélérer la lecture (2.0x)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Section: Lecture Vidéo
        item {
            SettingsSectionHeader(title = "Lecture Vidéo")
        }

        item {
            SettingsToggleRow(
                title = "Reprise automatique",
                subtitle = "Reprendre la vidéo là où vous vous êtes arrêté",
                checked = resumeVideoAutomatically,
                onCheckedChange = { resumeVideoAutomatically = it }
            )
        }

        item {
            SettingsToggleRow(
                title = "Décodage matériel",
                subtitle = "Optimiser les performances en utilisant le GPU de l'appareil",
                checked = useHardwareDecoding,
                onCheckedChange = { useHardwareDecoding = it }
            )
        }

        // Section: Lecture Audio
        item {
            SettingsSectionHeader(title = "Lecture Audio")
        }

        item {
            SettingsToggleRow(
                title = "Enchaînement automatique",
                subtitle = "Lire le morceau suivant automatiquement",
                checked = autoPlayNextTrack,
                onCheckedChange = { autoPlayNextTrack = it }
            )
        }

        item {
            SettingsToggleRow(
                title = "Transition fluide (Crossfade)",
                subtitle = "Estomper la fin du morceau en lançant le suivant",
                checked = crossfadeEnabled,
                onCheckedChange = { AudioPlaybackManager.toggleCrossfade() }
            )
        }

        // Section: Minuteur
        item {
            SettingsSectionHeader(title = "Mise en veille")
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Minuteur de mise en veille", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (sleepTimerRemaining > 0) "Mise en veille active : ${sleepTimerRemaining / 1000 / 60} min restantes"
                        else "Suspendre la musique après une période donnée",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Button(
                    onClick = {
                        val currentMins = (sleepTimerRemaining / 1000 / 60).toInt()
                        val nextMins = when (currentMins) {
                            0 -> 15
                            15 -> 30
                            30 -> 45
                            45 -> 60
                            else -> 0
                        }
                        AudioPlaybackManager.startSleepTimer(nextMins)
                        Toast.makeText(
                            context,
                            if (nextMins == 0) "Minuteur désactivé" else "Veille programmée dans $nextMins min",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sleepTimerRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = if (sleepTimerRemaining > 0) "Activé" else "Configurer",
                        color = Color.Black,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Section: Réinitialisation
        item {
            SettingsSectionHeader(title = "Données & Sécurité")
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Effacer l'historique de lecture", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Réinitialiser les positions de reprise de toutes les vidéos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.history.value.forEach {
                                viewModel.repository.deletePlaybackPosition(it.filePath)
                            }
                            Toast.makeText(context, "Historique effacé", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Effacer", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
