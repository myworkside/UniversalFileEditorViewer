package com.sumitupdat.universalfileeditorviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem

import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun FileItemRow(
    fileItem: FileItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(fileItem.name) },
        supportingContent = {
            Text(if (fileItem.isDirectory) "Folder" else formatSize(fileItem.size))
        },
        leadingContent = {
            Icon(
                imageVector = getFileIcon(fileItem),
                contentDescription = null,
                tint = if (fileItem.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (fileItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

fun getFileIcon(fileItem: FileItem): ImageVector {
    return if (fileItem.isDirectory) {
        Icons.Default.Folder
    } else {
        when (fileItem.extension) {
            "pdf" -> Icons.Default.PictureAsPdf
            "jpg", "png", "gif", "webp" -> Icons.Default.Image
            "mp3", "wav", "flac" -> Icons.Default.AudioFile
            "mp4", "mkv", "avi" -> Icons.Default.VideoFile
            "zip", "rar", "7z" -> Icons.Default.Inventory2
            else -> Icons.Default.Description
        }
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}
