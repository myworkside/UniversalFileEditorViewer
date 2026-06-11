package com.sumitupdat.universalfileeditorviewer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumitupdat.universalfileeditorviewer.data.local.FavoriteFile
import com.sumitupdat.universalfileeditorviewer.data.local.RecentFile
import com.sumitupdat.universalfileeditorviewer.data.model.FileCategory
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.data.model.getCategoryFromExtension
import com.sumitupdat.universalfileeditorviewer.data.model.toFileItem
import com.sumitupdat.universalfileeditorviewer.domain.repository.FileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FileViewModel(private val repository: FileRepository) : ViewModel() {

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _rawFiles = MutableStateFlow<List<FileItem>>(emptyList())
    
    private val _storageRoots = MutableStateFlow<List<FileItem>>(emptyList())
    val storageRoots: StateFlow<List<FileItem>> = _storageRoots.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    val favorites: StateFlow<List<FavoriteFile>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFiles: StateFlow<List<RecentFile>> = repository.getRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category counts for Dashboard
    val categoryCounts = FileCategory.entries.associateWith { category ->
        repository.getCountByCategory(category).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Main source of files for the UI
    val files: StateFlow<List<FileItem>> = combine(
        _currentPath,
        _selectedCategory,
        _searchQuery,
        _rawFiles,
        favorites
    ) { path, category, query, rawFiles, favs ->
        val sourceFiles = when {
            query.isNotEmpty() -> {
                // If searching, we don't have a live global search flow here easily, 
                // but we can use the globalSearchResults or just filter locally if query is small.
                // For production-ready, we should use a dedicated search flow.
                emptyList() // We'll handle search separately
            }
            category != null -> {
                // We need a way to get all files of this category from the DB as a Flow
                // but combine expects static lists or we must flatten.
                // Let's simplify: if category is set, we'll manually update _rawFiles.
                rawFiles
            }
            else -> rawFiles
        }
        
        sourceFiles.map { file ->
            file.copy(isFavorite = favs.any { it.path == file.path })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Global Search results from index
    val globalSearchResults: StateFlow<List<FileItem>> = _searchQuery
        .debounce(300)
        .filter { it.length >= 2 }
        .flatMapLatest { query -> repository.searchIndexedFiles(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
        startIndexing()
    }

    fun refresh() {
        loadStorageRoots()
    }

    fun startIndexing() {
        viewModelScope.launch {
            _isScanning.value = true
            repository.startFullScan()
            _isScanning.value = false
        }
    }

    fun selectCategory(category: FileCategory) {
        _selectedCategory.value = category
        _currentPath.value = ""
        _searchQuery.value = ""
        
        viewModelScope.launch {
            _isLoading.value = true
            when (category) {
                FileCategory.FAVORITES -> {
                    repository.getFavorites().collectLatest { favList ->
                        _rawFiles.value = favList.map { it.toFileItem() }
                        _isLoading.value = false
                    }
                }
                FileCategory.RECENT -> {
                    repository.getRecentFiles().collectLatest { recentList ->
                        _rawFiles.value = recentList.map { it.toFileItem() }
                        _isLoading.value = false
                    }
                }
                FileCategory.SEARCH -> {
                    // Search all handled by the SearchBar and globalSearchResults flow
                    _rawFiles.value = emptyList()
                    _isLoading.value = false
                }
                else -> {
                    repository.getFilesByCategory(category).collectLatest { categoryFiles ->
                        _rawFiles.value = categoryFiles
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    private fun FavoriteFile.toFileItem(): FileItem = FileItem(
        name = name,
        path = path,
        isDirectory = isDirectory,
        extension = File(path).extension,
        category = getCategoryFromExtension(File(path).extension, isDirectory),
        isFavorite = true
    )

    private fun RecentFile.toFileItem(): FileItem = FileItem(
        name = name,
        path = path,
        isDirectory = isDirectory,
        extension = File(path).extension,
        category = getCategoryFromExtension(File(path).extension, isDirectory)
    )

    fun loadStorageRoots() {
        val roots = repository.getStorageRoots().map { it.toFileItem() }
        _storageRoots.value = roots
        if (_currentPath.value.isEmpty() && roots.isNotEmpty() && _selectedCategory.value == null) {
            loadFiles(roots[0].path)
        } else if (_currentPath.value.isNotEmpty()) {
            loadFiles(_currentPath.value)
        }
    }

    fun loadFiles(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentPath.value = path
            _selectedCategory.value = null
            _searchQuery.value = ""
            val result = repository.getFiles(path)
            _rawFiles.value = result
            _isLoading.value = false
        }
    }

    fun navigateUp(): Boolean {
        if (_selectedCategory.value != null) {
            _selectedCategory.value = null
            loadStorageRoots()
            return true
        }
        
        if (_currentPath.value.isEmpty()) return false
        val currentFile = File(_currentPath.value)
        val parent = currentFile.parentFile
        val isRoot = _storageRoots.value.any { it.path == _currentPath.value }
        if (isRoot) return false
        return if (parent != null && parent.exists()) {
            loadFiles(parent.absolutePath)
            true
        } else false
    }

    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            if (repository.deleteFile(fileItem.path)) {
                if (_selectedCategory.value == null) loadFiles(_currentPath.value)
            }
        }
    }

    fun toggleFavorite(fileItem: FileItem) {
        viewModelScope.launch { repository.toggleFavorite(fileItem) }
    }

    fun createFolder(name: String) {
        if (_currentPath.value.isNotEmpty()) {
            viewModelScope.launch {
                if (repository.createFolder(_currentPath.value, name)) loadFiles(_currentPath.value)
            }
        }
    }

    fun createFile(name: String) {
        if (_currentPath.value.isNotEmpty()) {
            viewModelScope.launch {
                if (repository.createFile(_currentPath.value, name)) loadFiles(_currentPath.value)
            }
        }
    }

    fun renameFile(fileItem: FileItem, newName: String) {
        viewModelScope.launch {
            if (repository.renameFile(fileItem.path, newName)) {
                if (_selectedCategory.value == null) loadFiles(_currentPath.value)
            }
        }
    }

    fun addRecentFile(fileItem: FileItem) {
        viewModelScope.launch { repository.addRecentFile(fileItem) }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            _selectedCategory.value = null
            _currentPath.value = ""
        }
    }
}
