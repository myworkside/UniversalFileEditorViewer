package com.sumitupdat.universalfileeditorviewer.util

data class TransferProgress(
    val transferId: Long,
    val fileName: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speed: Long, // bytes per second
    val remainingTimeMillis: Long,
    val status: String
) {
    val percentage: Float get() = if (totalBytes > 0) bytesTransferred.toFloat() / totalBytes else 0f
}
