package com.sumitupdat.universalfileeditorviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.ui.components.FileItemRow
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel

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
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf("Folder") } // "Folder" or "File"

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create $createType") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotEmpty()) {
                        if (createType == "Folder") {
                            viewModel.createFolder(name)
                        } else {
                            viewModel.createFile(name)
                        }
                    }
                    showCreateDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Column {
                SmallFloatingActionButton(
                    onClick = {
                        createType = "File"
                        showCreateDialog = true
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = "Create File")
                }
                FloatingActionButton(onClick = {
                    createType = "Folder"
                    showCreateDialog = true
                }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Folder")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
            } else if (files.isEmpty()) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No results found" else "Empty folder",
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
}
