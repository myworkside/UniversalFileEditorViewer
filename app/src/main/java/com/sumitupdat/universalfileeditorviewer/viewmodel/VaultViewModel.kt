package com.sumitupdat.universalfileeditorviewer.viewmodel

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumitupdat.universalfileeditorviewer.data.local.VaultAuditLog
import com.sumitupdat.universalfileeditorviewer.data.local.VaultFileEntity
import com.sumitupdat.universalfileeditorviewer.data.repository.VaultRepository
import com.sumitupdat.universalfileeditorviewer.util.security.VaultAuthManager
import com.sumitupdat.universalfileeditorviewer.util.security.VaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VaultUiState(
    val isLocked: Boolean = true,
    val vaultFiles: List<VaultFileEntity> = emptyList(),
    val trashFiles: List<VaultFileEntity> = emptyList(),
    val auditLogs: List<VaultAuditLog> = emptyList(),
    val totalSize: Long = 0,
    val fileCount: Int = 0,
    val categoryCounts: Map<String, Int> = emptyMap(),
    val error: String? = null,
    val message: String? = null,
    val isLoading: Boolean = false,
    val failedAttempts: Int = 0,
    val isBruteForceLocked: Boolean = false,
    val sortOrder: String = "DATE",
    val searchQuery: String = ""
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultRepository,
    private val authManager: VaultAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var collectionJob: Job? = null

    init {
        if (!VaultManager.isKeyHealthy()) {
            _uiState.update { it.copy(error = "Vault encryption key compromised. Data is inaccessible.") }
        }
        observeVaultData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeVaultData() {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            val filesFlow = uiState.flatMapLatest { state ->
                if (state.searchQuery.isEmpty()) repository.getFiles(state.sortOrder)
                else repository.searchFiles(state.searchQuery)
            }

            combine(
                filesFlow,
                repository.getTrashFiles(),
                repository.getTotalVaultSize(),
                repository.getVaultFileCount(),
                repository.getAuditLogs()
            ) { files, trash, size, count, logs ->
                val counts = files.groupBy { it.category }.mapValues { it.value.size }
                _uiState.update { it.copy(
                    vaultFiles = files,
                    trashFiles = trash,
                    totalSize = size ?: 0,
                    fileCount = count,
                    categoryCounts = counts,
                    auditLogs = logs
                ) }
            }.collect()
        }
    }

    fun setSortOrder(order: String) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun authenticate(activity: FragmentActivity, onAuthenticated: () -> Unit = {}) {
        if (_uiState.value.isBruteForceLocked) {
            _uiState.update { it.copy(error = "Too many failed attempts. Try again later.") }
            return
        }

        authManager.authenticate(
            activity = activity,
            onSuccess = {
                _uiState.update { it.copy(isLocked = false, error = null, failedAttempts = 0) }
                onAuthenticated()
            },
            onError = { msg ->
                val newFailedCount = _uiState.value.failedAttempts + 1
                _uiState.update { it.copy(error = msg, failedAttempts = newFailedCount) }
                if (newFailedCount >= 5) {
                    handleBruteForce()
                }
            }
        )
    }

    private fun handleBruteForce() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBruteForceLocked = true) }
            delay(30000) // 30 second lockout
            _uiState.update { it.copy(isBruteForceLocked = false, failedAttempts = 0) }
        }
    }

    fun lockVault() {
        _uiState.update { it.copy(isLocked = true) }
    }

    fun addFileToVault(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.moveToVault(uri)
                _uiState.update { it.copy(message = "File encrypted successfully") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Encryption failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun restoreFile(activity: FragmentActivity, id: Long, targetDir: File) {
        authenticate(activity) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.restoreFromVault(id, targetDir)
                result.onSuccess { 
                    _uiState.update { it.copy(message = "Restored as $it", isLoading = false) }
                }.onFailure { 
                    _uiState.update { it.copy(error = it.message, isLoading = false) }
                }
            }
        }
    }

    fun moveToTrash(id: Long) {
        viewModelScope.launch {
            repository.moveToTrash(id)
        }
    }

    fun restoreFromTrash(id: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(id)
        }
    }

    fun deletePermanently(activity: FragmentActivity, id: Long) {
        authenticate(activity) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                repository.deletePermanently(id)
                _uiState.update { it.copy(message = "Deleted permanently", isLoading = false) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
