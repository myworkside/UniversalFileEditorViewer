package com.sumitupdat.universalfileeditorviewer.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

object SecurityUtils {
    
    /**
     * Calculates SHA-256 checksum of an InputStream.
     * Processes in chunks to support large files without OOM.
     */
    fun calculateChecksum(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Sanitizes a filename to prevent path traversal and remove illegal characters.
     */
    fun sanitizeFileName(fileName: String): String {
        // Remove path components
        val nameOnly = File(fileName).name
        
        // Remove illegal characters for most file systems
        return nameOnly.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim()
            .takeIf { it.isNotEmpty() } ?: "received_file_${System.currentTimeMillis()}"
    }

    /**
     * Validates that the target directory is within the app's allowed storage.
     */
    fun isPathSafe(file: File, allowedBaseDir: File): Boolean {
        val canonicalPath = file.canonicalPath
        val allowedPath = allowedBaseDir.canonicalPath
        return canonicalPath.startsWith(allowedPath)
    }
}
