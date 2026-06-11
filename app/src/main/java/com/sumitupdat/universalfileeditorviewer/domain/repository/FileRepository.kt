package com.sumitupdat.universalfileeditorviewer.domain.repository

import com.sumitupdat.universalfileeditorviewer.data.local.FavoriteFile
import com.sumitupdat.universalfileeditorviewer.data.local.FileDao
import com.sumitupdat.universalfileeditorviewer.data.local.RecentFile
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.data.model.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class FileRepository(private val fileDao: FileDao) {

    suspend fun getFiles(directoryPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        val directory = File(directoryPath)
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.map { it.toFileItem() }?.sortedWith(
                compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() }
            ) ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun createFolder(parentPath: String, folderName: String): Boolean = withContext(Dispatchers.IO) {
        val newFolder = File(parentPath, folderName)
        if (!newFolder.exists()) {
            newFolder.mkdirs()
        } else {
            false
        }
    }

    suspend fun createFile(parentPath: String, fileName: String): Boolean = withContext(Dispatchers.IO) {
        val newFile = File(parentPath, fileName)
        if (!newFile.exists()) {
            newFile.createNewFile()
        } else {
            false
        }
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists()) {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } else {
            false
        }
    }

    suspend fun renameFile(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val oldFile = File(oldPath)
        val newFile = File(oldFile.parent, newName)
        if (oldFile.exists() && !newFile.exists()) {
            oldFile.renameTo(newFile)
        } else {
            false
        }
    }

    // Database operations
    fun getFavorites(): Flow<List<FavoriteFile>> = fileDao.getFavorites()
    fun getRecentFiles(): Flow<List<RecentFile>> = fileDao.getRecentFiles()

    suspend fun toggleFavorite(fileItem: FileItem) {
        val isFavorite = fileDao.isFavorite(fileItem.path)
        if (isFavorite) {
            fileDao.removeFavorite(FavoriteFile(fileItem.path, fileItem.name, fileItem.isDirectory))
        } else {
            fileDao.addFavorite(FavoriteFile(fileItem.path, fileItem.name, fileItem.isDirectory))
        }
    }

    suspend fun addRecentFile(fileItem: FileItem) {
        fileDao.addRecentFile(RecentFile(fileItem.path, fileItem.name, fileItem.isDirectory))
    }

    suspend fun isFavorite(path: String): Boolean = fileDao.isFavorite(path)
}
