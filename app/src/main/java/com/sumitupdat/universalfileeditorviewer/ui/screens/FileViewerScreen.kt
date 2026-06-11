package com.sumitupdat.universalfileeditorviewer.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(fileItem: FileItem, onBack: () -> Unit) {
    val context = LocalContext.current
    val file = File(fileItem.path)
    
    var textContent by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val isTextFile = remember(fileItem.extension) {
        listOf("txt", "java", "kt", "py", "cpp", "c", "html", "css", "js", "json", "xml", "md").contains(fileItem.extension.lowercase())
    }

    LaunchedEffect(fileItem.path) {
        if (isTextFile) {
            isLoading = true
            try {
                textContent = file.readText()
            } catch (e: Exception) {
                textContent = "Error reading file: ${e.message}"
            }
            isLoading = false
        }
    }

    fun openFile() {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun shareFile() {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share file"))
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun saveFile() {
        try {
            file.writeText(textContent)
            isEditing = false
        } catch (e: Exception) {
            // Handle error
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileItem.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isTextFile) {
                        if (isEditing) {
                            IconButton(onClick = { saveFile() }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                    IconButton(onClick = { shareFile() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { openFile() }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open In")
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
            if (isLoading) {
                CircularProgressIndicator()
            } else if (isTextFile) {
                if (isEditing) {
                    TextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val scrollState = rememberScrollState()
                    Text(
                        text = textContent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = com.sumitupdat.universalfileeditorviewer.ui.components.getFileIcon(fileItem),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(fileItem.name, style = MaterialTheme.typography.headlineSmall)
                    Text(com.sumitupdat.universalfileeditorviewer.ui.components.formatSize(fileItem.size), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = { openFile() }) {
                        Text("Open in external app")
                    }
                }
            }
        }
    }
}
