package com.sumitupdat.universalfileeditorviewer.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumitupdat.universalfileeditorviewer.data.local.VaultAuditLog
import com.sumitupdat.universalfileeditorviewer.data.local.VaultFileEntity
import com.sumitupdat.universalfileeditorviewer.data.repository.VaultRepository
import com.sumitupdat.universalfileeditorviewer.util.security.VaultSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VaultUiState(
    val isLocked: Boolean = true,
    val isPinSet: Boolean = false,
    val remainingAttempts: Int = 5,
    val lockoutUntil: Long = 0,
    val vaultFiles: List<VaultFileEntity> = emptyList(),
    val trashFiles: List<VaultFileEntity> = emptyList(),
    val auditLogs: List<VaultAuditLog> = emptyList(),
    val totalSize: Long = 0,
    val fileCount: Int = 0,
    val categoryCounts: Map<String, Int> = emptyMap(),
    val error: String? = null,
    val message: String? = null,
    val isLoading: Boolean = false,
    val sortOrder: String = "DATE",
    val searchQuery: String = ""
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultRepository,
    private val securityManager: VaultSecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var collectionJob: Job? = null

    init {
        checkPinStatus()
        observeVaultData()
    }

    private fun checkPinStatus() {
        viewModelScope.launch {
            val isSet = securityManager.isPinSet()
            _uiState.update { it.copy(isPinSet = isSet) }
        }
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

    fun setVaultPin(pin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (securityManager.setPin(pin)) {
                _uiState.update { it.copy(isPinSet = true, message = "PIN set successfully", isLoading = false) }
            }
        }
    }

    fun unlockWithPin(pin: String) {
        viewModelScope.launch {
            Log.d("VAULT", "PIN entered for verification")
            _uiState.update { it.copy(isLoading = true) }
            
            val result = securityManager.verifyPin(pin)
            Log.d("VAULT", "Verification result: $result")

            when (result) {
                is VaultSecurityManager.PinVerificationResult.Success -> {
                    Log.d("VAULT", "Vault unlocked successfully")
                    _uiState.update { it.copy(
                        isLocked = false, 
                        remainingAttempts = 5, 
                        isLoading = false, 
                        error = null 
                    ) }
                }
                is VaultSecurityManager.PinVerificationResult.Invalid -> {
                    _uiState.update { it.copy(
                        remainingAttempts = result.remainingAttempts,
                        error = "Incorrect PIN. ${result.remainingAttempts} attempts remaining.",
                        isLoading = false
                    ) }
                }
                is VaultSecurityManager.PinVerificationResult.Locked -> {
                    _uiState.update { it.copy(
                        lockoutUntil = result.until,
                        error = "Vault locked due to multiple failed attempts.",
                        isLoading = false
                    ) }
                }
                is VaultSecurityManager.PinVerificationResult.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
            }
        }
    }

    fun lockVault() {
        _uiState.update { it.copy(isLocked = true) }
    }

    fun setSortOrder(order: String) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
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

    fun restoreFile(id: Long, targetDir: File) {
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

    fun deletePermanently(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.deletePermanently(id)
            _uiState.update { it.copy(message = "Deleted permanently", isLoading = false) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
