package com.sumitupdat.universalfileeditorviewer.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumitupdat.universalfileeditorviewer.data.local.FavoriteFile
import com.sumitupdat.universalfileeditorviewer.data.local.RecentFile
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.data.model.toFileItem
import com.sumitupdat.universalfileeditorviewer.domain.repository.FileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "FileViewModel"

class FileViewModel(private val repository: FileRepository) : ViewModel() {

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _rawFiles = MutableStateFlow<List<FileItem>>(emptyList())
    
    private val _storageRoots = MutableStateFlow<List<FileItem>>(emptyList())
    val storageRoots: StateFlow<List<FileItem>> = _storageRoots.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val favorites: StateFlow<List<FavoriteFile>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFiles: StateFlow<List<RecentFile>> = repository.getRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<FileItem>> = combine(_rawFiles, favorites) { files, favs ->
        files.map { file ->
            file.copy(isFavorite = favs.any { it.path == file.path })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        loadStorageRoots()
    }

    fun loadStorageRoots() {
        Log.d(TAG, "Loading storage roots...")
        val roots = repository.getStorageRoots().map { it.toFileItem() }
        Log.d(TAG, "Found roots: ${roots.size}")
        _storageRoots.value = roots
        if (_currentPath.value.isEmpty() && roots.isNotEmpty()) {
            Log.d(TAG, "Setting initial path to: ${roots[0].path}")
            loadFiles(roots[0].path)
        } else if (_currentPath.value.isNotEmpty()) {
            Log.d(TAG, "Refreshing current path: ${_currentPath.value}")
            loadFiles(_currentPath.value)
        }
    }

    fun loadFiles(path: String) {
        viewModelScope.launch {
            Log.d(TAG, "loadFiles: $path")
            _isLoading.value = true
            _currentPath.value = path
            val result = repository.getFiles(path)
            Log.d(TAG, "loadFiles result size: ${result.size}")
            _rawFiles.value = result
            _isLoading.value = false
        }
    }

    fun navigateUp(): Boolean {
        if (_currentPath.value.isEmpty()) return false
        val currentFile = File(_currentPath.value)
        val parent = currentFile.parentFile
        
        val isRoot = _storageRoots.value.any { it.path == _currentPath.value }
        if (isRoot) return false

        return if (parent != null && parent.exists()) {
            loadFiles(parent.absolutePath)
            true
        } else {
            false
        }
    }

    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            if (repository.deleteFile(fileItem.path)) {
                loadFiles(_currentPath.value)
            }
        }
    }

    fun toggleFavorite(fileItem: FileItem) {
        viewModelScope.launch {
            repository.toggleFavorite(fileItem)
            // No need to call loadFiles, combine will handle UI update
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            if (repository.createFolder(_currentPath.value, name)) {
                loadFiles(_currentPath.value)
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            if (repository.createFile(_currentPath.value, name)) {
                loadFiles(_currentPath.value)
            }
        }
    }

    fun renameFile(fileItem: FileItem, newName: String) {
        viewModelScope.launch {
            if (repository.renameFile(fileItem.path, newName)) {
                loadFiles(_currentPath.value)
            }
        }
    }

    fun copyFile(source: FileItem, destinationPath: String) {
        viewModelScope.launch {
            if (repository.copyFile(source.path, destinationPath)) {
                loadFiles(_currentPath.value)
            }
        }
    }

    fun moveFile(source: FileItem, destinationPath: String) {
        viewModelScope.launch {
            if (repository.moveFile(source.path, destinationPath)) {
                loadFiles(_currentPath.value)
            }
        }
    }

    fun addRecentFile(fileItem: FileItem) {
        viewModelScope.launch {
            repository.addRecentFile(fileItem)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val allFiles = repository.getFiles(_currentPath.value)
            _rawFiles.value = if (query.isEmpty()) {
                allFiles
            } else {
                allFiles.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
    }
}
