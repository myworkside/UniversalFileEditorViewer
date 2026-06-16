package com.sumitupdat.universalfileeditorviewer.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files WHERE isDeleted = 0 ORDER BY " +
            "CASE WHEN :sort = 'NAME' THEN originalName END ASC, " +
            "CASE WHEN :sort = 'SIZE' THEN fileSize END DESC, " +
            "CASE WHEN :sort = 'DATE' THEN createdAt END DESC")
    fun getAllVaultFiles(sort: String): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE isDeleted = 0 AND originalName LIKE '%' || :query || '%'")
    fun searchVaultFiles(query: String): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashFiles(): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE id = :id")
    suspend fun getVaultFileById(id: Long): VaultFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultFile(vaultFile: VaultFileEntity): Long

    @Update
    suspend fun updateVaultFile(vaultFile: VaultFileEntity)

    @Delete
    suspend fun deleteVaultFile(vaultFile: VaultFileEntity)

    @Query("SELECT SUM(fileSize) FROM vault_files WHERE isDeleted = 0")
    fun getTotalVaultSize(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM vault_files WHERE isDeleted = 0")
    fun getVaultFileCount(): Flow<Int>

    // Audit Logs
    @Insert
    suspend fun insertAuditLog(log: VaultAuditLog)

    @Query("SELECT * FROM vault_audit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentAuditLogs(): Flow<List<VaultAuditLog>>

    // Auto-cleanup for trash (older than 30 days)
    @Query("SELECT * FROM vault_files WHERE isDeleted = 1 AND deletedAt < :expiryTime")
    suspend fun getExpiredTrashFiles(expiryTime: Long): List<VaultFileEntity>
}
