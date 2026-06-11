package com.sumitupdat.universalfileeditorviewer.domain.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.sumitupdat.universalfileeditorviewer.data.local.FavoriteFile
import com.sumitupdat.universalfileeditorviewer.data.local.FileDao
import com.sumitupdat.universalfileeditorviewer.data.local.RecentFile
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.data.model.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val TAG = "FileRepository"

class FileRepository(private val fileDao: FileDao, private val context: Context) {

    fun getStorageRoots(): List<File> {
        val roots = mutableListOf<File>()
        try {
            // Internal Storage
            val internalRoot = Environment.getExternalStorageDirectory()
            Log.d(TAG, "Internal Storage Root: ${internalRoot.absolutePath}")
            if (internalRoot.exists()) {
                roots.add(internalRoot)
            } else {
                val fallbackRoot = File("/storage/emulated/0")
                if (fallbackRoot.exists()) roots.add(fallbackRoot)
            }

            // SD Cards and other external storage
            val externalFilesDirs = context.getExternalFilesDirs(null)
            for (dir in externalFilesDirs) {
                if (dir != null) {
                    val path = dir.absolutePath
                    val index = path.indexOf("/Android/data/")
                    if (index != -1) {
                        val rootPath = path.substring(0, index)
                        val root = File(rootPath)
                        if (!roots.contains(root) && root.exists()) {
                            roots.add(root)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting storage roots", e)
        }
        return roots
    }

    suspend fun getFiles(directoryPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val directory = File(directoryPath)
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                if (files == null) {
                    Log.e(TAG, "listFiles() returned null for $directoryPath")
                    return@withContext emptyList()
                }
                files.map { it.toFileItem() }.sortedWith(
                    compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() }
                )
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getFiles", e)
            emptyList()
        }
    }

    suspend fun searchFiles(query: String, rootPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<File>()
        try {
            val root = File(rootPath)
            if (root.exists() && root.isDirectory) {
                searchRecursive(root, query, results)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
        }
        results.map { it.toFileItem() }
    }

    private fun searchRecursive(directory: File, query: String, results: MutableList<File>) {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.name.contains(query, ignoreCase = true)) {
                results.add(file)
            }
            if (file.isDirectory && results.size < 100) {
                searchRecursive(file, query, results)
            }
            if (results.size >= 100) break
        }
    }

    suspend fun createFolder(parentPath: String, folderName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val newFolder = File(parentPath, folderName)
            if (!newFolder.exists()) {
                val success = newFolder.mkdirs()
                if (success) scanFile(newFolder)
                success
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createFile(parentPath: String, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val newFile = File(parentPath, fileName)
            if (!newFile.exists()) {
                val success = newFile.createNewFile()
                if (success) scanFile(newFile)
                success
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                val success = if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                if (success) scanFile(file)
                success
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFile(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            val newFile = File(oldFile.parent, newName)
            if (oldFile.exists() && !newFile.exists()) {
                val success = oldFile.renameTo(newFile)
                if (success) {
                    scanFile(oldFile)
                    scanFile(newFile)
                }
                success
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun copyFile(sourcePath: String, destPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            if (sourceFile.isDirectory) {
                sourceFile.copyRecursively(destFile, overwrite = true)
            } else {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            scanFile(destFile)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun moveFile(sourcePath: String, destPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            val success = if (sourceFile.renameTo(destFile)) {
                true
            } else {
                if (copyFile(sourcePath, destPath)) {
                    deleteFile(sourcePath)
                } else {
                    false
                }
            }
            if (success) {
                scanFile(sourceFile)
                scanFile(destFile)
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    private fun scanFile(file: File) {
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
    }

    fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

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
