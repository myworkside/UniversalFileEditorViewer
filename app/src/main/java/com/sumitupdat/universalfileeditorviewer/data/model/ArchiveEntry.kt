package com.sumitupdat.universalfileeditorviewer.data.model

data class ArchiveEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long = 0,
    val lastModified: Long = 0
)
