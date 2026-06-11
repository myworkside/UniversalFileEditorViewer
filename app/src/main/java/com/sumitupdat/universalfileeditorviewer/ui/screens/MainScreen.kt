package com.sumitupdat.universalfileeditorviewer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel

import kotlinx.coroutines.launch

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
                    selected = true,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("browser") {
                            popUpTo("browser") { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Storage, contentDescription = null) }
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
                            val encodedPath = URLEncoder.encode(file.path, StandardCharsets.UTF_8.toString())
                            navController.navigate("viewer/$encodedPath")
                        }
                    }
                )
            }
            composable("viewer/{filePath}") { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                val path = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
                // We need to find the FileItem or just create a temporary one from path
                val file = java.io.File(path)
                FileViewerScreen(
                    fileItem = com.sumitupdat.universalfileeditorviewer.data.model.FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
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
