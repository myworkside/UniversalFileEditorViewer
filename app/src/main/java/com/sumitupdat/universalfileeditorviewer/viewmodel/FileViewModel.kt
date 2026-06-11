package com.sumitupdat.universalfileeditorviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.domain.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FileViewModel(private val repository: FileRepository) : ViewModel() {

    private val _currentPath = MutableStateFlow(File("/storage/emulated/0").absolutePath)
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFiles(_currentPath.value)
    }

    fun loadFiles(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentPath.value = path
            _files.value = repository.getFiles(path)
            _isLoading.value = false
        }
    }

    fun navigateUp(): Boolean {
        val currentFile = File(_currentPath.value)
        val parent = currentFile.parentFile
        return if (parent != null && parent.exists() && parent.canRead()) {
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
            // No need to reload files as favorites might be shown elsewhere
            // but we might want to refresh the current list if it shows favorite status
            loadFiles(_currentPath.value)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            if (repository.createFolder(_currentPath.value, name)) {
                loadFiles(_currentPath.value)
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        // Filter current files based on query
        viewModelScope.launch {
            val allFiles = repository.getFiles(_currentPath.value)
            _files.value = if (query.isEmpty()) {
                allFiles
            } else {
                allFiles.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
    }
}
