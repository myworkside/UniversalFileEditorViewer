package com.sumitupdat.universalfileeditorviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.ui.components.FileItemRow
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileViewModel,
    onMenuClick: () -> Unit,
    onFileClick: (com.sumitupdat.universalfileeditorviewer.data.model.FileItem) -> Unit
) {
    val files by viewModel.files.collectAsState()
    val searchResults by viewModel.globalSearchResults.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    var isSearching by remember { mutableStateOf(false) }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf("Folder") } // "Folder" or "File"

    val displayFiles = if (searchQuery.isNotEmpty()) searchResults else files

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
                    onSearch = { /* Done by flow */ },
                    active = true,
                    onActiveChange = { if (!it) {
                        isSearching = false
                        viewModel.onSearchQueryChange("")
                    } },
                    placeholder = { Text("Search all storage...") },
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
                    // Results are shown in the LazyColumn below the TopBar
                }
            } else {
                TopAppBar(
                    title = { 
                        Column {
                            val category = selectedCategory
                            val folderName = if (category != null) {
                                category.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            } else {
                                currentPath.split("/").last().ifEmpty { "Internal Storage" }
                            }
                            Text("$folderName (${displayFiles.size})", style = MaterialTheme.typography.titleMedium)
                            if (currentPath.isNotEmpty() && category == null) {
                                Text(currentPath, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    },
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
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (selectedCategory == null && searchQuery.isEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
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
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (displayFiles.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No results for \"$searchQuery\"" else "This category or folder is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Refresh List")
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(displayFiles) { file ->
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
