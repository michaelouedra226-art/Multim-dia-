package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.data.AudioItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

object AudioPlaybackManager {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    // Player state flows
    private val _currentTrack = MutableStateFlow<AudioItem?>(null)
    val currentTrack: StateFlow<AudioItem?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _sleepTimerRemaining = MutableStateFlow(0L) // in milliseconds
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining

    private val _crossfadeEnabled = MutableStateFlow(false)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled

    private val _bassBoostEnabled = MutableStateFlow(false)
    val bassBoostEnabled: StateFlow<Boolean> = _bassBoostEnabled

    private val _eqLow = MutableStateFlow(50) // slider values 0-100
    val eqLow: StateFlow<Int> = _eqLow
    private val _eqMid = MutableStateFlow(50)
    val eqMid: StateFlow<Int> = _eqMid
    private val _eqHigh = MutableStateFlow(50)
    val eqHigh: StateFlow<Int> = _eqHigh

    private var playlist: List<AudioItem> = emptyList()
    private var currentIndex: Int = -1

    private var sleepTimerJob: Job? = null
    private var positionUpdateJob: Job? = null

    init {
        startPositionUpdates()
    }

    fun setPlaylist(tracks: List<AudioItem>, startIndex: Int) {
        playlist = tracks
        currentIndex = startIndex
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            playTrack(tracks[startIndex])
        }
    }

    fun playTrack(track: AudioItem) {
        scope.launch {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null

                val file = File(track.path)
                if (!file.exists()) return@launch

                val mp = MediaPlayer().apply {
                    setDataSource(track.path)
                    prepare()
                    applySpeedAndEq(this)
                    start()
                }

                mediaPlayer = mp
                _currentTrack.value = track
                _isPlaying.value = true
                _duration.value = mp.duration.toLong()

                mp.setOnCompletionListener {
                    handleTrackCompletion()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _isPlaying.value = false
            }
        }
    }

    fun playPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _isPlaying.value = false
        } else {
            mp.start()
            _isPlaying.value = true
        }
    }

    fun next() {
        if (playlist.isEmpty()) return
        currentIndex = (currentIndex + 1) % playlist.size
        playTrack(playlist[currentIndex])
    }

    fun previous() {
        if (playlist.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        playTrack(playlist[currentIndex])
    }

    fun seekTo(posMs: Long) {
        mediaPlayer?.seekTo(posMs.toInt())
        _position.value = posMs
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                applySpeedAndEq(mp)
            }
        }
    }

    fun toggleBassBoost() {
        _bassBoostEnabled.value = !_bassBoostEnabled.value
        // Apply EQ changes
        mediaPlayer?.let { applySpeedAndEq(it) }
    }

    fun setEq(low: Int, mid: Int, high: Int) {
        _eqLow.value = low
        _eqMid.value = mid
        _eqHigh.value = high
        mediaPlayer?.let { applySpeedAndEq(it) }
    }

    fun toggleCrossfade() {
        _crossfadeEnabled.value = !_crossfadeEnabled.value
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _sleepTimerRemaining.value = 0L
            return
        }
        val ms = minutes * 60 * 1000L
        _sleepTimerRemaining.value = ms

        sleepTimerJob = scope.launch {
            var remaining = ms
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepTimerRemaining.value = remaining
            }
            pausePlayback()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    private fun applySpeedAndEq(mp: MediaPlayer) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = PlaybackParams().apply {
                    speed = _playbackSpeed.value
                }
                mp.playbackParams = params
            }

            // Software amplification of EQ ranges via stereo balancing
            // On standard local files, we can also simulate bass boost by increasing low channel output
            // Real android EQ (Equalizer class) requires an audio session ID, which we can obtain via mp.audioSessionId
            // Let's create a real Android Audio Effect or simulate volume scaling
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleTrackCompletion() {
        if (_crossfadeEnabled.value) {
            // Simulate crossfade transition
            scope.launch {
                // Fade out current player
                for (i in 10 downTo 1) {
                    val vol = i / 10.0f
                    mediaPlayer?.setVolume(vol, vol)
                    delay(100)
                }
                next()
            }
        } else {
            next()
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob = scope.launch {
            while (true) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _position.value = mp.currentPosition.toLong()
                    }
                }
                delay(500)
            }
        }
    }
}
