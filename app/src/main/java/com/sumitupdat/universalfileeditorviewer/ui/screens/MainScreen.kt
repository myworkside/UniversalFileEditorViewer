package com.sumitupdat.universalfileeditorviewer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel
import com.sumitupdat.universalfileeditorviewer.data.model.FileItem
import com.sumitupdat.universalfileeditorviewer.ui.components.FileItemRow
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.io.File

@Composable
fun MainScreen(viewModel: FileViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Universal File Editor", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Storage") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("browser") {
                            popUpTo("browser") { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Storage, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Recent Files") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("recent")
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Favorites") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("favorites")
                    },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Storage Analyzer") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("analyzer")
                    },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) }
                )
                Spacer(modifier = Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Instagram (sumitupdat)") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/sumitupdat/"))
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.Link, contentDescription = null) }
                )
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "browser") {
            composable("browser") {
                FileBrowserScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } },
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
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
