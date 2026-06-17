package com.sumitupdat.universalfileeditorviewer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.sumitupdat.universalfileeditorviewer.ui.screens.devtools.*
import com.sumitupdat.universalfileeditorviewer.ui.screens.vault.PrivateVaultScreen
import com.sumitupdat.universalfileeditorviewer.ui.settings.SettingsScreen
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FileViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    
    val currentPath by viewModel.currentPath.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearching by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem("dashboard", "Home", Icons.Outlined.Home, Icons.Filled.Home),
        NavItem("browser", "Explorer", Icons.Outlined.Folder, Icons.Filled.Folder),
        NavItem("recent", "Recent", Icons.Outlined.History, Icons.Filled.History),
        NavItem("favorites", "Favorites", Icons.Outlined.Star, Icons.Filled.Star),
        NavItem("wireless", "Wireless Sharing", Icons.Outlined.Wifi, Icons.Filled.Wifi),
        NavItem("devtools", "Developer Tools", Icons.Outlined.Build, Icons.Filled.Build),
        NavItem("vault", "Private Vault", Icons.Outlined.Lock, Icons.Filled.Lock),
        NavItem("analyzer", "Analytics", Icons.Outlined.Analytics, Icons.Filled.Analytics),
        NavItem("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
        NavItem("about", "About", Icons.Outlined.Info, Icons.Filled.Info)
    )

    BoxWithConstraints {
        val isWideScreen = maxWidth >= 600.dp
        
        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen && !isSearching) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(8.dp))
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }

            Scaffold(
                topBar = {
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
                },
                bottomBar = {
                    if (!isWideScreen && !isSearching) {
                        Surface(
                            tonalElevation = 3.dp,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                NavigationBar(
                                    modifier = Modifier.width(850.dp), // Fixed width to allow scrolling 10 items
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp
                                ) {
                                    navItems.forEach { item ->
                                        val selected = currentRoute == item.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo("dashboard") { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) item.selectedIcon else item.icon,
                                                    contentDescription = item.label
                                                )
                                            },
                                            label = { Text(item.label, maxLines = 1) }
                                        )
                                    }
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
                    NavHost(
                        navController = navController, 
                        startDestination = "dashboard",
                        enterTransition = { fadeIn(tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
                        exitTransition = { fadeOut(tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
                        popEnterTransition = { fadeIn(tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) },
                        popExitTransition = { fadeOut(tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) }
                    ) {
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
                                onDelete = { file -> viewModel.deleteFile(file) },
                                onMoveToVault = { file -> viewModel.moveFileToVault(file) }
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
                                onDelete = { file -> viewModel.deleteFile(file) },
                                onMoveToVault = { file -> viewModel.moveFileToVault(file) }
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
                            SettingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToAnalyzer = { navController.navigate("analyzer") },
                                onNavigateToLogs = { /* Navigate to logs if available */ }
                            )
                        }
                        composable("wireless") {
                            WirelessSharingScreen(onBack = { navController.popBackStack() })
                        }
                        composable("devtools") {
                            DevToolsScreen(
                                onBack = { navController.popBackStack() },
                                onLaunchTool = { toolName ->
                                    when (toolName) {
                                        in codeEditors.map { it.name } -> navController.navigate("devtools/workspace")
                                        "Manifest" -> navController.navigate("devtools/manifest")
                                        "Permissions" -> navController.navigate("devtools/permissions")
                                        "APK Analyzer" -> navController.navigate("devtools/apk_analyzer")
                                        else -> navController.navigate("devtools/workspace")
                                    }
                                }
                            )
                        }
                        composable("devtools/workspace") {
                            CodeEditorWorkspace(onBack = { navController.popBackStack() })
                        }
                        composable("devtools/manifest") {
                            ManifestViewer(onBack = { navController.popBackStack() })
                        }
                        composable("devtools/permissions") {
                            PermissionInspector(onBack = { navController.popBackStack() })
                        }
                        composable("devtools/apk_analyzer") {
                            ApkAnalyzer(onBack = { navController.popBackStack() })
                        }
                        composable("vault") {
                            PrivateVaultScreen(onBack = { navController.popBackStack() })
                        }
                        composable("about") {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String, icon: ImageVector) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$name Screen",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Coming Soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        onFavoriteToggle = { viewModel.toggleFavorite(file) },
                        onMoveToVault = { viewModel.moveFileToVault(file) }
                    )
                }
            }
        }
    }
}

fun getRouteTitle(route: String): String {
    return when (route) {
        "dashboard" -> "Home"
        "browser" -> "Explorer"
        "recent" -> "Recent"
        "favorites" -> "Favorites"
        "wireless" -> "Wireless Sharing"
        "devtools" -> "Developer Tools"
        "vault" -> "Private Vault"
        "analyzer" -> "Analytics"
        "settings" -> "Settings"
        "about" -> "About"
        else -> if (route.startsWith("viewer")) "File Viewer" else "Universal IDE"
    }
}

@Composable
fun SimpleFileList(
    files: List<FileItem>,
    onFileClick: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onMoveToVault: ((FileItem) -> Unit)? = null
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
                    onFavoriteToggle = { /* Not applicable here */ },
                    onMoveToVault = if (onMoveToVault != null) { { onMoveToVault(file) } } else null
                )
            }
        }
    }
}
