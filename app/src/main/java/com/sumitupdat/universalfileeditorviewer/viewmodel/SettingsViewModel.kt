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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.userPreferences
                .onEach { prefs ->
                    Log.d(TAG, "Preferences loaded: $prefs")
                    _uiState.update { it.copy(preferences = prefs, isLoading = false) }
                }
                .catch { e ->
                    Log.e(TAG, "Error loading preferences", e)
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect()
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch {
            Log.d(TAG, "Updating theme to $mode")
            repository.updatePreferences { it.copy(theme = mode) }
        }
    }

    fun toggleAmoled(enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Toggling AMOLED: $enabled")
            repository.updatePreferences { it.copy(isAmoled = enabled) }
        }
    }

    fun toggleDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Toggling Dynamic Colors: $enabled")
            repository.updatePreferences { it.copy(useDynamicColors = enabled) }
        }
    }

    fun updateAccentColor(color: Int) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(accentColor = color) }
        }
    }

    fun updateFontSize(multiplier: Float) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(fontSizeMultiplier = multiplier) }
        }
    }

    fun updateUiDensity(density: UiDensity) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(uiDensity = density) }
        }
    }

    fun toggleShowHiddenFiles(show: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(showHiddenFiles = show) }
        }
    }

    fun toggleShowExtensions(show: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(showFileExtensions = show) }
        }
    }

    fun updateFileSorting(order: FileSortOrder) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(fileSorting = order) }
        }
    }

    fun toggleThumbnailGeneration(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(thumbnailGeneration = enabled) }
        }
    }

    fun updateRecentFilesLimit(limit: Int) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(recentFilesLimit = limit) }
        }
    }

    fun updateBiometric(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(biometricEnabled = enabled) }
        }
    }

    fun updateDeviceCredential(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(deviceCredentialEnabled = enabled) }
        }
    }

    fun toggleAutoLockVault(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(autoLockVault = enabled) }
        }
    }

    fun updateAutoLockTimeout(minutes: Int) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(autoLockTimeoutMinutes = minutes) }
        }
    }

    fun toggleSecureDelete(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(secureDelete = enabled) }
        }
    }

    fun toggleAutoReceiveFiles(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(autoReceiveFiles = enabled) }
        }
    }

    fun toggleBackgroundTransfers(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(backgroundTransfers = enabled) }
        }
    }

    fun toggleDeviceDiscovery(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(deviceDiscovery = enabled) }
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(language = lang) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                context.cacheDir.deleteRecursively()
                _uiState.update { it.copy(message = "Cache cleared successfully", isLoading = false) }
            } catch (e: Exception) {
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
