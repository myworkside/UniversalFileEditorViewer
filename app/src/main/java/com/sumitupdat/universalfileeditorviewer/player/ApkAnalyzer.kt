package com.sumitupdat.universalfileeditorviewer.player

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.sumitupdat.universalfileeditorviewer.util.IDELogger
import java.io.File

data class ApkInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val apkSize: Long,
    val label: String
)

class ApkAnalyzer(private val context: Context) {
    private val TAG = "ApkAnalyzer"

    fun analyze(uri: Uri): ApkInfo? {
        var tempFile: File? = null
        return try {
            IDELogger.i(TAG, "Starting analysis for URI: $uri")
            
            // Create a unique temp file
            tempFile = File.createTempFile("analyze_", ".apk", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }

            val flags = PackageManager.GET_PERMISSIONS or 
                        PackageManager.GET_ACTIVITIES or 
                        PackageManager.GET_SERVICES or 
                        PackageManager.GET_RECEIVERS or 
                        PackageManager.GET_PROVIDERS

            val packageInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(tempFile.path, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                context.packageManager.getPackageArchiveInfo(tempFile.path, flags)
            }

            packageInfo?.let { pi ->
                val appInfo = pi.applicationInfo
                appInfo?.sourceDir = tempFile.path
                appInfo?.publicSourceDir = tempFile.path
                
                val label = try { pi.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: "Unknown" } catch (e: Exception) { "Unknown" }

                ApkInfo(
                    packageName = pi.packageName,
                    versionName = pi.versionName ?: "N/A",
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode else pi.versionCode.toLong(),
                    minSdk = appInfo?.minSdkVersion ?: 0,
                    targetSdk = appInfo?.targetSdkVersion ?: 0,
                    permissions = pi.requestedPermissions?.toList() ?: emptyList(),
                    activities = pi.activities?.map { it.name } ?: emptyList(),
                    services = pi.services?.map { it.name } ?: emptyList(),
                    receivers = pi.receivers?.map { it.name } ?: emptyList(),
                    providers = pi.providers?.map { it.name } ?: emptyList(),
                    apkSize = tempFile.length(),
                    label = label
                ).also {
                    IDELogger.i(TAG, "Analysis successful for ${it.packageName}")
                }
            }
        } catch (e: Exception) {
            IDELogger.e(TAG, "Analysis failed: ${e.message}")
            null
        } finally {
            tempFile?.delete()
            IDELogger.d(TAG, "Temp file cleaned up")
        }
    }
}
