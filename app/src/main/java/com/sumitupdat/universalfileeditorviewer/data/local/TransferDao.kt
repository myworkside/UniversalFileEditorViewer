package com.sumitupdat.universalfileeditorviewer.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY timestamp DESC")
    fun getAllTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getTransferById(id: Long): TransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity): Long

    @Update
    suspend fun updateTransfer(transfer: TransferEntity)

    @Delete
    suspend fun deleteTransfer(transfer: TransferEntity)

    @Query("SELECT SUM(fileSize) FROM transfers WHERE transferStatus = 'COMPLETED'")
    fun getTotalSharedData(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM transfers WHERE transferType = 'SEND' AND transferStatus = 'COMPLETED'")
    fun getTotalSentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transfers WHERE transferType = 'RECEIVE' AND transferStatus = 'COMPLETED'")
    fun getTotalReceivedCount(): Flow<Int>

    @Query("SELECT AVG(transferSpeed) FROM transfers WHERE transferStatus = 'COMPLETED' AND transferSpeed > 0")
    fun getAverageSpeed(): Flow<Double?>
}
