package com.sumitupdat.universalfileeditorviewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumitupdat.universalfileeditorviewer.data.model.FileCategory
import com.sumitupdat.universalfileeditorviewer.ui.theme.*
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel

@Composable
fun DashboardScreen(
    viewModel: FileViewModel,
    onCategoryClick: (FileCategory) -> Unit,
    onMenuClick: () -> Unit
) {
    val isScanning by viewModel.isScanning.collectAsState()

    val categories = remember {
        listOf(
            DashboardItem("Documents", Icons.Default.Description, GradientDoc, FileCategory.DOCUMENTS),
            DashboardItem("Images", Icons.Default.Image, GradientImage, FileCategory.IMAGES),
            DashboardItem("Audio", Icons.Default.AudioFile, GradientAudio, FileCategory.AUDIO),
            DashboardItem("Video", Icons.Default.VideoFile, GradientVideo, FileCategory.VIDEO),
            DashboardItem("Archives", Icons.Default.Inventory2, GradientArchive, FileCategory.ARCHIVES),
            DashboardItem("Code Files", Icons.Default.Code, GradientCode, FileCategory.CODE),
            DashboardItem("Databases", Icons.Default.Storage, GradientDatabase, FileCategory.DATABASES),
            DashboardItem("Fonts", Icons.Default.FontDownload, GradientFont, FileCategory.FONTS),
            DashboardItem("Spreadsheets", Icons.Default.TableChart, GradientImage, FileCategory.SPREADSHEETS),
            DashboardItem("Presentations", Icons.Default.PresentToAll, GradientVideo, FileCategory.PRESENTATIONS),
            DashboardItem("Configuration", Icons.Default.Settings, GradientCode, FileCategory.CONFIGURATION),
            DashboardItem("Logs", Icons.Default.History, GradientCode, FileCategory.LOGS),
            DashboardItem("Android", Icons.Default.Android, GradientAndroid, FileCategory.ANDROID),
            DashboardItem("Web Files", Icons.Default.Language, GradientWeb, FileCategory.WEB),
            DashboardItem("GIS & Maps", Icons.Default.Map, GradientAudio, FileCategory.GIS),
            DashboardItem("Backups", Icons.Default.Backup, GradientDoc, FileCategory.BACKUP),
            DashboardItem("Cloud Storage", Icons.Default.Cloud, GradientCloud, FileCategory.OTHER),
            DashboardItem("Emails", Icons.Default.Email, GradientArchive, FileCategory.EMAILS),
            DashboardItem("Windows", Icons.Default.Window, GradientCloud, FileCategory.WINDOWS),
            DashboardItem("Linux", Icons.Default.Terminal, GradientDatabase, FileCategory.LINUX),
            DashboardItem("Other", Icons.Default.MoreHoriz, GradientCloud, FileCategory.OTHER)
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val screenWidth = maxWidth
        val columns = when {
            screenWidth < 600.dp -> 2
            screenWidth < 900.dp -> 3
            else -> 4
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(categories) { index, item ->
                CategoryCard(index, item, viewModel, onCategoryClick)
            }
        }
        
        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun CategoryCard(index: Int, item: DashboardItem, viewModel: FileViewModel, onClick: (FileCategory) -> Unit) {
    val countState = viewModel.categoryCounts[item.category]
    val count by (countState?.collectAsState() ?: remember { mutableStateOf(0) })
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 30L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.9f)
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable { onClick(item.category) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                item.gradient.first().copy(alpha = 0.8f),
                                item.gradient.last().copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = if (count == 1) "1 file" else "$count files",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }
}

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val category: FileCategory
)
