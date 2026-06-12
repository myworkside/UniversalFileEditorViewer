package com.sumitupdat.universalfileeditorviewer.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.ui.components.ZoomableBox
import com.sumitupdat.universalfileeditorviewer.ui.screens.viewers.ArchiveViewerScreen
import com.sumitupdat.universalfileeditorviewer.ui.screens.viewers.PptxViewerScreen
import com.sumitupdat.universalfileeditorviewer.ui.screens.viewers.XlsxViewerScreen
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream

private const val TAG = "FileViewerScreen"

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(fileItem: FileItem, viewModel: FileViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val file = File(fileItem.path)
    
    val spreadsheetData by viewModel.spreadsheetData.collectAsState()
    val archiveEntries by viewModel.archiveEntries.collectAsState()
    val presentationData by viewModel.presentationData.collectAsState()
    val isLoading by viewModel.viewerLoading.collectAsState()
    val error by viewModel.viewerError.collectAsState()

    var textContent by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var isTextLoading by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var rotation by remember { mutableIntStateOf(0) }

    val extension = fileItem.extension.lowercase()
    
    val viewerType = remember(extension) {
        when (extension) {
            "txt", "java", "kt", "py", "cpp", "c", "html", "css", "js", "json", "xml", "md", "log", "ini", "cfg", "conf", "php", "yaml", "sh", "bat", "sql" -> ViewerType.TEXT
            "pdf" -> ViewerType.PDF
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "svg", "tiff" -> ViewerType.IMAGE
            "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp" -> ViewerType.VIDEO
            "mp3", "wav", "aac", "flac", "ogg", "m4a" -> ViewerType.AUDIO
            "zip", "rar", "7z", "tar", "gz", "iso" -> ViewerType.ARCHIVE
            "xlsx", "xls" -> ViewerType.XLSX
            "pptx", "ppt" -> ViewerType.PPTX
            "docx", "doc" -> ViewerType.DOCX
            "apk" -> ViewerType.APK
            "db", "sqlite" -> ViewerType.SQLITE
            else -> ViewerType.EXTERNAL
        }
    }

    LaunchedEffect(fileItem.path, viewerType) {
        when (viewerType) {
            ViewerType.TEXT -> {
                isTextLoading = true
                try {
                    textContent = if (file.length() > 512 * 1024) {
                        "File is too large for internal editor. Showing preview...\n\n" + file.inputStream().bufferedReader().use { it.readText().take(5000) }
                    } else file.readText()
                } catch (e: Exception) { Log.e(TAG, "Text load error", e) }
                isTextLoading = false
            }
            ViewerType.XLSX -> viewModel.loadSpreadsheet(fileItem.path)
            ViewerType.ARCHIVE -> viewModel.loadArchive(fileItem.path)
            ViewerType.PPTX -> viewModel.loadPresentation(fileItem.path)
            else -> {}
        }
    }

    fun openInExternalApp() {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this file.", Toast.LENGTH_SHORT).show()
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
        } catch (e: Exception) { }
    }

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    title = { Text(fileItem.name, maxLines = 1) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    actions = {
                        if (viewerType == ViewerType.TEXT && file.length() < 1024 * 1024) {
                            IconButton(onClick = { isEditing = !isEditing }) {
                                Icon(if (isEditing) Icons.Default.Visibility else Icons.Default.Edit, "Toggle Edit")
                            }
                        }
                        IconButton(onClick = { isFullScreen = true }) { Icon(Icons.Default.Fullscreen, "Full Screen") }
                        IconButton(onClick = { rotation = (rotation + 90) % 360 }) { Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate") }
                        IconButton(onClick = { shareFile() }) { Icon(Icons.Default.Share, "Share") }
                        IconButton(onClick = { openInExternalApp() }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open In") }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(if (isFullScreen) PaddingValues(0.dp) else padding), contentAlignment = Alignment.Center) {
            if (isLoading || isTextLoading) CircularProgressIndicator()
            else if (error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    Button(onClick = { openInExternalApp() }) { Text("Try External App") }
                }
            } else {
                when (viewerType) {
                    ViewerType.TEXT -> {
                        if (isEditing) {
                            TextField(value = textContent, onValueChange = { textContent = it }, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(textContent, modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), fontSize = 14.sp)
                        }
                    }
                    ViewerType.IMAGE -> ZoomableBox(modifier = Modifier.fillMaxSize(), rotation = rotation.toFloat()) {
                        AsyncImage(model = file, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                    ViewerType.VIDEO -> VideoPlayer(file = file)
                    ViewerType.AUDIO -> AudioPlayer(file = file)
                    ViewerType.PDF -> PdfViewer(file = file)
                    ViewerType.ARCHIVE -> ArchiveViewerScreen(
                        entries = archiveEntries,
                        onExtract = { viewModel.extractArchiveFile(fileItem.path, it.path) },
                        onExtractAll = { viewModel.extractAll(fileItem.path) }
                    )
                    ViewerType.XLSX -> spreadsheetData?.let { XlsxViewerScreen(data = it) }
                    ViewerType.PPTX -> presentationData?.let { PptxViewerScreen(data = it) }
                    ViewerType.DOCX -> DocxViewer(file = file)
                    ViewerType.APK -> ApkViewer(file = file)
                    ViewerType.SQLITE -> SqliteViewer(file = file)
                    ViewerType.EXTERNAL -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.FilePresent, contentDescription = null, modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text(fileItem.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = { openInExternalApp() }) { Text("Open in external app") }
                        }
                    }
                }
            }
            
            if (isFullScreen) {
                IconButton(
                    onClick = { isFullScreen = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.FullscreenExit, "Exit Full Screen")
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayer(file: File) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(Uri.fromFile(file))); prepare() } }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    AndroidView(factory = { PlayerView(context).apply { player = exoPlayer } }, modifier = Modifier.fillMaxSize())
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun AudioPlayer(file: File) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(Uri.fromFile(file))); prepare() } }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text(file.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        AndroidView(factory = { PlayerView(context).apply { player = exoPlayer; useController = true; controllerAutoShow = true } }, modifier = Modifier.fillMaxWidth().height(100.dp))
    }
}

