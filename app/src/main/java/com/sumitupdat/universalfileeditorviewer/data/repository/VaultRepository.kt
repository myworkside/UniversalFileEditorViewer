package com.sumitupdat.universalfileeditorviewer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sumitupdat.universalfileeditorviewer.data.local.VaultAuditLog
import com.sumitupdat.universalfileeditorviewer.data.local.VaultFileDao
import com.sumitupdat.universalfileeditorviewer.data.local.VaultFileEntity
import com.sumitupdat.universalfileeditorviewer.util.security.HashUtils
import com.sumitupdat.universalfileeditorviewer.util.security.VaultManager
import com.sumitupdat.universalfileeditorviewer.util.vault.CategoryDetector
import com.sumitupdat.universalfileeditorviewer.util.vault.SecureDeleteManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(
    private val vaultDao: VaultFileDao,
    @ApplicationContext private val context: Context
) {
    private val vaultDir = File(context.filesDir, "Vault").apply { if (!exists()) mkdirs() }

    fun getFiles(sort: String = "DATE"): Flow<List<VaultFileEntity>> = vaultDao.getAllVaultFiles(sort)
    fun searchFiles(query: String): Flow<List<VaultFileEntity>> = vaultDao.searchVaultFiles(query)
    fun getTrashFiles(): Flow<List<VaultFileEntity>> = vaultDao.getTrashFiles()
    fun getTotalVaultSize(): Flow<Long?> = vaultDao.getTotalVaultSize()
    fun getVaultFileCount(): Flow<Int> = vaultDao.getVaultFileCount()
    fun getAuditLogs(): Flow<List<VaultAuditLog>> = vaultDao.getRecentAuditLogs()

    suspend fun moveToVault(
        uri: Uri,
        originalName: String? = null,
        category: String? = null,
        fileSize: Long? = null
    ) = withContext(Dispatchers.IO) {
        val (infoName, infoSize) = getFileInfo(uri)
        val name = originalName ?: infoName
        val size = fileSize ?: infoSize
        
        // Calculate SHA-256 for original file
        val originalChecksum = context.contentResolver.openInputStream(uri)?.use { 
            HashUtils.calculateSha256(it)
        } ?: ""

        val finalCategory = category ?: CategoryDetector.detectCategory(name)
        val encryptedFileName = UUID.randomUUID().toString() + ".enc"
        val encryptedFile = File(vaultDir, encryptedFileName)
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(encryptedFile).use { outputStream ->
                val iv = VaultManager.encryptStream(inputStream, outputStream)
                
                val entity = VaultFileEntity(
                    originalName = name,
                    encryptedPath = encryptedFile.absolutePath,
                    fileSize = size,
                    category = finalCategory,
                    iv = iv,
                    checksum = originalChecksum,
                    keyVersion = VaultManager.LATEST_KEY_VERSION
                )
                vaultDao.insertVaultFile(entity)
                logAction("ADD", "Added $name ($finalCategory)")
            }
        }
    }

    suspend fun restoreFromVault(id: Long, targetDir: File): Result<String> = withContext(Dispatchers.IO) {
        val entity = vaultDao.getVaultFileById(id) ?: return@withContext Result.failure(Exception("File not found"))
        val encryptedFile = File(entity.encryptedPath)
        
        val safeName = entity.originalName.replace("..", "").replace("/", "")
        val restoredFile = getUniqueFile(targetDir, safeName)

        if (!encryptedFile.exists()) return@withContext Result.failure(Exception("Encrypted file missing"))

        try {
            FileInputStream(encryptedFile).use { inputStream ->
                FileOutputStream(restoredFile).use { outputStream ->
                    VaultManager.decryptStream(inputStream, outputStream, entity.iv, entity.keyVersion)
                }
            }
            
            // Verify integrity
            val restoredChecksum = FileInputStream(restoredFile).use { HashUtils.calculateSha256(it) }
            if (restoredChecksum != entity.checksum) {
                restoredFile.delete()
                return@withContext Result.failure(Exception("File integrity verification failed"))
            }

            SecureDeleteManager.secureDelete(encryptedFile)
            vaultDao.deleteVaultFile(entity)
            logAction("RESTORE", "Restored ${entity.originalName}")
            return@withContext Result.success(restoredFile.name)
        } catch (e: Exception) {
            if (restoredFile.exists()) restoredFile.delete()
            return@withContext Result.failure(e)
        }
    }

    suspend fun moveToTrash(id: Long) = withContext(Dispatchers.IO) {
        val entity = vaultDao.getVaultFileById(id) ?: return@withContext
        vaultDao.updateVaultFile(entity.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
        logAction("TRASH", "Moved ${entity.originalName} to trash")
    }

    suspend fun restoreFromTrash(id: Long) = withContext(Dispatchers.IO) {
        val entity = vaultDao.getVaultFileById(id) ?: return@withContext
        vaultDao.updateVaultFile(entity.copy(isDeleted = false, deletedAt = null))
        logAction("RESTORE_TRASH", "Restored ${entity.originalName} from trash")
    }

    suspend fun deletePermanently(id: Long) = withContext(Dispatchers.IO) {
        val entity = vaultDao.getVaultFileById(id) ?: return@withContext
        val encryptedFile = File(entity.encryptedPath)
        SecureDeleteManager.secureDelete(encryptedFile)
        vaultDao.deleteVaultFile(entity)
        logAction("DELETE", "Permanently deleted ${entity.originalName}")
    }

    suspend fun exportBackup(targetUri: Uri) = withContext(Dispatchers.IO) {
        // Implementation for creating an encrypted backup package
        // This would involve ZIP-ing the Vault directory and encrypting the metadata
    }

    suspend fun importBackup(sourceUri: Uri) = withContext(Dispatchers.IO) {
        // Implementation for importing and decrypting a backup package
    }

    private suspend fun logAction(action: String, details: String) {
        vaultDao.insertAuditLog(VaultAuditLog(action = action, details = details))
    }

    private fun getFileInfo(uri: Uri): Pair<String, Long> {
        if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            if (file.exists()) {
                return file.name to file.length()
            }
        }

        var name = "Unknown_${System.currentTimeMillis()}"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
        return name to size
    }

    private fun getUniqueFile(directory: File, fileName: String): File {
        var file = File(directory, fileName)
        if (!file.exists()) return file
        val name = fileName.substringBeforeLast(".")
        val ext = fileName.substringAfterLast(".", "")
        val extension = if (ext.isNotEmpty()) ".$ext" else ""
        var counter = 1
        while (file.exists()) {
            file = File(directory, "$name($counter)$extension")
            counter++
        }
        return file
    }
}
