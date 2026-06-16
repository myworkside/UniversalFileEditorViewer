package com.sumitupdat.universalfileeditorviewer.data.repository

import com.sumitupdat.universalfileeditorviewer.data.local.TransferDao
import com.sumitupdat.universalfileeditorviewer.data.local.TransferEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepository @Inject constructor(
    private val transferDao: TransferDao
) {
    val allTransfers: Flow<List<TransferEntity>> = transferDao.getAllTransfers()
    val totalSharedData: Flow<Long?> = transferDao.getTotalSharedData()
    val totalSentCount: Flow<Int> = transferDao.getTotalSentCount()
    val totalReceivedCount: Flow<Int> = transferDao.getTotalReceivedCount()
    val averageSpeed: Flow<Double?> = transferDao.getAverageSpeed()

    suspend fun insertTransfer(transfer: TransferEntity): Long {
        return transferDao.insertTransfer(transfer)
    }

    suspend fun updateTransfer(transfer: TransferEntity) {
        transferDao.updateTransfer(transfer)
    }

    suspend fun deleteTransfer(transfer: TransferEntity) {
        transferDao.deleteTransfer(transfer)
    }
    
    suspend fun getTransferById(id: Long): TransferEntity? {
        return transferDao.getTransferById(id)
    }
}