@Composable
fun PdfViewer(file: File) {
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(file) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val list = mutableListOf<Bitmap>()
            for (i in 0 until renderer.pageCount.coerceAtMost(30)) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                list.add(bitmap)
                page.close()
            }
            bitmaps = list
            renderer.close()
            pfd.close()
        } catch (e: Exception) { Log.e(TAG, "PDF error", e) }
    }
    if (bitmaps.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    else ZoomableBox(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) { items(bitmaps) { bitmap ->
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().padding(8.dp), contentScale = ContentScale.FillWidth)
        }}
    }
}

@Composable
fun DocxViewer(file: File) {
    var text by remember { mutableStateOf("") }
    LaunchedEffect(file) {
        try { FileInputStream(file).use { fis -> text = XWPFDocument(fis).paragraphs.joinToString("\n") { it.paragraphText } } } catch (e: Exception) { }
    }
    ZoomableBox(Modifier.fillMaxSize()) {
        Text(text, modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()))
    }
}

@Composable
fun ApkViewer(file: File) {
    val context = LocalContext.current
    val info = remember(file) { try { context.packageManager.getPackageArchiveInfo(file.absolutePath, 0) } catch (e: Exception) { null } }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Android, null, Modifier.size(100.dp), tint = Color(0xFF3DDC84))
        if (info != null) {
            Text("Package: ${info.packageName}", style = MaterialTheme.typography.titleMedium)
            Text("Version: ${info.versionName}", style = MaterialTheme.typography.bodyMedium)
        } else Text("Invalid APK")
    }
}

@Composable
fun SqliteViewer(file: File) {
    var tables by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(file) {
        try {
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(file.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            val list = mutableListOf<String>()
            while (cursor.moveToNext()) list.add(cursor.getString(0))
            cursor.close(); db.close()
            tables = list
        } catch (e: Exception) { }
    }
    LazyColumn(Modifier.fillMaxSize()) { items(tables) { table ->
        ListItem(headlineContent = { Text(table) }, leadingContent = { Icon(Icons.Default.TableChart, null) })
        HorizontalDivider()
    }}
}

enum class ViewerType {
    TEXT, IMAGE, VIDEO, AUDIO, PDF, ARCHIVE, XLSX, PPTX, DOCX, APK, SQLITE, EXTERNAL
}
