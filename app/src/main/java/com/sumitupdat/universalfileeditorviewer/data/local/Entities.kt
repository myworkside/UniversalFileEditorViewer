package com.sumitupdat.universalfileeditorviewer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sumitupdat.universalfileeditorviewer.data.model.FileCategory

@Entity(tableName = "favorites")
data class FavoriteFile(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val openedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "file_index")
data class IndexedFile(
    @PrimaryKey val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String,
    val category: FileCategory
)
