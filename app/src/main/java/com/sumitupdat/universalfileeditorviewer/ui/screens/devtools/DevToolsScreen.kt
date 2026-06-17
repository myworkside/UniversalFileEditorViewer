package com.sumitupdat.universalfileeditorviewer.ui.screens.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsScreen(
    onBack: () -> Unit,
    onLaunchTool: (String) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Developer Tools", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings or Info */ }) {
                        Icon(Icons.Default.Info, contentDescription = "Info")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(2) }) { DevToolCategoryHeader("Code Editors", Icons.Default.Code) }
            items(codeEditors) { editor ->
                DevToolCard(editor.name, editor.icon, editor.color) { onLaunchTool(editor.name) }
            }

            item(span = { GridItemSpan(2) }) { DevToolCategoryHeader("Android Tools", Icons.Default.Android) }
            items(androidTools) { tool ->
                DevToolCard(tool.name, tool.icon, tool.color) { onLaunchTool(tool.name) }
            }

            item(span = { GridItemSpan(2) }) { DevToolCategoryHeader("File Tools", Icons.Default.FolderZip) }
            items(fileTools) { tool ->
                DevToolCard(tool.name, tool.icon, tool.color) { onLaunchTool(tool.name) }
            }
            
            item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun DevToolCategoryHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun DevToolCard(name: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

data class DevTool(val name: String, val icon: ImageVector, val color: Color)

val codeEditors = listOf(
    DevTool("Kotlin", Icons.Default.Code, Color(0xFF7F52FF)),
    DevTool("Java", Icons.Default.Code, Color(0xFF007396)),
    DevTool("Python", Icons.Default.Code, Color(0xFF3776AB)),
    DevTool("JS", Icons.Default.Javascript, Color(0xFFF7DF1E)),
    DevTool("HTML", Icons.Default.Html, Color(0xFFE34F26)),
    DevTool("CSS", Icons.Default.Css, Color(0xFF1572B6)),
    DevTool("JSON", Icons.Default.DataObject, Color(0xFF000000)),
    DevTool("XML", Icons.Default.Code, Color(0xFFFFA500)),
    DevTool("YAML", Icons.Default.Settings, Color(0xFFCB171E)),
    DevTool("Markdown", Icons.Default.Description, Color(0xFF000000)),
    DevTool("SQL", Icons.Default.Storage, Color(0xFF336791))
)

val androidTools = listOf(
    DevTool("APK Analyzer", Icons.Default.Analytics, Color(0xFF3DDC84)),
    DevTool("Manifest", Icons.Default.Article, Color(0xFF3DDC84)),
    DevTool("Permissions", Icons.Default.Lock, Color(0xFF3DDC84)),
    DevTool("Resources", Icons.Default.Source, Color(0xFF3DDC84))
)

val fileTools = listOf(
    DevTool("Explorer", Icons.Default.Folder, Color(0xFFFFC107)),
    DevTool("Workspace", Icons.Default.Workspaces, Color(0xFF2196F3)),
    DevTool("Recent", Icons.Default.History, Color(0xFF9C27B0))
)
