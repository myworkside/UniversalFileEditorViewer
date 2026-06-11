package com.sumitupdat.universalfileeditorviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.ui.components.FileItemRow
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun MainScreen(viewModel: FileViewModel) {
    val navController = rememberNavController()
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            // VS Code Style Activity Bar (Sidebar)
            NavigationRail(
                modifier = Modifier.width(72.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                header = {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.padding(vertical = 16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                NavigationRailItem(
                    selected = false,
                    onClick = { navController.navigate("dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationRailItem(
                    selected = false,
                    onClick = { navController.navigate("browser") },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Explorer") },
                    label = { Text("Explorer", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationRailItem(
                    selected = false,
                    onClick = { navController.navigate("recent") },
                    icon = { Icon(Icons.Default.History, contentDescription = "Recent") },
                    label = { Text("Recent", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationRailItem(
                    selected = false,
                    onClick = { navController.navigate("favorites") },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                    label = { Text("Favorites", style = MaterialTheme.typography.labelSmall) }
                )
                Spacer(modifier = Modifier.weight(1f))
                NavigationRailItem(
                    selected = false,
                    onClick = { navController.navigate("analyzer") },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analyzer") }
                )
                NavigationRailItem(
                    selected = false,
                    onClick = { /* Settings */ },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onCategoryClick = { category ->
                                viewModel.selectCategory(category)
                                navController.navigate("browser")
                            },
                            onMenuClick = { /* Sidebar toggle if needed */ }
                        )
                    }
                    composable("browser") {
                        FileBrowserScreen(
                            viewModel = viewModel,
                            onMenuClick = { /* Activity Bar is always visible */ },
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
                        FileListScreen(
                            title = "Recent Files",
                            files = recentFiles.map { FileItem(it.name, it.path, it.isDirectory, extension = File(it.path).extension) },
                            onBack = { navController.popBackStack() },
                            onFileClick = { file ->
                                val encodedPath = URLEncoder.encode(file.path, StandardCharsets.UTF_8.toString())
                                navController.navigate("viewer/$encodedPath")
                            },
                            onDelete = { file -> viewModel.deleteFile(file) }
                        )
                    }
                    composable("favorites") {
                        val favorites by viewModel.favorites.collectAsState()
                        FileListScreen(
                            title = "Favorites",
                            files = favorites.map { FileItem(it.name, it.path, it.isDirectory, extension = File(it.path).extension, isFavorite = true) },
                            onBack = { navController.popBackStack() },
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
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("analyzer") {
                        StorageAnalyzerScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
        StatusBar(currentPath = currentPath, fileCount = files.size)
    }
}

@Composable
fun StatusBar(currentPath: String, fileCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth().height(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeviceHub, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("main*", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(12.dp))
                Text("UTF-8", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = if (currentPath.isEmpty()) "Universal IDE" else currentPath,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false).padding(horizontal = 16.dp)
            )
            Text("Files: $fileCount", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    title: String,
    files: List<FileItem>,
    onBack: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No files found")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(files) { file ->
                    FileItemRow(
                        fileItem = file,
                        onClick = { onFileClick(file) },
                        onDelete = { onDelete(file) },
                        onFavoriteToggle = { /* Already in favorites or not applicable here */ }
                    )
                }
            }
        }
    }
}
