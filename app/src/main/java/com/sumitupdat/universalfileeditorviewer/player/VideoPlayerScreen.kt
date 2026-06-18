package com.sumitupdat.universalfileeditorviewer.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    file: File,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val controller by viewModel.controller.collectAsState()

    LaunchedEffect(file) {
        viewModel.playFile(file, isVideo = true)
    }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { viewModel.toggleControllerVisibility() },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width / 2) {
                            viewModel.seekTo((uiState.currentPosition - 10000).coerceAtLeast(0))
                        } else {
                            viewModel.seekTo((uiState.currentPosition + 10000).coerceAtMost(uiState.duration))
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        val width = size.width
                        val height = size.height
                        val position = change.position
                        
                        if (position.x < width / 2) {
                            viewModel.adjustBrightness(-dragAmount.y / height)
                        } else {
                            viewModel.adjustVolume(-dragAmount.y / height)
                        }
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = controller
            },
            modifier = Modifier.fillMaxSize()
        )

        // Brightness / Volume Indicators
        GestureIndicator(uiState)

        // VLC-Style Controls Overlay
        AnimatedVisibility(
            visible = uiState.isControllerVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            VideoControls(
                uiState = uiState,
                context = context,
                onBack = onBack,
                onTogglePlay = viewModel::togglePlayPause,
                onSeek = viewModel::seekTo,
                onSpeedChange = viewModel::setPlaybackSpeed
            )
        }
    }
}

@Composable
fun GestureIndicator(uiState: PlayerUiState) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Brightness indicator on the left
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(32.dp)
                .width(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.LightMode, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { uiState.brightness },
                modifier = Modifier
                    .width(4.dp)
                    .height(100.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }

        // Volume indicator on the right
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(32.dp)
                .width(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (uiState.volume > 0.5f) Icons.AutoMirrored.Filled.VolumeUp else if (uiState.volume > 0f) Icons.AutoMirrored.Filled.VolumeDown else Icons.AutoMirrored.Filled.VolumeMute,
                null, tint = Color.White, modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { uiState.volume },
                modifier = Modifier
                    .width(4.dp)
                    .height(100.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun VideoControls(
    uiState: PlayerUiState,
    context: Context,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text(
                text = uiState.fileName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { 
                val activity = context.findActivity()
                activity?.requestedOrientation = if (activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }) {
                Icon(Icons.Default.ScreenRotation, "Rotate", tint = Color.White)
            }

            IconButton(onClick = { 
                val activity = context.findActivity()
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                activity?.enterPictureInPictureMode(params)
            }) {
                Icon(Icons.Default.PictureInPicture, "PiP", tint = Color.White)
            }

            IconButton(onClick = { /* Subtitles */ }) {
                Icon(Icons.Default.Subtitles, "Subtitles", tint = Color.White)
            }
            IconButton(onClick = { /* Settings */ }) {
                Icon(Icons.Default.Settings, "Settings", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Seek Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(uiState.currentPosition), color = Color.White, fontSize = 12.sp)
                Slider(
                    value = uiState.currentPosition.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..(uiState.duration.toFloat().coerceAtLeast(1f)),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(formatTime(uiState.duration), color = Color.White, fontSize = 12.sp)
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showSpeedDialog by remember { mutableStateOf(false) }
                
                if (showSpeedDialog) {
                    AlertDialog(
                        onDismissRequest = { showSpeedDialog = false },
                        title = { Text("Playback Speed") },
                        text = {
                            Column {
                                listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).forEach { speed ->
                                    TextButton(
                                        onClick = { 
                                            onSpeedChange(speed)
                                            showSpeedDialog = false 
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("${speed}x", color = if (uiState.playbackSpeed == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showSpeedDialog = false }) { Text("Close") } }
                    )
                }

                IconButton(onClick = { showSpeedDialog = true }) {
                    Text("${uiState.playbackSpeed}x", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = { /* Previous */ }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { onSeek(uiState.currentPosition - 10000) }) {
                    Icon(Icons.Default.Replay10, "Rewind", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(64.dp)) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
                IconButton(onClick = { onSeek(uiState.currentPosition + 10000) }) {
                    Icon(Icons.Default.Forward10, "Forward", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { /* Next */ }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
