package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.MainViewModel
import com.example.data.VideoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.min

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    // Lock screen to Landscape for immersive play
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Audio & Brightness system managers
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    val window = activity?.window
    var currentBrightness by remember {
        mutableStateOf(
            window?.attributes?.screenBrightness?.let { if (it < 0) 0.5f else it } ?: 0.5f
        )
    }

    // Media Player states
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(1L) }
    var position by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var showControls by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }

    // Gesture indicator overlays
    var gestureType by remember { mutableStateOf("") } // "VOLUME", "BRIGHTNESS", "SEEK", "SPEED"
    var gestureValue by remember { mutableStateOf(0) } // percentage or speed

    // Initialize MediaPlayer
    val mediaPlayer = remember { MediaPlayer() }
    var isPrepared by remember { mutableStateOf(false) }

    LaunchedEffect(video) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, Uri.fromFile(File(video.path)))
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener { mp ->
                isPrepared = true
                duration = mp.duration.toLong()
                // Auto resume
                val startProgress = video.progress
                if (startProgress > 0 && startProgress < duration) {
                    mp.seekTo(startProgress.toInt())
                }
                mp.start()
                isPlaying = true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur de lecture de la vidéo", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    // Position updates tracking
    LaunchedEffect(isPlaying, isPrepared) {
        while (isPlaying && isPrepared) {
            try {
                position = mediaPlayer.currentPosition.toLong()
                // Save progress periodically
                viewModel.saveVideoProgress(video.path, position, duration)
            } catch (e: Exception) {
                // ignore
            }
            delay(1000)
        }
    }

    // Hide controls after timeout
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    // Capture standard android back press
    BackHandler {
        if (isPrepared) {
            viewModel.saveVideoProgress(video.path, mediaPlayer.currentPosition.toLong(), duration)
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_container")
    ) {
        // Video View Renderer
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            try {
                                mediaPlayer.setDisplay(holder)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            mediaPlayer.setDisplay(null)
                        }

                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Overlay Layer (Volume, Brightness, Seek, Speed Acceleration)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isScreenLocked) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (isScreenLocked) return@detectTapGestures
                            val halfWidth = size.width / 2
                            if (offset.x < halfWidth) {
                                // Seek back 10s
                                val newPos = max(0L, position - 10000L)
                                mediaPlayer.seekTo(newPos.toInt())
                                position = newPos
                                gestureType = "SEEK"
                                gestureValue = -10
                                coroutineScope.launch {
                                    delay(800)
                                    gestureType = ""
                                }
                            } else {
                                // Seek forward 10s
                                val newPos = min(duration, position + 10000L)
                                mediaPlayer.seekTo(newPos.toInt())
                                position = newPos
                                gestureType = "SEEK"
                                gestureValue = 10
                                coroutineScope.launch {
                                    delay(800)
                                    gestureType = ""
                                }
                            }
                        },
                        onLongPress = {
                            if (isScreenLocked) return@detectTapGestures
                            // Fast forward 2.0x acceleration on hold
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    val params = mediaPlayer.playbackParams
                                    params.speed = 2.0f
                                    mediaPlayer.playbackParams = params
                                    gestureType = "SPEED"
                                    gestureValue = 200
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        onPress = {
                            try {
                                awaitRelease()
                                // Revert to normal speed upon releasing long-press
                                if (gestureType == "SPEED" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val params = mediaPlayer.playbackParams
                                    params.speed = playbackSpeed
                                    mediaPlayer.playbackParams = params
                                    gestureType = ""
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        },
                        onTap = {
                            showControls = !showControls
                        }
                    )
                }
                .pointerInput(isScreenLocked) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            if (isScreenLocked) return@detectDragGestures
                            val halfWidth = size.width / 2
                            if (change.position.x < halfWidth) {
                                // Brightness adjust (left side)
                                val delta = -dragAmount.y / size.height.toFloat()
                                currentBrightness = min(1.0f, max(0.01f, currentBrightness + delta))
                                window?.let {
                                    val lp = it.attributes
                                    lp.screenBrightness = currentBrightness
                                    it.attributes = lp
                                }
                                gestureType = "BRIGHTNESS"
                                gestureValue = (currentBrightness * 100).toInt()
                            } else {
                                // Volume adjust (right side)
                                val delta = -dragAmount.y / size.height.toFloat()
                                val volDelta = (delta * maxVolume).toInt()
                                if (volDelta != 0) {
                                    currentVolume = min(maxVolume, max(0, currentVolume + volDelta))
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                                    gestureType = "VOLUME"
                                    gestureValue = (currentVolume * 100 / maxVolume)
                                }
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                delay(800)
                                gestureType = ""
                            }
                        }
                    )
                }
        )

        // Overlay status indicators (Brightness / Volume / Seek / Speed)
        AnimatedVisibility(
            visible = gestureType.isNotEmpty(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (gestureType) {
                            "VOLUME" -> if (gestureValue == 0) Icons.Default.VolumeMute else Icons.Default.VolumeUp
                            "BRIGHTNESS" -> Icons.Default.BrightnessMedium
                            "SEEK" -> if (gestureValue > 0) Icons.Default.FastForward else Icons.Default.FastRewind
                            else -> Icons.Default.Speed
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (gestureType) {
                            "VOLUME" -> "Volume: $gestureValue%"
                            "BRIGHTNESS" -> "Luminosité: $gestureValue%"
                            "SEEK" -> if (gestureValue > 0) "+10s" else "-10s"
                            else -> "Vitesse: 2.0x (Maintien)"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Top and Bottom control bars
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (isPrepared) {
                        viewModel.saveVideoProgress(video.path, mediaPlayer.currentPosition.toLong(), duration)
                        mediaPlayer.stop()
                        mediaPlayer.release()
                    }
                    onDismiss()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = video.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                if (!isScreenLocked) {
                    // Screenshot button (Simulated)
                    IconButton(onClick = {
                        Toast.makeText(context, "Capture d'écran enregistrée", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.White)
                    }

                    // Favorite button
                    IconButton(onClick = {
                        viewModel.toggleFavorite(video.path, true, video.title)
                        Toast.makeText(
                            context,
                            if (video.isFavorite) "Retiré des favoris" else "Ajouté aux favoris",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (video.isFavorite) Color.Red else Color.White
                        )
                    }
                }

                // Lock button
                IconButton(onClick = { isScreenLocked = !isScreenLocked }) {
                    Icon(
                        imageVector = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock",
                        tint = if (isScreenLocked) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }
        }

        // Bottom playback controls
        AnimatedVisibility(
            visible = showControls && !isScreenLocked,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                // Progress Bar & Durations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(position),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = position.toFloat(),
                        onValueChange = {
                            mediaPlayer.seekTo(it.toInt())
                            position = it.toLong()
                        },
                        valueRange = 0f..max(1f, duration.toFloat()),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Text(
                        text = formatTime(duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Playback speed selector button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable {
                                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)
                                val nextIndex = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                                playbackSpeed = speeds[nextIndex]
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    try {
                                        val params = mediaPlayer.playbackParams
                                        params.speed = playbackSpeed
                                        mediaPlayer.playbackParams = params
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${playbackSpeed}x", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }

                    // Main controls (Rewind, Play/Pause, FastForward)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val newPos = max(0L, position - 10000L)
                            mediaPlayer.seekTo(newPos.toInt())
                            position = newPos
                        }) {
                            Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = {
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.pause()
                                    isPlaying = false
                                } else {
                                    mediaPlayer.start()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
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

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(onClick = {
                            val newPos = min(duration, position + 10000L)
                            mediaPlayer.seekTo(newPos.toInt())
                            position = newPos
                        }) {
                            Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    // Aspect ratio or Picture-in-Picture selector button
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            try {
                                activity?.enterPictureInPictureMode()
                            } catch (e: Exception) {
                                Toast.makeText(context, "PiP indisponible", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "PiP requiert Android 8+", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = Color.White)
                    }
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    val hr = (ms / (1000 * 60 * 60))
    return if (hr > 0) {
        String.format("%d:%02d:%02d", hr, min, sec)
    } else {
        String.format("%02d:%02d", min, sec)
    }
}
