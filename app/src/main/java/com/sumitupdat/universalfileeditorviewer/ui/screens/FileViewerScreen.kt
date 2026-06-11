package com.sumitupdat.universalfileeditorviewer.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import java.io.File
import java.util.zip.ZipFile

private const val TAG = "FileViewerScreen"

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(fileItem: FileItem, onBack: () -> Unit) {
    val context = LocalContext.current
    val file = File(fileItem.path)
    
    var textContent by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val extension = fileItem.extension.lowercase()
    
    val viewerType = remember(extension) {
        when (extension) {
            "txt", "java", "kt", "py", "cpp", "c", "html", "css", "js", "json", "xml", "md", "log", "ini", "cfg", "conf", "php", "yaml", "sh", "bat", "sql" -> ViewerType.TEXT
            "pdf" -> ViewerType.PDF
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "svg" -> ViewerType.IMAGE
            "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp" -> ViewerType.VIDEO
            "mp3", "wav", "aac", "flac", "ogg", "m4a" -> ViewerType.AUDIO
            "zip" -> ViewerType.ZIP
            "apk" -> ViewerType.APK
            "db", "sqlite" -> ViewerType.SQLITE
            else -> ViewerType.EXTERNAL
        }
    }

    LaunchedEffect(fileItem.path, viewerType) {
        if (viewerType == ViewerType.TEXT) {
            isLoading = true
            try {
                textContent = if (file.length() > 1024 * 1024) {
                    "File is too large to display (${file.length() / 1024} KB). Try opening in an external app."
                } else {
                    file.readText()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading text file", e)
                error = "Error reading file: ${e.message}"
            }
            isLoading = false
        }
    }

    fun openInExternalApp() {
        Log.d(TAG, "Opening in external app: ${file.absolutePath}")
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open in external app", e)
            error = "Could not find an app to open this file."
        }
    }

    fun shareFile() {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share file"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share file", e)
        }
    }

    fun saveFile() {
        try {
            file.writeText(textContent)
            isEditing = false
            Log.d(TAG, "File saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save file", e)
            error = "Failed to save file: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileItem.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewerType == ViewerType.TEXT) {
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
                    IconButton(onClick = { openInExternalApp() }) {
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
            } else if (error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { openInExternalApp() }) {
                        Text("Try External App")
                    }
                }
            } else {
                when (viewerType) {
                    ViewerType.TEXT -> {
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
                    }
                    ViewerType.IMAGE -> {
                        AsyncImage(
                            model = file,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    ViewerType.VIDEO, ViewerType.AUDIO -> {
                        VideoPlayer(file = file)
                    }
                    ViewerType.PDF -> {
                        PdfViewer(file = file)
                    }
                    ViewerType.ZIP -> {
                        ZipViewer(file = file)
                    }
                    ViewerType.APK -> {
                        ApkViewer(file = file)
                    }
                    ViewerType.SQLITE -> {
                        SqliteViewer(file = file)
                    }
                    ViewerType.EXTERNAL -> {
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
                            Button(onClick = { openInExternalApp() }) {
                                Text("Open in external app")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(file: File) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun PdfViewer(file: File) {
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(file) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            val list = mutableListOf<Bitmap>()
            for (i in 0 until pageCount.coerceAtMost(20)) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                list.add(bitmap)
                page.close()
            }
            bitmaps = list
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering PDF", e)
        }
    }

    if (bitmaps.isEmpty()) {
        CircularProgressIndicator()
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(bitmaps) { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

@Composable
fun ZipViewer(file: File) {
    var entries by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(file) {
        try {
            ZipFile(file).use { zip ->
                entries = zip.entries().asSequence().map { it.name }.toList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading ZIP", e)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(entries) { entry ->
            ListItem(
                headlineContent = { Text(entry) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun ApkViewer(file: File) {
    val context = LocalContext.current
    val packageInfo = remember(file) {
        try {
            context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        } catch (e: Exception) {
            null
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color(0xFF3DDC84))
        Spacer(modifier = Modifier.height(16.dp))
        if (packageInfo != null) {
            Text("Package: ${packageInfo.packageName}", style = MaterialTheme.typography.titleMedium)
            @Suppress("DEPRECATION")
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
            Text("Version: ${packageInfo.versionName} ($versionCode)", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("Could not read APK information", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun SqliteViewer(file: File) {
    var tables by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(file) {
        try {
            val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            val list = mutableListOf<String>()
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0))
            }
            cursor.close()
            db.close()
            tables = list
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SQLite DB", e)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Tables in Database:", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
        LazyColumn {
            items(tables) { table ->
                ListItem(
                    headlineContent = { Text(table) },
                    leadingContent = { Icon(Icons.Default.TableChart, contentDescription = null) }
                )
                HorizontalDivider()
            }
        }
    }
}

enum class ViewerType {
    TEXT, IMAGE, VIDEO, AUDIO, PDF, ZIP, APK, SQLITE, EXTERNAL
}
