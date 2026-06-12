package com.sumitupdat.universalfileeditorviewer.domain.repository

import android.content.Context
import android.util.Log
import com.github.junrar.Archive
import com.sumitupdat.universalfileeditorviewer.data.model.ArchiveEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ArchiveRepository"

class ArchiveRepository(private val context: Context) {

    private fun resolveSafeOutputFile(targetDir: File, entryFileName: String): File {
        val canonicalTargetDir = targetDir.canonicalFile
        val outFile = File(canonicalTargetDir, entryFileName).canonicalFile
        if (!outFile.toPath().startsWith(canonicalTargetDir.toPath())) {
            throw SecurityException("Blocked archive entry outside target directory: $entryFileName")
        }
        return outFile
    }

    suspend fun getArchiveEntries(file: File): List<ArchiveEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ArchiveEntry>()
        try {
            val extension = file.extension.lowercase()
            when (extension) {
                "zip" -> {
                    ZipFile(file).use { zip ->
                        zip.entries.asSequence().forEach { entry ->
                            entries.add(ArchiveEntry(
                                name = entry.name.substringAfterLast('/').ifEmpty { entry.name },
                                path = entry.name,
                                isDirectory = entry.isDirectory,
                                size = entry.size,
                                compressedSize = entry.compressedSize,
                                lastModified = entry.time
                            ))
                        }
                    }
                }
                "rar" -> {
                    Archive(file).use { archive ->
                        archive.fileHeaders.forEach { header ->
                            entries.add(ArchiveEntry(
                                name = header.fileName.substringAfterLast('\\').substringAfterLast('/').ifEmpty { header.fileName },
                                path = header.fileName,
                                isDirectory = header.isDirectory,
                                size = header.fullUnpackSize,
                                compressedSize = header.packSize,
                                lastModified = header.mTime?.time ?: 0
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading archive: ${file.name}", e)
        }
        entries.sortedWith(compareByDescending<ArchiveEntry> { it.isDirectory }.thenBy { it.path.lowercase() })
    }

    suspend fun extractFile(archiveFile: File, entryPath: String, targetDir: File): File? = withContext(Dispatchers.IO) {
        try {
            val extension = archiveFile.extension.lowercase()
            if (extension == "zip") {
                ZipFile(archiveFile).use { zip ->
                    val entry = zip.getEntry(entryPath) ?: return@withContext null
                    val outFile = resolveSafeOutputFile(targetDir, entry.name.substringAfterLast('/'))
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    return@withContext outFile
                }
            } else if (extension == "rar") {
                Archive(archiveFile).use { archive ->
                    val header = archive.fileHeaders.find { it.fileName == entryPath } ?: return@withContext null
                    val outFile = resolveSafeOutputFile(targetDir, header.fileName.substringAfterLast('\\').substringAfterLast('/'))
                    FileOutputStream(outFile).use { output ->
                        archive.extractFile(header, output)
                    }
                    return@withContext outFile
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed", e)
        }
        null
    }
}
