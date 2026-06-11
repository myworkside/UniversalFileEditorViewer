package com.sumitupdat.universalfileeditorviewer.data.model

import java.io.File

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val extension: String = "",
    val isFavorite: Boolean = false
)

fun File.toFileItem(): FileItem {
    return FileItem(
        name = this.name,
        path = this.absolutePath,
        isDirectory = this.isDirectory,
        size = if (this.isDirectory) 0 else this.length(),
        lastModified = this.lastModified(),
        extension = this.extension.lowercase()
    )
}
