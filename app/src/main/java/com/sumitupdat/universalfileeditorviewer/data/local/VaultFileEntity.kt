package com.sumitupdat.universalfileeditorviewer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalName: String,
    val encryptedPath: String,
    val fileSize: Long,
    val category: String,
    val iv: ByteArray,
    val checksum: String, // SHA-256 for integrity verification
    val keyVersion: Int = 2,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_audit_logs")
data class VaultAuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val action: String, // UNLOCK, RESTORE, DELETE, ADD, FAILED_AUTH
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
