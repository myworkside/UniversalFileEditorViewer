package com.sumitupdat.universalfileeditorviewer.data.local

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    // Appearance
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val isAmoled: Boolean = false,
    val useDynamicColors: Boolean = true,
    val accentColor: Int = 0xFF6200EE.toInt(),
    val fontSizeMultiplier: Float = 1.0f,
    val uiDensity: UiDensity = UiDensity.DEFAULT,

    // File Manager
    val defaultStartFolder: String = "Internal Storage",
    val showHiddenFiles: Boolean = false,
    val showFileExtensions: Boolean = true,
    val fileSorting: FileSortOrder = FileSortOrder.NAME_ASC,
    val thumbnailGeneration: Boolean = true,
    val recentFilesLimit: Int = 20,

    // Security
    val biometricEnabled: Boolean = false,
    val deviceCredentialEnabled: Boolean = false,
    val autoLockVault: Boolean = true,
    val autoLockTimeoutMinutes: Int = 5,
    val secureDelete: Boolean = false,

    // Wireless
    val autoReceiveFiles: Boolean = false,
    val backgroundTransfers: Boolean = true,
    val deviceDiscovery: Boolean = true,

    // Language & Region
    val language: String = "System",
    val dateFormat: String = "dd/MM/yyyy",
    val timeFormat: String = "24h"
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class UiDensity { COMPACT, DEFAULT, COZY }
enum class FileSortOrder { NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC }
