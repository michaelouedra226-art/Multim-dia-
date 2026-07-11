package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.MainViewModel
import com.example.data.VideoItem
import java.io.File

@Composable
fun Html5VideoPlayerScreen(
    video: VideoItem,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Lock screen to Landscape for immersive HTML5 play
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Keep track of current playback progress
    var lastProgressMs by remember { mutableStateOf(video.progress) }

    // Javascript interface to bridge HTML5 events to our Kotlin ViewModel
    val webAppInterface = remember {
        object {
            @JavascriptInterface
            fun onProgress(positionMs: Long) {
                lastProgressMs = positionMs
                viewModel.saveVideoProgress(video.path, positionMs, video.duration)
            }

            @JavascriptInterface
            fun onEnded() {
                viewModel.saveVideoProgress(video.path, video.duration, video.duration)
                activity?.runOnUiThread {
                    onDismiss()
                }
            }

            @JavascriptInterface
            fun closePlayer() {
                viewModel.saveVideoProgress(video.path, lastProgressMs, video.duration)
                activity?.runOnUiThread {
                    onDismiss()
                }
            }

            @JavascriptInterface
            fun showError(message: String) {
                activity?.runOnUiThread {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Handles native android back button press
    BackHandler {
        viewModel.saveVideoProgress(video.path, lastProgressMs, video.duration)
        onDismiss()
    }

    // Generate responsive HTML5 Player page with Glassmorphism controls
    val htmlContent = remember(video) {
        getHtml5PlayerSource()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("html5_video_player_container")
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Enable hardware acceleration
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    // Configure security & playback settings
                    settings.apply {
                        javaScriptEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    webChromeClient = object : WebChromeClient() {
                        // Support HTML5 fullscreen requests if any
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Once page is loaded, pass the actual local media file URI, title, and last progress position
                            val escapedPath = Uri.fromFile(File(video.path)).toString()
                            val escapedTitle = video.title.replace("'", "\\'")
                            view?.evaluateJavascript(
                                "loadVideo('$escapedPath', '$escapedTitle', $lastProgressMs)",
                                null
                            )
                        }
                    }

                    addJavascriptInterface(webAppInterface, "AndroidPlayer")
                    loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { webView ->
                // Keep updated if needed
            }
        )
    }
}

/**
 * Returns a highly polished, interactive HTML5 video player codebase.
 * Designed with Glassmorphic visual principles, responsive layouts,
 * interactive progress bars, and custom audio controls.
 */
private fun getHtml5PlayerSource(): String {
    return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>Lecteur Multimédia Nova Player</title>
            <style>
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                    user-select: none;
                    -webkit-user-select: none;
                }
                body, html {
                    width: 100%;
                    height: 100%;
                    background-color: #000;
                    overflow: hidden;
                    font-family: system-ui, -apple-system, sans-serif;
                    color: #fff;
                }
                .player-container {
                    position: relative;
                    width: 100%;
                    height: 100%;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    background: #000;
                }
                video {
                    width: 100%;
                    height: 100%;
                    object-fit: contain;
                    outline: none;
                }
                
                /* Controls Overlay with top/bottom smooth ambient fade */
                .controls-overlay {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    background: linear-gradient(to bottom, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0) 25%, rgba(0,0,0,0) 75%, rgba(0,0,0,0.85) 100%);
                    opacity: 1;
                    transition: opacity 0.35s cubic-bezier(0.4, 0, 0.2, 1);
                    z-index: 10;
                }
                .controls-overlay.hidden {
                    opacity: 0;
                    pointer-events: none;
                }
                
                /* Top Header bar */
                .top-bar {
                    padding: 20px 24px;
                    display: flex;
                    align-items: center;
                    gap: 16px;
                }
                .back-btn {
                    background: rgba(255, 255, 255, 0.08);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    color: white;
                    cursor: pointer;
                    padding: 10px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 50%;
                    transition: background 0.2s, transform 0.1s;
                }
                .back-btn:active {
                    background: rgba(255, 255, 255, 0.2);
                    transform: scale(0.95);
                }
                .back-btn svg {
                    width: 20px;
                    height: 20px;
                    fill: currentColor;
                }
                .video-title {
                    font-size: 18px;
                    font-weight: 600;
                    letter-spacing: 0.3px;
                    text-shadow: 0 2px 8px rgba(0,0,0,0.8);
                    flex-grow: 1;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
                
                /* Lock button to lock controls overlay state */
                .lock-btn {
                    background: rgba(255, 255, 255, 0.08);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    color: white;
                    cursor: pointer;
                    padding: 10px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    transition: all 0.2s;
                }
                .lock-btn.locked {
                    background: #D0BCFF;
                    color: #000;
                    border-color: #D0BCFF;
                }
                
                /* Centered quick-control actions */
                .center-controls {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 40px;
                }
                .center-btn {
                    background: rgba(25, 20, 35, 0.6);
                    border: 1px solid rgba(255, 255, 255, 0.15);
                    color: white;
                    border-radius: 50%;
                    width: 56px;
                    height: 56px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    cursor: pointer;
                    transition: transform 0.2s, background 0.2s;
                    backdrop-filter: blur(12px);
                }
                .center-btn:active {
                    transform: scale(0.9);
                    background: rgba(255, 255, 255, 0.25);
                }
                .play-pause-btn {
                    width: 76px;
                    height: 76px;
                    background: #D0BCFF;
                    color: #121016;
                    border: none;
                    box-shadow: 0 4px 20px rgba(208, 188, 255, 0.3);
                }
                .play-pause-btn:active {
                    background: #E8DEF8;
                    transform: scale(0.92);
                }
                
                /* Glassmorphic Bottom Controls Panel */
                .bottom-panel {
                    margin: 16px;
                    padding: 16px 20px;
                    background: rgba(20, 18, 24, 0.65);
                    backdrop-filter: blur(20px);
                    -webkit-backdrop-filter: blur(20px);
                    border: 1px solid rgba(255, 255, 255, 0.08);
                    border-radius: 16px;
                    display: flex;
                    flex-direction: column;
                    gap: 12px;
                    box-shadow: 0 8px 32px rgba(0,0,0,0.5);
                }
                
                /* Seekbar slider styling */
                .seekbar-container {
                    display: flex;
                    align-items: center;
                    width: 100%;
                    gap: 14px;
                }
                .time-label {
                    font-size: 12px;
                    font-weight: 500;
                    min-width: 44px;
                    color: rgba(255, 255, 255, 0.8);
                    font-variant-numeric: tabular-nums;
                }
                .seekbar-wrapper {
                    position: relative;
                    flex-grow: 1;
                    height: 20px;
                    display: flex;
                    align-items: center;
                    cursor: pointer;
                }
                .seekbar-track {
                    position: absolute;
                    width: 100%;
                    height: 5px;
                    background: rgba(255, 255, 255, 0.15);
                    border-radius: 3px;
                }
                .seekbar-fill {
                    position: absolute;
                    height: 5px;
                    background: #D0BCFF;
                    border-radius: 3px;
                    width: 0%;
                }
                .seekbar-thumb {
                    position: absolute;
                    width: 14px;
                    height: 14px;
                    background: #D0BCFF;
                    border: 2px solid #fff;
                    border-radius: 50%;
                    left: 0%;
                    transform: translateX(-50%);
                    box-shadow: 0 2px 6px rgba(0,0,0,0.4);
                    transition: transform 0.15s;
                }
                .seekbar-wrapper:active .seekbar-thumb {
                    transform: translateX(-50%) scale(1.3);
                }
                .seekbar-input {
                    position: absolute;
                    width: 100%;
                    height: 100%;
                    opacity: 0;
                    cursor: pointer;
                    z-index: 5;
                }
                
                /* Secondary Control Actions Row */
                .actions-row {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                }
                .left-actions, .right-actions {
                    display: flex;
                    align-items: center;
                    gap: 20px;
                }
                
                .btn-icon {
                    background: none;
                    border: none;
                    color: rgba(255, 255, 255, 0.85);
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 8px;
                    border-radius: 50%;
                    transition: all 0.2s;
                }
                .btn-icon:active {
                    background: rgba(255, 255, 255, 0.1);
                    color: white;
                }
                .btn-icon svg {
                    width: 22px;
                    height: 22px;
                    fill: currentColor;
                }
                
                /* Volume interactive control */
                .volume-control {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }
                .volume-slider {
                    width: 84px;
                    height: 4px;
                    -webkit-appearance: none;
                    background: rgba(255, 255, 255, 0.2);
                    border-radius: 2px;
                    outline: none;
                }
                .volume-slider::-webkit-slider-thumb {
                    -webkit-appearance: none;
                    width: 12px;
                    height: 12px;
                    border-radius: 50%;
                    background: #D0BCFF;
                    cursor: pointer;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.4);
                }
                
                /* Custom Dropdown Styling */
                .speed-menu {
                    background: rgba(255, 255, 255, 0.08);
                    border: 1px solid rgba(255, 255, 255, 0.12);
                    color: white;
                    padding: 6px 12px;
                    border-radius: 8px;
                    font-size: 13px;
                    font-weight: 600;
                    outline: none;
                    cursor: pointer;
                    transition: background 0.2s;
                }
                .speed-menu:active {
                    background: rgba(255, 255, 255, 0.15);
                }
                .speed-menu option {
                    background: #141218;
                    color: #fff;
                }
                
                /* Gestures/Doubletap responsive indicators */
                .feedback-pill {
                    position: absolute;
                    top: 50%;
                    transform: translateY(-50%);
                    width: 25%;
                    height: 50%;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    background: radial-gradient(circle, rgba(208,188,255,0.18) 0%, rgba(0,0,0,0) 70%);
                    border-radius: 50%;
                    opacity: 0;
                    pointer-events: none;
                    z-index: 5;
                }
                .feedback-pill.left { left: 8%; }
                .feedback-pill.right { right: 8%; }
                .feedback-pill.active {
                    animation: pulseEffect 0.5s cubic-bezier(0.1, 0.8, 0.3, 1) forwards;
                }
                @keyframes pulseEffect {
                    0% { opacity: 0; transform: translateY(-50%) scale(0.85); }
                    50% { opacity: 1; }
                    100% { opacity: 0; transform: translateY(-50%) scale(1.15); }
                }
                .feedback-label {
                    font-size: 13px;
                    font-weight: 700;
                    color: #D0BCFF;
                    margin-top: 6px;
                    text-shadow: 0 1px 4px rgba(0,0,0,0.5);
                }
                
                /* Toast overlay styling */
                .toast-indicator {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -50%);
                    background: rgba(20, 18, 24, 0.9);
                    border: 1px solid rgba(255,255,255,0.1);
                    border-radius: 12px;
                    padding: 14px 20px;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    gap: 6px;
                    opacity: 0;
                    pointer-events: none;
                    transition: opacity 0.25s ease;
                    z-index: 30;
                }
                .toast-indicator.active {
                    opacity: 1;
                }
                .toast-title {
                    font-size: 12px;
                    font-weight: 500;
                    color: rgba(255,255,255,0.6);
                }
                .toast-value {
                    font-size: 16px;
                    font-weight: 800;
                    color: #D0BCFF;
                }
            </style>
        </head>
        <body>
            <div class="player-container" id="container">
                <video id="videoPlayer" playsinline></video>
                
                <!-- HTML5 Modern Control Overlays -->
                <div class="controls-overlay" id="overlay">
                    <div class="top-bar">
                        <button class="back-btn" onclick="onBackBtn()">
                            <svg viewBox="0 0 24 24"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                        </button>
                        <div class="video-title" id="title">Nova Player HTML5</div>
                        <button class="lock-btn" id="lockBtn" onclick="toggleLock(event)">
                            <svg id="lockOpenIcon" viewBox="0 0 24 24" width="20" height="20" style="display: block; fill: currentColor;"><path d="M12 17c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm6-9h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zM8.9 6c0-1.71 1.39-3.1 3.1-3.1s3.1 1.39 3.1 3.1v2H8.9V6zM18 20H6V10h12v10z"/></svg>
                            <svg id="lockClosedIcon" viewBox="0 0 24 24" width="20" height="20" style="display: none; fill: currentColor;"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1s3.1 1.39 3.1 3.1v2z"/></svg>
                        </button>
                    </div>
                    
                    <div class="center-controls" id="centerPanel">
                        <button class="center-btn" onclick="skipVideo(-10)">
                            <svg viewBox="0 0 24 24"><path d="M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8zm-1.33 9.35c-.06-.11-.1-.25-.13-.42-.03-.17-.04-.38-.04-.63s.01-.46.04-.63c.03-.17.07-.31.13-.42s.14-.2.24-.26c.1-.06.22-.09.37-.09.15 0 .27.03.37.09.1.06.18.15.24.26s.1.25.13.42c.03.17.04.38.04.63s-.01.46-.04.63c-.03.17-.07.31-.13.42s-.14.2-.24.26c-.1.06-.22.09-.37.09-.15 0-.27-.03-.37-.09-.1-.06-.18-.15-.24-.26zm3.5-.04c-.11-.14-.19-.34-.23-.59s-.07-.56-.07-.93V11c0-.28.02-.53.07-.73s.12-.37.23-.5.23-.2.39-.2c.15 0 .28.07.39.2s.18.31.23.51c.05.2.07.45.07.73v.83c0 .37-.02.68-.07.93s-.12.45-.23.59c-.11.14-.24.21-.39.21s-.28-.07-.39-.21z"/></svg>
                        </button>
                        <button class="center-btn play-pause-btn" id="playPauseCenter" onclick="onTogglePlay()">
                            <svg id="centerPlay" viewBox="0 0 24 24" width="36" height="36" style="display: block;"><path d="M8 5v14l11-7z"/></svg>
                            <svg id="centerPause" viewBox="0 0 24 24" width="36" height="36" style="display: none;"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
                        </button>
                        <button class="center-btn" onclick="skipVideo(10)">
                            <svg viewBox="0 0 24 24"><path d="M10 6L5 11h5v4c3.31 0 6-2.69 6-6s-2.69-6-6-6zm2 15c-4.42 0-8-3.58-8-8h2c0 3.31 2.69 6 6 6s6-2.69 6-6-2.69-6-6-6v4l5-5-5-5v3c-4.42 0-8 3.58-8 8s3.58 8 8 8z"/></svg>
                        </button>
                    </div>
                    
                    <div class="bottom-panel" id="bottomPanel">
                        <!-- Custom styled Seekbar -->
                        <div class="seekbar-container">
                            <span class="time-label" id="currentLabel">00:00</span>
                            <div class="seekbar-wrapper">
                                <div class="seekbar-track"></div>
                                <div class="seekbar-fill" id="seekFill"></div>
                                <div class="seekbar-thumb" id="seekThumb"></div>
                                <input type="range" class="seekbar-input" id="seekInput" min="0" max="100" value="0">
                            </div>
                            <span class="time-label" id="totalLabel">00:00</span>
                        </div>
                        
                        <!-- Actions & Volume -->
                        <div class="actions-row">
                            <div class="left-actions">
                                <button class="btn-icon" id="playPauseBottom" onclick="onTogglePlay()">
                                    <svg id="bottomPlay" viewBox="0 0 24 24" style="display: block;"><path d="M8 5v14l11-7z"/></svg>
                                    <svg id="bottomPause" viewBox="0 0 24 24" style="display: none;"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
                                </button>
                                
                                <div class="volume-control">
                                    <button class="btn-icon" onclick="onToggleMute()">
                                        <svg id="volOnIcon" viewBox="0 0 24 24" style="display: block;"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/></svg>
                                        <svg id="volOffIcon" viewBox="0 0 24 24" style="display: none;"><path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.21.05-.42.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/></svg>
                                    </button>
                                    <input type="range" class="volume-slider" id="volSlider" min="0" max="1" step="0.05" value="1" oninput="changeVolume(this.value)">
                                </div>
                            </div>
                            
                            <div class="right-actions">
                                <select class="speed-menu" id="speedMenu" onchange="changeSpeed(this.value)">
                                    <option value="0.5">0.5x</option>
                                    <option value="0.75">0.75x</option>
                                    <option value="1.0" selected>1.0x</option>
                                    <option value="1.25">1.25x</option>
                                    <option value="1.5">1.5x</option>
                                    <option value="2.0">2.0x</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Quick Feedback panels -->
                <div class="feedback-pill left" id="pLeft">
                    <svg viewBox="0 0 24 24" width="30" height="30" fill="#D0BCFF"><path d="M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z"/></svg>
                    <div class="feedback-label">-10s</div>
                </div>
                <div class="feedback-pill right" id="pRight">
                    <svg viewBox="0 0 24 24" width="30" height="30" fill="#D0BCFF"><path d="M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z"/></svg>
                    <div class="feedback-label">+10s</div>
                </div>
                
                <div class="toast-indicator" id="toastPanel">
                    <div class="toast-title" id="toastTitle">Volume</div>
                    <div class="toast-value" id="toastValue">100%</div>
                </div>
            </div>
            
            <script>
                const video = document.getElementById('videoPlayer');
                const overlay = document.getElementById('overlay');
                const container = document.getElementById('container');
                const titleText = document.getElementById('title');
                const currentLabel = document.getElementById('currentLabel');
                const totalLabel = document.getElementById('totalLabel');
                const seekFill = document.getElementById('seekFill');
                const seekThumb = document.getElementById('seekThumb');
                const seekInput = document.getElementById('seekInput');
                const volSlider = document.getElementById('volSlider');
                const volOn = document.getElementById('volOnIcon');
                const volOff = document.getElementById('volOffIcon');
                
                // Play & Pause UI elements
                const centerPlay = document.getElementById('centerPlay');
                const centerPause = document.getElementById('centerPause');
                const bottomPlay = document.getElementById('bottomPlay');
                const bottomPause = document.getElementById('bottomPause');
                
                // Feedback pills
                const pLeft = document.getElementById('pLeft');
                const pRight = document.getElementById('pRight');
                
                // Toast notifications
                const toastPanel = document.getElementById('toastPanel');
                const toastTitle = document.getElementById('toastTitle');
                const toastValue = document.getElementById('toastValue');
                
                let isLocked = false;
                let hideControlsTimer;
                let isDraggingSeekbar = false;
                let savedVolume = 1.0;
                let toastTimer;
                
                // Invoked by Kotlin WebView Client once HTML finishes loading
                function loadVideo(filePath, fileTitle, progressMs) {
                    titleText.innerText = fileTitle;
                    video.src = filePath;
                    video.load();
                    
                    video.onloadedmetadata = () => {
                        totalLabel.innerText = formatDuration(video.duration);
                        seekInput.max = Math.floor(video.duration);
                        
                        if (progressMs && progressMs > 0) {
                            video.currentTime = progressMs / 1000;
                        }
                        
                        video.play()
                            .then(() => setPlayingState(true))
                            .catch(e => {
                                console.warn("Video autoplay failed.", e);
                            });
                    };
                    
                    video.onerror = () => {
                        if (window.AndroidPlayer) {
                            window.AndroidPlayer.showError("Impossible de charger la vidéo HTML5");
                        }
                    };
                }
                
                function formatDuration(seconds) {
                    if (isNaN(seconds)) return "00:00";
                    const h = Math.floor(seconds / 3600);
                    const m = Math.floor((seconds % 3600) / 60);
                    const s = Math.floor(seconds % 60);
                    const pad = (v) => v.toString().padStart(2, '0');
                    return h > 0 ? (h + ":" + pad(m) + ":" + pad(s)) : (pad(m) + ":" + pad(s));
                }
                
                function onTogglePlay() {
                    if (video.paused) {
                        video.play();
                        setPlayingState(true);
                    } else {
                        video.pause();
                        setPlayingState(false);
                    }
                    keepOverlayVisible();
                }
                
                function setPlayingState(playing) {
                    if (playing) {
                        centerPlay.style.display = 'none';
                        centerPause.style.display = 'block';
                        bottomPlay.style.display = 'none';
                        bottomPause.style.display = 'block';
                    } else {
                        centerPlay.style.display = 'block';
                        centerPause.style.display = 'none';
                        bottomPlay.style.display = 'block';
                        bottomPause.style.display = 'none';
                    }
                }
                
                // Track playback progress
                video.addEventListener('timeupdate', () => {
                    if (!isDraggingSeekbar) {
                        currentLabel.innerText = formatDuration(video.currentTime);
                        seekInput.value = video.currentTime;
                        syncSeekbarUI();
                        
                        // Push progress report to Android ViewModel
                        if (window.AndroidPlayer) {
                            window.AndroidPlayer.onProgress(Math.floor(video.currentTime * 1000));
                        }
                    }
                });
                
                video.addEventListener('ended', () => {
                    setPlayingState(false);
                    if (window.AndroidPlayer) {
                        window.AndroidPlayer.onEnded();
                    }
                });
                
                function syncSeekbarUI() {
                    const percent = (seekInput.value / seekInput.max) * 100;
                    seekFill.style.width = percent + '%';
                    seekThumb.style.left = percent + '%';
                }
                
                // Dragging Seekbar
                seekInput.addEventListener('input', () => {
                    isDraggingSeekbar = true;
                    currentLabel.innerText = formatDuration(seekInput.value);
                    syncSeekbarUI();
                });
                
                seekInput.addEventListener('change', () => {
                    video.currentTime = seekInput.value;
                    isDraggingSeekbar = false;
                    keepOverlayVisible();
                });
                
                function skipVideo(secs) {
                    if (isLocked) return;
                    video.currentTime = Math.max(0, Math.min(video.duration, video.currentTime + secs));
                    keepOverlayVisible();
                }
                
                // Volume Adjustments
                function changeVolume(val) {
                    video.volume = val;
                    video.muted = (val == 0);
                    updateVolumeIcon(val);
                }
                
                function onToggleMute() {
                    if (video.muted) {
                        video.muted = false;
                        video.volume = savedVolume;
                        volSlider.value = savedVolume;
                        updateVolumeIcon(savedVolume);
                    } else {
                        savedVolume = video.volume > 0 ? video.volume : 1.0;
                        video.muted = true;
                        video.volume = 0;
                        volSlider.value = 0;
                        updateVolumeIcon(0);
                    }
                    keepOverlayVisible();
                }
                
                function updateVolumeIcon(val) {
                    if (val == 0) {
                        volOn.style.display = 'none';
                        volOff.style.display = 'block';
                    } else {
                        volOn.style.display = 'block';
                        volOff.style.display = 'none';
                    }
                }
                
                // Speed selection
                function changeSpeed(rate) {
                    video.playbackRate = parseFloat(rate);
                    triggerToast("Vitesse", rate + "x");
                    keepOverlayVisible();
                }
                
                // Lock feature toggling
                function toggleLock(event) {
                    event.stopPropagation();
                    isLocked = !isLocked;
                    
                    const lockOpen = document.getElementById('lockOpenIcon');
                    const lockClosed = document.getElementById('lockClosedIcon');
                    const lockBtn = document.getElementById('lockBtn');
                    
                    if (isLocked) {
                        lockBtn.classList.add('locked');
                        lockOpen.style.display = 'none';
                        lockClosed.style.display = 'block';
                        document.getElementById('centerPanel').style.visibility = 'hidden';
                        document.getElementById('bottomPanel').style.visibility = 'hidden';
                        triggerToast("Écran", "Verrouillé");
                    } else {
                        lockBtn.classList.remove('locked');
                        lockOpen.style.display = 'block';
                        lockClosed.style.display = 'none';
                        document.getElementById('centerPanel').style.visibility = 'visible';
                        document.getElementById('bottomPanel').style.visibility = 'visible';
                        triggerToast("Écran", "Déverrouillé");
                    }
                    keepOverlayVisible();
                }
                
                // Tapping and Gestures handling (Single / Double Taps)
                let lastTap = 0;
                container.addEventListener('click', (e) => {
                    // Ignore clicks on buttons/sliders/etc
                    if (e.target.closest('button') || e.target.closest('input') || e.target.closest('select')) {
                        return;
                    }
                    
                    const now = Date.now();
                    const tapInterval = now - lastTap;
                    
                    if (tapInterval < 300) {
                        // Double Tap
                        if (isLocked) return;
                        const rect = container.getBoundingClientRect();
                        const clickX = e.clientX - rect.left;
                        const halfWidth = rect.width / 2;
                        
                        if (clickX < halfWidth) {
                            skipVideo(-10);
                            showPillFeedback('left');
                        } else {
                            skipVideo(10);
                            showPillFeedback('right');
                        }
                    } else {
                        // Single Tap: toggle controls overlay
                        if (overlay.classList.contains('hidden')) {
                            overlay.classList.remove('hidden');
                            keepOverlayVisible();
                        } else {
                            overlay.classList.add('hidden');
                        }
                    }
                    lastTap = now;
                });
                
                function showPillFeedback(side) {
                    const pill = side === 'left' ? pLeft : pRight;
                    pill.classList.remove('active');
                    void pill.offsetWidth; // Force CSS reflow
                    pill.classList.add('active');
                    setTimeout(() => {
                        pill.classList.remove('active');
                    }, 500);
                }
                
                // Helper to trigger HUD toast notifications
                function triggerToast(label, val) {
                    toastTitle.innerText = label;
                    toastValue.innerText = val;
                    toastPanel.classList.add('active');
                    clearTimeout(toastTimer);
                    toastTimer = setTimeout(() => {
                        toastPanel.classList.remove('active');
                    }, 1000);
                }
                
                function keepOverlayVisible() {
                    clearTimeout(hideControlsTimer);
                    overlay.classList.remove('hidden');
                    hideControlsTimer = setTimeout(() => {
                        if (!video.paused && !isDraggingSeekbar) {
                            overlay.classList.add('hidden');
                        }
                    }, 4000);
                }
                
                // Initial show overlay timer
                keepOverlayVisible();
                
                function onBackBtn() {
                    if (window.AndroidPlayer) {
                        window.AndroidPlayer.closePlayer();
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
