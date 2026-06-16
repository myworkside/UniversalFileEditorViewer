package com.sumitupdat.universalfileeditorviewer.domain.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.sumitupdat.universalfileeditorviewer.data.local.*
import com.sumitupdat.universalfileeditorviewer.data.model.FileCategory
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.data.model.getCategoryFromExtension
import com.sumitupdat.universalfileeditorviewer.data.model.toFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val TAG = "FileRepository"

class FileRepository(private val fileDao: FileDao, private val context: Context) {

    fun getStorageRoots(): List<File> {
        val roots = mutableListOf<File>()
        try {
            val internalRoot = Environment.getExternalStorageDirectory()
            if (internalRoot.exists()) roots.add(internalRoot)
            
            val externalFilesDirs = context.getExternalFilesDirs(null)
            for (dir in externalFilesDirs) {
                if (dir != null) {
                    val path = dir.absolutePath
                    val index = path.indexOf("/Android/data/")
                    if (index != -1) {
                        val root = File(path.substring(0, index))
                        if (!roots.contains(root) && root.exists()) roots.add(root)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting storage roots", e)
        }
        return roots
    }

    suspend fun startFullScan() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting exhaustive storage scan...")
            val indexedFiles = mutableListOf<IndexedFile>()
            val roots = getStorageRoots()
            
            for (root in roots) {
                scanRecursive(root, indexedFiles)
            }
            
            fileDao.clearIndex()
            indexedFiles.chunked(1000).forEach { chunk ->
                fileDao.insertIndex(chunk)
            }
            Log.d(TAG, "Scan completed. Indexed ${indexedFiles.size} files across all folders.")
        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
        }
    }

    private fun scanRecursive(directory: File, results: MutableList<IndexedFile>) {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.name.startsWith(".")) continue
            
            if (file.isDirectory) {
                scanRecursive(file, results)
            } else {
                val extension = file.extension.lowercase()
                results.add(IndexedFile(
                    path = file.absolutePath,
                    name = file.name,
                    isDirectory = false,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    extension = extension,
                    category = getCategoryFromExtension(extension, false)
                ))
            }
        }
    }

    fun searchIndexedFiles(query: String): Flow<List<FileItem>> = 
        fileDao.searchFiles(query).map { list -> list.map { it.toFileItem() } }
    
    fun getFilesByCategory(category: FileCategory): Flow<List<FileItem>> = 
        fileDao.getFilesByCategory(category).map { list -> list.map { it.toFileItem() } }
    
    fun getCountByCategory(category: FileCategory): Flow<Int> = fileDao.getCountByCategory(category)

    private fun IndexedFile.toFileItem(): FileItem = FileItem(
        name = name,
        path = path,
        isDirectory = isDirectory,
        size = size,
        lastModified = lastModified,
        extension = extension,
        category = category
    )

    suspend fun getFiles(
        directoryPath: String,
        showHidden: Boolean = false,
        sortOrder: FileSortOrder = FileSortOrder.NAME_ASC
    ): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val directory = File(directoryPath)
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles() ?: return@withContext emptyList()
                
                val filteredFiles = if (showHidden) files.toList() else files.filter { !it.name.startsWith(".") }
                
                val fileItems = filteredFiles.map { it.toFileItem() }
                
                sortFileItems(fileItems, sortOrder)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun sortFileItems(items: List<FileItem>, order: FileSortOrder): List<FileItem> {
        val comparator = when (order) {
            FileSortOrder.NAME_ASC -> compareBy<FileItem> { it.name.lowercase() }
            FileSortOrder.NAME_DESC -> compareByDescending<FileItem> { it.name.lowercase() }
            FileSortOrder.DATE_ASC -> compareBy<FileItem> { it.lastModified }
            FileSortOrder.DATE_DESC -> compareByDescending<FileItem> { it.lastModified }
            FileSortOrder.SIZE_ASC -> compareBy<FileItem> { it.size }
            FileSortOrder.SIZE_DESC -> compareByDescending<FileItem> { it.size }
        }
        
        return items.sortedWith(compareByDescending<FileItem> { it.isDirectory }.then(comparator))
    }

    suspend fun createFolder(parentPath: String, folderName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val newFolder = File(parentPath, folderName)
            if (!newFolder.exists()) {
                val success = newFolder.mkdirs()
                if (success) scanFile(newFolder)
                success
            } else false
        } catch (e: Exception) { false }
    }

    suspend fun createFile(parentPath: String, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val newFile = File(parentPath, fileName)
            if (!newFile.exists()) {
                val success = newFile.createNewFile()
                if (success) scanFile(newFile)
                success
            } else false
        } catch (e: Exception) { false }
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
                if (success) scanFile(file)
                success
            } else false
        } catch (e: Exception) { false }
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
            } else false
        } catch (e: Exception) { false }
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
        } catch (e: Exception) { false }
    }

    suspend fun moveFile(sourcePath: String, destPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            val success = if (sourceFile.renameTo(destFile)) true
            else if (copyFile(sourcePath, destPath)) { deleteFile(sourcePath); true }
            else false
            if (success) {
                scanFile(sourceFile)
                scanFile(destFile)
            }
            success
        } catch (e: Exception) { false }
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
