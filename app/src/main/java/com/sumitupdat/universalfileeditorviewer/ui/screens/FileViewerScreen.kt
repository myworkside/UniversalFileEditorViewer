package com.sumitupdat.universalfileeditorviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(fileItem: FileItem, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileItem.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (fileItem.extension) {
                "txt", "java", "kt", "py", "cpp", "c", "html", "css", "js", "json", "xml" -> {
                    Text("Text/Code Viewer for: ${fileItem.name}")
                    // TODO: Implement actual text viewer
                }
                "pdf" -> {
                    Text("PDF Viewer for: ${fileItem.name}")
                    // TODO: Implement actual PDF viewer
                }
                "jpg", "png", "gif", "webp" -> {
                    Text("Image Viewer for: ${fileItem.name}")
                    // TODO: Implement actual image viewer
                }
                "mp3", "wav", "flac" -> {
                    Text("Audio Player for: ${fileItem.name}")
                    // TODO: Implement actual audio player
                }
                "mp4", "mkv", "avi" -> {
                    Text("Video Player for: ${fileItem.name}")
                    // TODO: Implement actual video player
                }
                else -> {
                    Text("Unsupported file type: ${fileItem.extension}")
                }
            }
        }
    }
}
