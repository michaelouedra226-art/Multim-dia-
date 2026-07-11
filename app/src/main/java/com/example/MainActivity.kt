package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.MediaDatabase
import com.example.data.MediaRepository
import com.example.ui.AudioPlayerOverlay
import com.example.ui.VideoPlayerScreen
import com.example.ui.Html5VideoPlayerScreen
import com.example.ui.tabs.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    // Permission launcher to scan storage
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val videoGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
        } else false

        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] ?: false
        } else false

        val oldStorageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

        if (videoGranted || audioGranted || oldStorageGranted) {
            viewModel.scanMedia(this)
        } else {
            Toast.makeText(this, "Permission requise pour indexer vos fichiers.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init repository & database
        val database = MediaDatabase.getDatabase(applicationContext)
        val repository = MediaRepository(database.mediaDao())
        viewModel = MainViewModel(repository)

        // Initial check and auto-scan if permissions are already given
        if (hasStoragePermission()) {
            viewModel.scanMedia(this)
        }

        setContent {
            MyApplicationTheme {
                MainAppHost(
                    viewModel = viewModel,
                    onRequestScan = { triggerPermissionRequest() }
                )
            }
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun triggerPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppHost(
    viewModel: MainViewModel,
    onRequestScan: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var currentTab by remember { mutableStateOf(0) } // 0: Accueil, 1: Vidéos, 2: Musique, 3: Dossiers, 4: Recherche, 5: Paramètres
    val activeVideo by viewModel.activeVideo.collectAsState()

    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()

    // Smart Back Button implementation
    var lastBackPressTime by remember { mutableStateOf(0L) }
    BackHandler(enabled = activeVideo == null) {
        if (selectedFolder != null) {
            viewModel.selectFolder(null)
        } else if (selectedPlaylist != null) {
            viewModel.selectPlaylist(null)
        } else if (currentTab != 0) {
            currentTab = 0 // Navigate back to Accueil
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                activity?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "Appuyez à nouveau pour quitter", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            if (activeVideo == null) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                0 -> "Nova Player"
                                1 -> "Vidéos"
                                2 -> "Musique"
                                3 -> "Dossiers"
                                4 -> "Recherche"
                                else -> "Paramètres"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (activeVideo == null) {
                Column {
                    // Floating audio player bottom bar (if playing)
                    AudioPlayerOverlay(viewModel = viewModel)

                    // Navigation Bar (Material 3)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = { Icon(if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Accueil") },
                            label = { Text("Accueil", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Icon(if (currentTab == 1) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary, contentDescription = "Vidéos") },
                            label = { Text("Vidéos", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_videos")
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Icon(if (currentTab == 2) Icons.Filled.MusicNote else Icons.Outlined.MusicNote, contentDescription = "Musique") },
                            label = { Text("Musique", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_music")
                        )
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            icon = { Icon(if (currentTab == 3) Icons.Filled.Folder else Icons.Outlined.Folder, contentDescription = "Dossiers") },
                            label = { Text("Dossiers", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_folders")
                        )
                        NavigationBarItem(
                            selected = currentTab == 4,
                            onClick = { currentTab = 4 },
                            icon = { Icon(if (currentTab == 4) Icons.Filled.Search else Icons.Outlined.Search, contentDescription = "Recherche") },
                            label = { Text("Recherche", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_search")
                        )
                        NavigationBarItem(
                            selected = currentTab == 5,
                            onClick = { currentTab = 5 },
                            icon = { Icon(if (currentTab == 5) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Paramètres") },
                            label = { Text("Paramètres", fontSize = 10.sp) },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router transitions
            when (currentTab) {
                0 -> AccueilTab(
                    viewModel = viewModel,
                    onNavigateToTab = { currentTab = it },
                    onRequestScan = onRequestScan
                )
                1 -> VideosTab(viewModel = viewModel)
                2 -> MusicTab(viewModel = viewModel)
                3 -> FoldersTab(viewModel = viewModel)
                4 -> SearchTab(viewModel = viewModel)
                5 -> SettingsTab(viewModel = viewModel)
            }
        }
    }

    val useHtml5Player by viewModel.useHtml5Player.collectAsState()

    // Fullscreen Overlay Video Player Renderer (If active)
    if (activeVideo != null) {
        if (useHtml5Player) {
            Html5VideoPlayerScreen(
                video = activeVideo!!,
                viewModel = viewModel,
                onDismiss = { viewModel.stopVideo() }
            )
        } else {
            VideoPlayerScreen(
                video = activeVideo!!,
                viewModel = viewModel,
                onDismiss = { viewModel.stopVideo() }
            )
        }
    }
}
