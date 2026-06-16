package com.sumitupdat.universalfileeditorviewer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransferStatus {
    PENDING, CONNECTING, TRANSFERRING, COMPLETED, FAILED, CANCELLED
}

enum class TransferType {
    SEND, RECEIVE
}

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val fileSize: Long,
    val deviceName: String,
    val transferType: TransferType,
    val timestamp: Long = System.currentTimeMillis(),
    val transferStatus: TransferStatus = TransferStatus.PENDING,
    val transferSpeed: Long = 0, // In bytes/sec
    val checksum: String? = null,
    val progress: Float = 0f,
    val filePath: String? = null
)
