package com.sumitupdat.universalfileeditorviewer.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.github.junrar.Archive
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.ui.components.ZoomableBox
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

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
        if (viewerType == ViewerType.TEXT) {
            isLoading = true
            try {
                textContent = if (file.length() > 512 * 1024) {
                    "File is too large for internal editor. Showing preview...\n\n" + file.inputStream().bufferedReader().use { it.readText().take(5000) }
                } else file.readText()
            } catch (e: Exception) { error = "Error: ${e.message}" }
            isLoading = false
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
        } catch (e: Exception) { error = "No app found to open this file." }
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
            TopAppBar(
                title = { Text(fileItem.name, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (viewerType == ViewerType.TEXT && file.length() < 1024 * 1024) {
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(if (isEditing) Icons.Default.Visibility else Icons.Default.Edit, "Toggle Edit")
                        }
                    }
                    IconButton(onClick = { shareFile() }) { Icon(Icons.Default.Share, "Share") }
                    IconButton(onClick = { openInExternalApp() }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open In") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (isLoading) CircularProgressIndicator()
            else if (error != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
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
                    ViewerType.IMAGE -> ZoomableBox(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(model = file, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                    ViewerType.VIDEO -> VideoPlayer(file = file)
                    ViewerType.AUDIO -> AudioPlayer(file = file)
                    ViewerType.PDF -> PdfViewer(file = file)
                    ViewerType.ARCHIVE -> ArchiveViewer(file = file)
                    ViewerType.XLSX -> XlsxViewer(file = file)
                    ViewerType.PPTX -> PptxViewer(file = file)
                    ViewerType.DOCX -> DocxViewer(file = file)
                    ViewerType.APK -> ApkViewer(file = file)
                    ViewerType.SQLITE -> SqliteViewer(file = file)
                    ViewerType.EXTERNAL -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = com.sumitupdat.universalfileeditorviewer.ui.components.getFileIcon(fileItem), contentDescription = null, modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text(fileItem.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                            Text(com.sumitupdat.universalfileeditorviewer.ui.components.formatSize(fileItem.size), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = { openInExternalApp() }) { Text("Open in external app") }
                        }
                    }
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
fun ArchiveViewer(file: File) {
    var entries by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(file) {
        isLoading = true
        try {
            val list = mutableListOf<String>()
            val extension = file.extension.lowercase()
            when (extension) {
                "zip" -> ZipFile(file).use { zip -> list.addAll(zip.entries.asSequence().map { it.name }) }
                "rar" -> Archive(file).use { archive -> list.addAll(archive.fileHeaders.map { it.fileNameString }) }
                "7z" -> SevenZFile(file).use { sz -> 
                    var entry = sz.nextEntry
                    while(entry != null) { list.add(entry.name); entry = sz.nextEntry }
                }
                "tar" -> TarArchiveInputStream(BufferedInputStream(FileInputStream(file))).use { ais ->
                    var entry = ais.nextEntry
                    while(entry != null) { list.add(entry.name); entry = ais.nextEntry }
                }
                "gz" -> GzipCompressorInputStream(BufferedInputStream(FileInputStream(file))).use { list.add(file.name.removeSuffix(".gz")) }
            }
            entries = list
        } catch (e: Exception) { Log.e(TAG, "Archive error", e) }
        isLoading = false
    }

    if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    else LazyColumn(Modifier.fillMaxSize()) {
        items(entries) { entry ->
            ListItem(headlineContent = { Text(entry) }, leadingContent = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null) }, trailingContent = { Icon(Icons.Default.FileDownload, "Extract") })
            HorizontalDivider()
        }
    }
}

@Composable
fun XlsxViewer(file: File) {
    var workbook by remember { mutableStateOf<XSSFWorkbook?>(null) }
    var selectedSheet by remember { mutableIntStateOf(0) }
    LaunchedEffect(file) {
        try { FileInputStream(file).use { fis -> workbook = XSSFWorkbook(fis) } } catch (e: Exception) { }
    }
    Column(Modifier.fillMaxSize()) {
        workbook?.let { wb ->
            LazyRow(Modifier.fillMaxWidth().padding(8.dp)) {
                items(wb.numberOfSheets) { i ->
                    FilterChip(selected = selectedSheet == i, onClick = { selectedSheet = i }, label = { Text(wb.getSheetName(i)) }, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            val sheet = wb.getSheetAt(selectedSheet)
            ZoomableBox(Modifier.weight(1f)) {
                val sv = rememberScrollState(); val sh = rememberScrollState()
                Column(Modifier.fillMaxSize().verticalScroll(sv).horizontalScroll(sh).padding(16.dp)) {
                    for (row in sheet) {
                        Row {
                            for (cell in row) {
                                val value = try { when (cell.cellType) {
                                    org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toString()
                                    org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
                                    else -> ""
                                } } catch (e: Exception) { "" }
                                Surface(Modifier.width(100.dp).height(30.dp).padding(1.dp), border = BorderStroke(0.5.dp, Color.Gray)) {
                                    Text(value, maxLines = 1, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
fun PptxViewer(file: File) {
    var slideshow by remember { mutableStateOf<XMLSlideShow?>(null) }
    var current by remember { mutableIntStateOf(0) }
    LaunchedEffect(file) {
        try { FileInputStream(file).use { fis -> slideshow = XMLSlideShow(fis) } } catch (e: Exception) { }
    }
    Column(Modifier.fillMaxSize()) {
        slideshow?.let { ss ->
            val slides = ss.slides
            if (slides.isNotEmpty()) {
                ZoomableBox(Modifier.weight(1f)) {
                    val slide = slides[current]
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp)) {
                        slide.shapes.forEach { if (it is XSLFTextShape) Text(it.text, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { if (current > 0) current-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    Text("Slide ${current + 1} / ${slides.size}")
                    IconButton(onClick = { if (current < slides.size - 1) current++ }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
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
