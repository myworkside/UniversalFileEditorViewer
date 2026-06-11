package com.sumitupdat.universalfileeditorviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.ui.components.FileItemRow
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel

import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.Menu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileViewModel,
    onMenuClick: () -> Unit,
    onFileClick: (com.sumitupdat.universalfileeditorviewer.data.model.FileItem) -> Unit
) {
    val files by viewModel.files.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isSearching) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onSearch = { isSearching = false },
                    active = true,
                    onActiveChange = { if (!it) isSearching = false },
                    placeholder = { Text("Search files...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { 
                            if (searchQuery.isNotEmpty()) viewModel.onSearchQueryChange("")
                            else isSearching = false 
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                }
            } else {
                TopAppBar(
                    title = { Text(currentPath.split("/").last().ifEmpty { "Storage" }) },
                    navigationIcon = {
                        Row {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                            IconButton(onClick = { viewModel.navigateUp() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Create file/folder */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(files) { file ->
                    FileItemRow(
                        fileItem = file,
                        onClick = {
                            if (file.isDirectory) {
                                viewModel.loadFiles(file.path)
                            } else {
                                onFileClick(file)
                            }
                        },
                        onDelete = { viewModel.deleteFile(file) },
                        onFavoriteToggle = { viewModel.toggleFavorite(file) }
                    )
                }
            }
        }
    }
}
