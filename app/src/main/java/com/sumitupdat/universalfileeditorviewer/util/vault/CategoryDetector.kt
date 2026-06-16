package com.sumitupdat.universalfileeditorviewer.util.vault

import android.webkit.MimeTypeMap

object CategoryDetector {
    fun detectCategory(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
        
        return when {
            mimeType.startsWith("image/") -> "Images"
            mimeType.startsWith("video/") -> "Videos"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType == "application/pdf" -> "Documents"
            extension in listOf("zip", "rar", "7z", "tar", "gz") -> "Archives"
            extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt") -> "Documents"
            else -> "Documents"
        }
    }
}
