package com.sumitupdat.universalfileeditorviewer.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumitupdat.universalfileeditorviewer.data.local.VaultAuditLog
import com.sumitupdat.universalfileeditorviewer.data.local.VaultFileEntity
import com.sumitupdat.universalfileeditorviewer.data.repository.VaultRepository
import com.sumitupdat.universalfileeditorviewer.util.security.VaultSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val searchQuery: String = "",
    val viewingFile: File? = null,
    val viewingMimeType: String? = null
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
            Log.d("VAULT_VM", "PIN entered for verification")
            _uiState.update { it.copy(isLoading = true) }
            
            val result = securityManager.verifyPin(pin)
            Log.d("VAULT_VM", "Verification result: $result")

            when (result) {
                is VaultSecurityManager.PinVerificationResult.Success -> {
                    Log.d("VAULT_VM", "Vault unlocked successfully")
                    _uiState.update { it.copy(
                        isLocked = false, 
                        remainingAttempts = 5, 
                        isLoading = false, 
                        error = null 
                    ) }
                }
                is VaultSecurityManager.PinVerificationResult.Invalid -> {
                    Log.w("VAULT_VM", "Invalid PIN attempt. Remaining: ${result.remainingAttempts}")
                    _uiState.update { it.copy(
                        remainingAttempts = result.remainingAttempts,
                        error = "Incorrect PIN. ${result.remainingAttempts} attempts remaining.",
                        isLoading = false
                    ) }
                }
                is VaultSecurityManager.PinVerificationResult.Locked -> {
                    Log.e("VAULT_VM", "Vault locked until ${result.until}")
                    _uiState.update { it.copy(
                        lockoutUntil = result.until,
                        error = "Vault locked due to multiple failed attempts.",
                        isLoading = false
                    ) }
                }
                is VaultSecurityManager.PinVerificationResult.Error -> {
                    Log.e("VAULT_VM", "Verification error: ${result.message}")
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
            }
        }
    }

    fun lockVault() {
        Log.d("VAULT_VM", "Vault locked manually/auto")
        repository.clearPreviewCache()
        _uiState.update { it.copy(isLocked = true, viewingFile = null, viewingMimeType = null) }
    }

    fun setSortOrder(order: String) {
        Log.d("VAULT_VM", "Sort order changed: $order")
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addFileToVault(uri: Uri) {
        viewModelScope.launch {
            Log.d("VAULT_VM", "Adding file to vault: $uri")
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.moveToVault(uri)
                Log.d("VAULT_VM", "File added successfully")
                _uiState.update { it.copy(message = "File encrypted successfully") }
            } catch (e: Exception) {
                Log.e("VAULT_VM", "Failed to add file", e)
                _uiState.update { it.copy(error = "Encryption failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun restoreFile(id: Long, targetDir: File) {
        viewModelScope.launch {
            Log.d("VAULT_VM", "Restoring file ID: $id to $targetDir")
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.restoreFromVault(id, targetDir)
            result.onSuccess { 
                Log.d("VAULT_VM", "Restored as $it")
                _uiState.update { it.copy(message = "Restored as $it", isLoading = false) }
            }.onFailure { 
                Log.e("VAULT_VM", "Restore failed", it)
                _uiState.update { it.copy(error = it.message, isLoading = false) }
            }
        }
    }

    fun moveToTrash(id: Long) {
        viewModelScope.launch {
            Log.d("VAULT_VM", "Moving to trash: $id")
            repository.moveToTrash(id)
        }
    }

    fun restoreFromTrash(id: Long) {
        viewModelScope.launch {
            Log.d("VAULT_VM", "Restoring from trash: $id")
            repository.restoreFromTrash(id)
        }
    }

    fun deletePermanently(id: Long) {
        viewModelScope.launch {
            Log.d("VAULT_VM", "Deleting permanently: $id")
            _uiState.update { it.copy(isLoading = true) }
            repository.deletePermanently(id)
            _uiState.update { it.copy(message = "Deleted permanently", isLoading = false) }
        }
    }

    fun openVaultFile(id: Long) {
        viewModelScope.launch {
            Log.d("VAULT", "Opening vault file ID: $id")
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = repository.decryptToCache(id)
            result.onSuccess { file ->
                Log.d("VAULT", "Decrypt success: ${file.path}")
                val extension = file.extension.lowercase()
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) 
                    ?: when(extension) {
                        "json" -> "application/json"
                        "xml" -> "application/xml"
                        "kt", "java", "py", "cpp", "c", "sh", "bat" -> "text/plain"
                        "md" -> "text/markdown"
                        else -> "text/plain" // Default to text for internal viewing
                    }
                
                Log.d("VAULT", "Viewer launching for MimeType: $mimeType")
                _uiState.update { it.copy(
                    viewingFile = file, 
                    viewingMimeType = mimeType, 
                    isLoading = false 
                ) }
            }.onFailure { exception ->
                Log.e("VAULT", "Open failed", exception)
                _uiState.update { it.copy(
                    error = "Could not open file: ${exception.message}", 
                    isLoading = false 
                ) }
            }
        }
    }

    fun closeViewer() {
        Log.d("VAULT_VM", "Closing internal viewer")
        repository.clearPreviewCache()
        _uiState.update { it.copy(viewingFile = null, viewingMimeType = null) }
    }

    override fun onCleared() {
        super.onCleared()
        repository.clearPreviewCache()
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
