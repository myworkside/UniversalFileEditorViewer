package com.sumitupdat.universalfileeditorviewer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.data.model.FileCategory
import com.sumitupdat.universalfileeditorviewer.ui.theme.*
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FileViewModel,
    onCategoryClick: (FileCategory) -> Unit,
    onMenuClick: () -> Unit
) {
    val isScanning by viewModel.isScanning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Universal IDE Dashboard", fontWeight = FontWeight.Bold)
                        if (isScanning) {
                            Text("Indexing storage...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.startIndexing() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rescan")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val categories = listOf(
            DashboardItem("Documents", Icons.Default.Description, CategoryDoc, FileCategory.DOCUMENTS),
            DashboardItem("Images", Icons.Default.Image, CategoryImage, FileCategory.IMAGES),
            DashboardItem("Audio", Icons.Default.AudioFile, CategoryAudio, FileCategory.AUDIO),
            DashboardItem("Video", Icons.Default.VideoFile, CategoryVideo, FileCategory.VIDEO),
            
            DashboardItem("Archives", Icons.Default.Inventory2, CategoryArchive, FileCategory.ARCHIVES),
            DashboardItem("Code Files", Icons.Default.Code, CategoryCode, FileCategory.CODE),
            DashboardItem("Databases", Icons.Default.Storage, CategoryDatabase, FileCategory.DATABASES),
            DashboardItem("Fonts", Icons.Default.FontDownload, CategoryFont, FileCategory.FONTS),
            
            DashboardItem("Configuration", Icons.Default.Settings, CategoryCode, FileCategory.CONFIGURATION),
            DashboardItem("Logs", Icons.Default.History, CategoryCode, FileCategory.LOGS),
            DashboardItem("Backup", Icons.Default.Backup, CategoryDoc, FileCategory.BACKUP),
            DashboardItem("Android", Icons.Default.Android, CategoryAndroid, FileCategory.ANDROID),
            
            DashboardItem("Linux Scripts", Icons.Default.Terminal, CategoryCode, FileCategory.LINUX),
            DashboardItem("Windows Scripts", Icons.Default.DesktopWindows, CategoryDoc, FileCategory.WINDOWS),
            DashboardItem("macOS Files", Icons.Default.Computer, CategoryDoc, FileCategory.MACOS),
            DashboardItem("Web Files", Icons.Default.Language, CategoryWeb, FileCategory.WEB),
            
            DashboardItem("Spreadsheets", Icons.Default.TableChart, CategoryImage, FileCategory.SPREADSHEETS),
            DashboardItem("Presentations", Icons.Default.PresentToAll, CategoryVideo, FileCategory.PRESENTATIONS),
            DashboardItem("Email Files", Icons.Default.Email, CategoryDoc, FileCategory.EMAILS),
            DashboardItem("GIS & Maps", Icons.Default.Map, CategoryAudio, FileCategory.GIS),
            
            DashboardItem("Search All", Icons.Default.Search, CategoryRecent, FileCategory.SEARCH),
            DashboardItem("Favorites", Icons.Default.Star, CategoryFavorite, FileCategory.FAVORITES),
            DashboardItem("Recent Files", Icons.Default.Schedule, CategoryRecent, FileCategory.RECENT),
            DashboardItem("More Types", Icons.Default.MoreHoriz, CategoryCode, FileCategory.OTHER)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(categories) { item ->
                CategoryCard(item, viewModel, onCategoryClick)
            }
        }
    }
}

@Composable
fun CategoryCard(item: DashboardItem, viewModel: FileViewModel, onClick: (FileCategory) -> Unit) {
    val countState = viewModel.categoryCounts[item.category]
    val count by (countState?.collectAsState() ?: remember { mutableStateOf(0) })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick(item.category) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = item.color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.title, style = MaterialTheme.typography.labelLarge, color = item.color, fontWeight = FontWeight.Bold)
            
            // Only show count for searchable categories, not meta categories like Search/Fav/Recent
            if (item.category !in listOf(FileCategory.SEARCH, FileCategory.FAVORITES, FileCategory.RECENT)) {
                Text(
                    text = "$count files",
                    style = MaterialTheme.typography.bodySmall,
                    color = item.color.copy(alpha = 0.7f)
                )
            }
        }
    }
}

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val category: FileCategory
)
