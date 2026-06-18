package com.sumitupdat.universalfileeditorviewer.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumitupdat.universalfileeditorviewer.data.local.*
import com.sumitupdat.universalfileeditorviewer.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        Log.d(TAG, "Loading preferences...")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.userPreferences
                .onEach { prefs ->
                    Log.d(TAG, "Preferences loaded successfully: $prefs")
                    _uiState.update { it.copy(preferences = prefs, isLoading = false) }
                }
                .catch { e ->
                    Log.e(TAG, "Error loading preferences from DataStore", e)
                    _uiState.update { it.copy(error = "Failed to load settings: ${e.message}", isLoading = false) }
                }
                .collect()
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch {
            Log.d(TAG, "Saving ThemeMode: $mode")
            repository.updatePreferences { it.copy(theme = mode) }
        }
    }

    fun toggleAmoled(enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Saving AMOLED: $enabled")
            repository.updatePreferences { it.copy(isAmoled = enabled) }
        }
    }

    fun toggleDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Dynamic Colors: $enabled")
            repository.updatePreferences { it.copy(useDynamicColors = enabled) }
        }
    }

    fun updateAccentColor(color: Int) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Accent Color: $color")
            repository.updatePreferences { it.copy(accentColor = color) }
        }
    }

    fun updateFontSize(multiplier: Float) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Font Size Multiplier: $multiplier")
            repository.updatePreferences { it.copy(fontSizeMultiplier = multiplier) }
        }
    }

    fun updateUiDensity(density: UiDensity) {
        viewModelScope.launch {
            Log.d(TAG, "Saving UI Density: $density")
            repository.updatePreferences { it.copy(uiDensity = density) }
        }
    }

    fun toggleShowHiddenFiles(show: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Show Hidden Files: $show")
            repository.updatePreferences { it.copy(showHiddenFiles = show) }
        }
    }

    fun toggleShowExtensions(show: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Show Extensions: $show")
            repository.updatePreferences { it.copy(showFileExtensions = show) }
        }
    }

    fun updateFileSorting(order: FileSortOrder) {
        viewModelScope.launch {
            Log.d(TAG, "Saving File Sort Order: $order")
            repository.updatePreferences { it.copy(fileSorting = order) }
        }
    }

    fun toggleThumbnailGeneration(enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Thumbnail Generation: $enabled")
            repository.updatePreferences { it.copy(thumbnailGeneration = enabled) }
        }
    }

    fun updateRecentFilesLimit(limit: Int) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Recent Files Limit: $limit")
            repository.updatePreferences { it.copy(recentFilesLimit = limit) }
        }
    }

    fun updateBiometric(enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Biometric Enabled: $enabled")
            repository.updatePreferences { it.copy(biometricEnabled = enabled) }
        }
    }

    fun updateDeviceCredential(enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Device Credential Enabled: $enabled")
            repository.updatePreferences { it.copy(deviceCredentialEnabled = enabled) }
        }
    }

    fun toggleAutoLockVault(enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Auto Lock Vault: $enabled")
            repository.updatePreferences { it.copy(autoLockVault = enabled) }
        }
    }

    fun updateAutoLockTimeout(minutes: Int) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Auto Lock Timeout: $minutes")
            repository.updatePreferences { it.copy(autoLockTimeoutMinutes = minutes) }
        }
    }

    fun toggleSecureDelete(enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Secure Delete: $enabled")
            repository.updatePreferences { it.copy(secureDelete = enabled) }
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            Log.d(TAG, "Saving Language: $lang")
            repository.updatePreferences { it.copy(language = lang) }
        }
    }

    fun clearCache() {
        Log.d(TAG, "Clearing app cache...")
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                context.cacheDir.deleteRecursively()
                Log.d(TAG, "Cache cleared successfully")
                _uiState.update { it.copy(message = "Cache cleared successfully", isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache", e)
                _uiState.update { it.copy(error = "Failed to clear cache: ${e.message}", isLoading = false) }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun exportSettings() {
        // Implementation for exporting settings JSON
        _uiState.update { it.copy(message = "Settings export not implemented yet") }
    }

    fun importSettings() {
        // Implementation for importing settings JSON
        _uiState.update { it.copy(message = "Settings import not implemented yet") }
    }
}
