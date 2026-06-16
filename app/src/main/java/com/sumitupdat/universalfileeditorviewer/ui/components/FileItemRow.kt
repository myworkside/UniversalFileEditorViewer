package com.sumitupdat.universalfileeditorviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.data.model.FileCategory
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.ui.theme.*

import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun FileItemRow(
    fileItem: FileItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onMoveToVault: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(fileItem.name) },
        supportingContent = {
            Text(if (fileItem.isDirectory) "Folder" else formatSize(fileItem.size))
        },
        leadingContent = {
            Icon(
                imageVector = getFileIcon(fileItem),
                contentDescription = null,
                tint = if (fileItem.isDirectory) MaterialTheme.colorScheme.primary else getCategoryColor(fileItem.category)
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (fileItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (fileItem.isFavorite) CategoryFavorite else LocalContentColor.current
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (!fileItem.isDirectory && onMoveToVault != null) {
                            DropdownMenuItem(
                                text = { Text("Move to Private Vault") },
                                onClick = { 
                                    onMoveToVault()
                                    showMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { 
                                onDelete()
                                showMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

fun getCategoryColor(category: FileCategory): Color {
    return when (category) {
        FileCategory.DOCUMENTS -> CategoryDoc
        FileCategory.IMAGES -> CategoryImage
        FileCategory.AUDIO -> CategoryAudio
        FileCategory.VIDEO -> CategoryVideo
        FileCategory.ARCHIVES -> CategoryArchive
        FileCategory.CODE -> CategoryCode
        FileCategory.DATABASES -> CategoryDatabase
        FileCategory.ANDROID -> CategoryAndroid
        FileCategory.WEB -> CategoryWeb
        FileCategory.FONTS -> CategoryFont
        FileCategory.RECENT -> CategoryRecent
        FileCategory.FAVORITES -> CategoryFavorite
        else -> CategoryCode
    }
}

fun getFileIcon(fileItem: FileItem): ImageVector {
    return if (fileItem.isDirectory) {
        Icons.Default.Folder
    } else {
        when (fileItem.extension.lowercase()) {
            "pdf" -> Icons.Default.PictureAsPdf
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "svg" -> Icons.Default.Image
            "mp3", "wav", "aac", "flac", "ogg", "m4a" -> Icons.Default.AudioFile
            "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp" -> Icons.Default.VideoFile
            "zip", "rar", "7z", "tar", "gz" -> Icons.Default.Inventory2
            "txt", "log", "ini", "cfg", "conf" -> Icons.Default.Description
            "java", "kt", "py", "cpp", "c", "html", "css", "js", "php", "json", "xml", "yaml" -> Icons.Default.Code
            "apk", "aab" -> Icons.Default.Android
            "db", "sqlite" -> Icons.Default.Storage
            "ttf", "otf" -> Icons.Default.FontDownload
            else -> Icons.Default.InsertDriveFile
        }
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}
