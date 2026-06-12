package com.sumitupdat.universalfileeditorviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.ui.components.FileItemRow
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FileViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    
    val currentPath by viewModel.currentPath.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                if (isSearching) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        onSearch = { isSearching = false },
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
                    ) { }
                } else {
                    TopAppBar(
                        title = { 
                            Column {
                                val title = if (currentRoute == "browser") {
                                    val category = selectedCategory
                                    if (category != null) {
                                        category.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                    } else {
                                        currentPath.split("/").last().ifEmpty { "Explorer" }
                                    }
                                } else {
                                    getRouteTitle(currentRoute)
                                }
                                Text(text = title, style = MaterialTheme.typography.titleLarge)
                                if (currentRoute == "browser" && currentPath.isNotEmpty() && selectedCategory == null) {
                                    Text(currentPath, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        },
                        navigationIcon = {
                            if (currentRoute == "browser" && (currentPath.isNotEmpty() || selectedCategory != null)) {
                                IconButton(onClick = { viewModel.navigateUp() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            } else {
                                IconButton(onClick = { /* Open Drawer */ }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { viewModel.refresh() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    )
                }
                
                if (!isSearching) {
                    // Horizontal Navigation Row
                    val selectedIndex = getTabIndex(currentRoute)
                    if (selectedIndex != -1) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedIndex,
                            edgePadding = 16.dp,
                            containerColor = MaterialTheme.colorScheme.background,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            TabItem(navController, "dashboard", "Home", Icons.Default.Home, currentRoute)
                            TabItem(navController, "browser", "Explorer", Icons.Default.Folder, currentRoute)
                            TabItem(navController, "recent", "Recent", Icons.Default.History, currentRoute)
                            TabItem(navController, "favorites", "Favorites", Icons.Default.Star, currentRoute)
                            TabItem(navController, "analyzer", "Analytics", Icons.Default.Analytics, currentRoute)
                            TabItem(navController, "settings", "Settings", Icons.Default.Settings, currentRoute)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if ((currentRoute == "dashboard" || currentRoute == "browser") && !isSearching) {
                FloatingActionButton(
                    onClick = { isSearching = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            NavHost(navController = navController, startDestination = "dashboard") {
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = viewModel,
                        onCategoryClick = { category ->
                            viewModel.selectCategory(category)
                            navController.navigate("browser")
                        },
                        onMenuClick = { }
                    )
                }
                composable("browser") {
                    FileBrowserContent(
                        viewModel = viewModel,
                        onFileClick = { file ->
                            if (!file.isDirectory) {
                                viewModel.addRecentFile(file)
                                val encodedPath = URLEncoder.encode(file.path, StandardCharsets.UTF_8.toString())
                                navController.navigate("viewer/$encodedPath")
                            }
                        }
                    )
                }
                composable("recent") {
                    val recentFiles by viewModel.recentFiles.collectAsState()
                    SimpleFileList(
                        files = recentFiles.map { FileItem(it.name, it.path, it.isDirectory, extension = File(it.path).extension) },
                        onFileClick = { file ->
                            val encodedPath = URLEncoder.encode(file.path, StandardCharsets.UTF_8.toString())
                            navController.navigate("viewer/$encodedPath")
                        },
                        onDelete = { file -> viewModel.deleteFile(file) }
                    )
                }
                composable("favorites") {
                    val favorites by viewModel.favorites.collectAsState()
                    SimpleFileList(
                        files = favorites.map { FileItem(it.name, it.path, it.isDirectory, extension = File(it.path).extension, isFavorite = true) },
                        onFileClick = { file ->
                            val encodedPath = URLEncoder.encode(file.path, StandardCharsets.UTF_8.toString())
                            navController.navigate("viewer/$encodedPath")
                        },
                        onDelete = { file -> viewModel.deleteFile(file) }
                    )
                }
                composable("viewer/{filePath}") { backStackEntry ->
                    val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                    val path = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
                    val file = File(path)
                    FileViewerScreen(
                        fileItem = FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = file.isDirectory,
                            size = file.length(),
                            extension = file.extension.lowercase()
                        ),
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("analyzer") {
                    StorageAnalyzerScreen(onBack = { navController.popBackStack() })
                }
                composable("settings") {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Settings Screen")
                    }
                }
            }
        }
    }
}

@Composable
fun FileBrowserContent(
    viewModel: FileViewModel,
    onFileClick: (FileItem) -> Unit
) {
    val files by viewModel.files.collectAsState()
    val searchResults by viewModel.globalSearchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val displayFiles = if (searchQuery.isNotEmpty()) searchResults else files

    Box(modifier = Modifier.fillMaxSize()) {
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
                    text = if (searchQuery.isNotEmpty()) "No results for \"$searchQuery\"" else "This folder is empty",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
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

@Composable
fun TabItem(navController: NavHostController, route: String, label: String, icon: ImageVector, currentRoute: String) {
    LeadingIconTab(
        selected = currentRoute == route,
        onClick = { 
            if (currentRoute != route) {
                navController.navigate(route) {
                    popUpTo("dashboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        text = { Text(label, style = MaterialTheme.typography.labelMedium) },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
    )
}

fun getTabIndex(route: String): Int {
    return when (route) {
        "dashboard" -> 0
        "browser" -> 1
        "recent" -> 2
        "favorites" -> 3
        "analyzer" -> 4
        "settings" -> 5
        else -> -1
    }
}

fun getRouteTitle(route: String): String {
    return when {
        route == "dashboard" -> "Universal IDE"
        route == "browser" -> "File Explorer"
        route == "recent" -> "Recent Files"
        route == "favorites" -> "Favorites"
        route == "analyzer" -> "Analytics"
        route == "settings" -> "Settings"
        route.startsWith("viewer") -> "File Viewer"
        else -> "Universal IDE"
    }
}

@Composable
fun SimpleFileList(
    files: List<FileItem>,
    onFileClick: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit
) {
    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No files found")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(files) { file ->
                FileItemRow(
                    fileItem = file,
                    onClick = { onFileClick(file) },
                    onDelete = { onDelete(file) },
                    onFavoriteToggle = { /* Not applicable here */ }
                )
            }
        }
    }
}
