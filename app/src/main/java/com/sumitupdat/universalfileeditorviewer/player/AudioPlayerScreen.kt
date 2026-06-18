package com.sumitupdat.universalfileeditorviewer.player

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@Composable
fun AudioPlayerScreen(
    file: File,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(file) {
        viewModel.playFile(file, isVideo = false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                "Now Playing",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { /* Equalizer */ }) {
                Icon(Icons.Default.Equalizer, "Equalizer")
            }
            IconButton(onClick = { /* Sleep Timer */ }) {
                Icon(Icons.Default.Timer, "Sleep Timer")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Large Album Art / Icon
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MusicNote,
                null,
                modifier = Modifier.size(150.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title and Subtitle
        Text(
            text = uiState.fileName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = "Unknown Artist",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // Progress Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = uiState.currentPosition.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..(uiState.duration.toFloat().coerceAtLeast(1f)),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(uiState.currentPosition), style = MaterialTheme.typography.labelSmall)
                Text(formatTime(uiState.duration), style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    Icons.Default.Shuffle, 
                    "Shuffle", 
                    tint = if (uiState.shuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { /* Previous */ }) {
                Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(48.dp))
            }
            FilledIconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "Play/Pause",
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = { /* Next */ }) {
                Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(48.dp))
            }
            IconButton(onClick = { viewModel.toggleRepeat() }) {
                Icon(
                    when (uiState.repeatMode) {
                        androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        androidx.media3.common.Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                        else -> Icons.Default.Repeat
                    },
                    "Repeat",
                    tint = if (uiState.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
