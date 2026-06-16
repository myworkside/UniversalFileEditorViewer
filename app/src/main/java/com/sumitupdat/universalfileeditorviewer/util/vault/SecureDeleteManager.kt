package com.sumitupdat.universalfileeditorviewer.util.vault

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom

object SecureDeleteManager {
    private val random = SecureRandom()

    /**
     * Securely deletes a file by overwriting it with random data before deletion.
     * This prevents recovery of the encrypted fragments.
     */
    suspend fun secureDelete(file: File) = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext

        try {
            val length = file.length()
            val raf = RandomAccessFile(file, "rws")
            
            // Pass 1: Zero out
            val buffer = ByteArray(64 * 1024) // 64KB buffer
            var pos = 0L
            while (pos < length) {
                val toWrite = (length - pos).coerceAtMost(buffer.size.toLong()).toInt()
                raf.write(ByteArray(toWrite))
                pos += toWrite
            }
            raf.getFD().sync()

            // Pass 2: Random data
            raf.seek(0)
            pos = 0L
            while (pos < length) {
                val toWrite = (length - pos).coerceAtMost(buffer.size.toLong()).toInt()
                random.nextBytes(buffer)
                raf.write(buffer, 0, toWrite)
                pos += toWrite
            }
            raf.getFD().sync()
            raf.close()

            // Final deletion
            file.delete()
        } catch (e: Exception) {
            // Fallback to normal delete if secure overwrite fails
            file.delete()
        }
    }
}
