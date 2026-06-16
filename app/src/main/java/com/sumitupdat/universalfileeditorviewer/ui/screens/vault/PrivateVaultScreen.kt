package com.sumitupdat.universalfileeditorviewer.ui.screens.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sumitupdat.universalfileeditorviewer.data.local.VaultAuditLog
import com.sumitupdat.universalfileeditorviewer.data.local.VaultFileEntity
import com.sumitupdat.universalfileeditorviewer.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateVaultScreen(
    onBack: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.lockVault()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.addFileToVault(it) } }

    var showTrash by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(targetState = uiState.isLocked, label = "LockIcon") { locked ->
                            Icon(
                                if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (locked) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (showTrash) "Vault Trash" else "Private Vault")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (showTrash) showTrash = false else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isLocked) {
                        if (!showTrash) {
                            IconButton(onClick = { showTrash = true }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Trash")
                            }
                        }
                        IconButton(onClick = { viewModel.lockVault() }) {
                            Icon(Icons.Default.Logout, contentDescription = "Lock")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isLocked) {
                FloatingActionButton(onClick = { filePickerLauncher.launch("*/*") }) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(targetState = uiState.isLocked, label = "VaultContent") { locked ->
                if (locked) VaultLockedContent(onUnlock = { activity?.let { viewModel.authenticate(it) } })
                else {
                    if (showTrash) VaultTrashContent(uiState, viewModel)
                    else VaultDashboardContent(uiState, viewModel)
                }
            }
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
fun VaultLockedContent(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Vault is Locked", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Hardware-backed AES-256 Encryption",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Fingerprint, null)
            Spacer(Modifier.width(8.dp))
            Text("Unlock Vault")
        }
    }
}

@Composable
fun VaultDashboardContent(uiState: com.sumitupdat.universalfileeditorviewer.viewmodel.VaultUiState, viewModel: com.sumitupdat.universalfileeditorviewer.viewmodel.VaultViewModel) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { VaultStatsCard(uiState) }
        
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search files...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(16.dp)
            )
        }

        item { SectionHeader("Encrypted Categories", Icons.Default.Category) }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(vaultCategories) { category ->
                    VaultCategoryCard(category, uiState.categoryCounts[category.name] ?: 0)
                }
            }
        }

        item { SectionHeader("Recent Activity", Icons.Default.History) }
        items(uiState.auditLogs.take(3)) { log ->
            AuditLogRow(log)
        }

        item { SectionHeader("Stored Files", Icons.Default.Description) }
        if (uiState.vaultFiles.isEmpty()) {
            item { EmptyVaultState() }
        } else {
            items(uiState.vaultFiles, key = { it.id }) { file ->
                VaultFileRow(
                    file = file,
                    onRestore = { activity?.let { viewModel.restoreFile(it, file.id, context.getExternalFilesDir(null) ?: context.filesDir) } },
                    onTrash = { viewModel.moveToTrash(file.id) }
                )
            }
        }
    }
}

@Composable
fun VaultStatsCard(uiState: com.sumitupdat.universalfileeditorviewer.viewmodel.VaultUiState) {
    val gradient = Brush.linearGradient(colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2)))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.background(gradient).padding(24.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Security Score: 100/100", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(16.dp))
                Text("Total Encrypted Data", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                Text(formatSize(uiState.totalSize), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatMetric("Files", uiState.fileCount.toString())
                    StatMetric("Health", "Optimal")
                    StatMetric("Version", "v2.0")
                }
            }
        }
    }
}

@Composable
fun StatMetric(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VaultCategoryCard(category: VaultCategory, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Glassmorphism effect background
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.Transparent))
            ).blur(10.dp))
            
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp), color = category.color.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) { Icon(category.icon, null, tint = category.color, modifier = Modifier.size(18.dp)) }
                }
                Column {
                    Text(category.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    val animatedCount by animateIntAsState(targetValue = count, label = "Count")
                    Text("$animatedCount items", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun VaultFileRow(file: VaultFileEntity, onRestore: () -> Unit, onTrash: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(file.originalName, maxLines = 1) },
        supportingContent = { Text(formatSize(file.fileSize)) },
        leadingContent = {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Restore") }, onClick = { onRestore(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Restore, null) })
                    DropdownMenuItem(text = { Text("Move to Trash") }, onClick = { onTrash(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        },
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    )
}

@Composable
fun AuditLogRow(log: VaultAuditLog) {
    val date = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.FiberManualRecord, null, modifier = Modifier.size(8.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(log.details, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun EmptyVaultState() {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ShieldMoon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text("Your vault is empty", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VaultTrashContent(uiState: com.sumitupdat.universalfileeditorviewer.viewmodel.VaultUiState, viewModel: com.sumitupdat.universalfileeditorviewer.viewmodel.VaultViewModel) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Text("Items in trash will be automatically deleted after 30 days.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (uiState.trashFiles.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Trash is empty", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            items(uiState.trashFiles, key = { it.id }) { file ->
                TrashFileRow(
                    file = file,
                    onRestore = { viewModel.restoreFromTrash(file.id) },
                    onDelete = { activity?.let { viewModel.deletePermanently(it, file.id) } }
                )
            }
        }
    }
}

@Composable
fun TrashFileRow(file: VaultFileEntity, onRestore: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(file.originalName) },
        supportingContent = { Text("Deleted • ${formatSize(file.fileSize)}") },
        leadingContent = {
            Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
        },
        trailingContent = {
            Row {
                IconButton(onClick = onRestore) { Icon(Icons.Default.Restore, "Restore") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteForever, "Delete Permanently", tint = MaterialTheme.colorScheme.error) }
            }
        }
    )
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

data class VaultCategory(val name: String, val icon: ImageVector, val color: Color)
val vaultCategories = listOf(
    VaultCategory("Documents", Icons.Default.Description, Color(0xFF2196F3)),
    VaultCategory("Images", Icons.Default.Image, Color(0xFFE91E63)),
    VaultCategory("Videos", Icons.Default.Movie, Color(0xFFFF9800)),
    VaultCategory("Archives", Icons.Default.Inventory2, Color(0xFF4CAF50))
)
