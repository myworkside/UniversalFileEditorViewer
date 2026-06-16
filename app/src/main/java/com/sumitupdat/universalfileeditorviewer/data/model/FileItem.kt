package com.sumitupdat.universalfileeditorviewer.data.model

import java.io.File

enum class FileCategory {
    DOCUMENTS, IMAGES, AUDIO, VIDEO, ARCHIVES, CODE, DATABASES, FONTS, 
    CONFIGURATION, LOGS, BACKUP, ANDROID, LINUX, WINDOWS, MACOS, WEB,
    SPREADSHEETS, PRESENTATIONS, EMAILS, GIS, OTHER, SEARCH, FAVORITES, RECENT
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val extension: String = "",
    val isFavorite: Boolean = false,
    val category: FileCategory = FileCategory.OTHER
)

fun File.toFileItem(): FileItem {
    val ext = this.extension.lowercase()
    return FileItem(
        name = this.name,
        path = this.absolutePath,
        isDirectory = this.isDirectory,
        size = if (this.isDirectory) 0 else this.length(),
        lastModified = this.lastModified(),
        extension = ext,
        category = getCategoryFromExtension(ext, this.isDirectory)
    )
}

fun getCategoryFromExtension(extension: String, isDirectory: Boolean): FileCategory {
    if (isDirectory) return FileCategory.OTHER
    
    // Check specific categories first to avoid overlap
    val ext = extension.lowercase()
    
    return when (ext) {
        "xlsx", "xls", "csv" -> FileCategory.SPREADSHEETS
        "pptx", "ppt" -> FileCategory.PRESENTATIONS
        "eml", "msg" -> FileCategory.EMAILS
        "kml", "gpx", "geojson" -> FileCategory.GIS
        "txt", "pdf", "docx", "rtf", "odt" -> FileCategory.DOCUMENTS
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "svg", "tiff" -> FileCategory.IMAGES
        "mp3", "wav", "aac", "flac", "ogg", "m4a" -> FileCategory.AUDIO
        "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp" -> FileCategory.VIDEO
        "zip", "rar", "7z", "tar", "gz", "iso" -> FileCategory.ARCHIVES
        "java", "kt", "py", "cpp", "c", "php", "json", "xml", "yaml", "js", "ts", "cs", "rb", "go", "rs" -> FileCategory.CODE
        "db", "sqlite", "mdb", "sql", "accdb" -> FileCategory.DATABASES
        "ttf", "otf", "woff", "woff2" -> FileCategory.FONTS
        "ini", "cfg", "conf" -> FileCategory.CONFIGURATION
        "log", "evt", "trace" -> FileCategory.LOGS
        "bak", "old", "tmp" -> FileCategory.BACKUP
        "apk", "aab", "dex", "obb" -> FileCategory.ANDROID
        "sh", "bash", "run" -> FileCategory.LINUX
        "bat", "cmd", "ps1", "vbs" -> FileCategory.WINDOWS
        "app", "plist", "pkg" -> FileCategory.MACOS
        "html", "css" -> FileCategory.WEB
        else -> FileCategory.OTHER
    }
}
