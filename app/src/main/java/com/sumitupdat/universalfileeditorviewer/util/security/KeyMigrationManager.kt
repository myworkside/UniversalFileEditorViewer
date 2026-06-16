package com.sumitupdat.universalfileeditorviewer.util.security

import android.content.Context
import com.sumitupdat.universalfileeditorviewer.data.local.VaultFileDao
import com.sumitupdat.universalfileeditorviewer.data.local.VaultFileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyMigrationManager @Inject constructor(
    private val vaultDao: VaultFileDao,
    @ApplicationContext private val context: Context
) {
    /**
     * Checks for files encrypted with older key versions and re-encrypts them.
     */
    suspend fun migrateIfNeeded() = withContext(Dispatchers.IO) {
        // Implementation for scanning and migrating files to LATEST_KEY_VERSION
    }

    private suspend fun reEncryptFile(entity: VaultFileEntity) {
        val oldFile = File(entity.encryptedPath)
        val tempFile = File(context.cacheDir, UUID.randomUUID().toString())
        val newEncryptedFile = File(oldFile.parent, UUID.randomUUID().toString() + ".enc")

        try {
            // Decrypt with old key
            FileInputStream(oldFile).use { input ->
                FileOutputStream(tempFile).use { output ->
                    VaultManager.decryptStream(input, output, entity.iv, entity.keyVersion)
                }
            }

            // Encrypt with new key
            val newIv = FileInputStream(tempFile).use { input ->
                FileOutputStream(newEncryptedFile).use { output ->
                    VaultManager.encryptStream(input, output, VaultManager.LATEST_KEY_VERSION)
                }
            }

            // Update DB
            vaultDao.updateVaultFile(entity.copy(
                encryptedPath = newEncryptedFile.absolutePath,
                iv = newIv,
                keyVersion = VaultManager.LATEST_KEY_VERSION
            ))

            oldFile.delete()
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }
}
