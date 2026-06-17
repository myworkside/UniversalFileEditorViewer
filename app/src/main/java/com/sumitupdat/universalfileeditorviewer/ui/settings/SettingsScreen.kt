package com.sumitupdat.universalfileeditorviewer.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumitupdat.universalfileeditorviewer.data.local.FileSortOrder
import com.sumitupdat.universalfileeditorviewer.data.local.ThemeMode
import com.sumitupdat.universalfileeditorviewer.data.local.UiDensity
import com.sumitupdat.universalfileeditorviewer.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAnalyzer: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showDensityDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = uiState.preferences.theme,
            onDismiss = { showThemeDialog = false },
            onSelect = { 
                viewModel.updateTheme(it)
                showThemeDialog = false
            }
        )
    }

    if (showSortDialog) {
        SortOrderSelectionDialog(
            currentOrder = uiState.preferences.fileSorting,
            onDismiss = { showSortDialog = false },
            onSelect = {
                viewModel.updateFileSorting(it)
                showSortDialog = false
            }
        )
    }

    if (showDensityDialog) {
        DensitySelectionDialog(
            currentDensity = uiState.preferences.uiDensity,
            onDismiss = { showDensityDialog = false },
            onSelect = {
                viewModel.updateUiDensity(it)
                showDensityDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = uiState.preferences.language,
            onDismiss = { showLanguageDialog = false },
            onSelect = {
                viewModel.updateLanguage(it)
                showLanguageDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        val filteredQuery = uiState.searchQuery.lowercase()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text("Search settings") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            } else null
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth()
                ) {}
                Spacer(modifier = Modifier.height(16.dp))
            }

            fun shouldShow(vararg keywords: String): Boolean {
                if (filteredQuery.isEmpty()) return true
                return keywords.any { it.lowercase().contains(filteredQuery) }
            }

            if (shouldShow("appearance", "theme", "amoled", "dark", "light", "dynamic", "color", "density", "font")) {
                item {
                    SettingsSection(title = "Appearance") {
                        if (shouldShow("theme", "dark", "light", "system")) {
                            SettingsItem(
                                title = "Theme",
                                subtitle = when(uiState.preferences.theme) {
                                    ThemeMode.SYSTEM -> "Follow System"
                                    ThemeMode.LIGHT -> "Light Mode"
                                    ThemeMode.DARK -> "Dark Mode"
                                },
                                icon = Icons.Outlined.Palette,
                                onClick = { showThemeDialog = true }
                            )
                        }
                        if (shouldShow("amoled", "black", "oled")) {
                            SettingsSwitch(
                                title = "AMOLED Black",
                                subtitle = "Pure black for OLED screens",
                                icon = Icons.Outlined.DarkMode,
                                checked = uiState.preferences.isAmoled,
                                onCheckedChange = viewModel::toggleAmoled
                            )
                        }
                        if (shouldShow("dynamic", "material you", "color")) {
                            SettingsSwitch(
                                title = "Dynamic Colors",
                                subtitle = "Material You integration",
                                icon = Icons.Outlined.ColorLens,
                                checked = uiState.preferences.useDynamicColors,
                                onCheckedChange = viewModel::toggleDynamicColors
                            )
                        }
                        
                        if (!uiState.preferences.useDynamicColors && shouldShow("accent", "color")) {
                            AccentColorPicker(
                                selectedColor = uiState.preferences.accentColor,
                                onColorSelect = viewModel::updateAccentColor
                            )
                        }

                        if (shouldShow("font", "size", "text")) {
                            FontSizeSlider(
                                currentMultiplier = uiState.preferences.fontSizeMultiplier,
                                onMultiplierChange = viewModel::updateFontSize
                            )
                        }

                        if (shouldShow("density", "ui", "compact", "cozy", "default")) {
                            SettingsItem(
                                title = "UI Density",
                                subtitle = uiState.preferences.uiDensity.name.lowercase().replaceFirstChar { it.uppercase() },
                                icon = Icons.AutoMirrored.Outlined.ViewQuilt,
                                onClick = { showDensityDialog = true }
                            )
                        }
                    }
                }
            }

            if (shouldShow("file", "manager", "hidden", "extension", "sorting", "thumbnail", "analyzer")) {
                item {
                    SettingsSection(title = "File Manager") {
                        if (shouldShow("default", "folder", "start")) {
                            SettingsItem(
                                title = "Default Start Folder",
                                subtitle = uiState.preferences.defaultStartFolder,
                                icon = Icons.Outlined.Folder,
                                onClick = {}
                            )
                        }
                        if (shouldShow("hidden", "dot")) {
                            SettingsSwitch(
                                title = "Show Hidden Files",
                                subtitle = "Display files starting with dot",
                                icon = Icons.Outlined.Visibility,
                                checked = uiState.preferences.showHiddenFiles,
                                onCheckedChange = viewModel::toggleShowHiddenFiles
                            )
                        }
                        if (shouldShow("extension", "type")) {
                            SettingsSwitch(
                                title = "Show Extensions",
                                subtitle = "Always show file extensions",
                                icon = Icons.Outlined.Extension,
                                checked = uiState.preferences.showFileExtensions,
                                onCheckedChange = viewModel::toggleShowExtensions
                            )
                        }
                        if (shouldShow("sorting", "order", "name", "date", "size")) {
                            SettingsItem(
                                title = "File Sorting",
                                subtitle = uiState.preferences.fileSorting.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                icon = Icons.AutoMirrored.Outlined.Sort,
                                onClick = { showSortDialog = true }
                            )
                        }
                        if (shouldShow("thumbnail", "preview", "generation")) {
                            SettingsSwitch(
                                title = "Thumbnail Generation",
                                subtitle = "Auto generate previews",
                                icon = Icons.Outlined.Image,
                                checked = uiState.preferences.thumbnailGeneration,
                                onCheckedChange = viewModel::toggleThumbnailGeneration
                            )
                        }
                        if (shouldShow("analyzer", "storage", "usage")) {
                            SettingsItem(
                                title = "Storage Analyzer",
                                subtitle = "Analyze storage usage",
                                icon = Icons.Outlined.Analytics,
                                onClick = onNavigateToAnalyzer
                            )
                        }
                    }
                }
            }

            if (shouldShow("security", "privacy", "biometric", "lock", "credential", "vault", "delete", "audit", "logs")) {
                item {
                    SettingsSection(title = "Security & Privacy") {
                        if (shouldShow("biometric", "fingerprint", "face")) {
                            SettingsSwitch(
                                title = "Biometric Unlock",
                                subtitle = "Secure vault with fingerprint/face",
                                icon = Icons.Outlined.Fingerprint,
                                checked = uiState.preferences.biometricEnabled,
                                onCheckedChange = viewModel::updateBiometric
                            )
                        }
                        if (shouldShow("credential", "pin", "pattern", "password")) {
                            SettingsSwitch(
                                title = "Device Credential",
                                subtitle = "Use PIN/Pattern/Password",
                                icon = Icons.Outlined.Lock,
                                checked = uiState.preferences.deviceCredentialEnabled,
                                onCheckedChange = viewModel::updateDeviceCredential
                            )
                        }
                        if (shouldShow("auto", "lock", "vault", "timer")) {
                            SettingsSwitch(
                                title = "Auto Lock Vault",
                                subtitle = "Lock when app is closed",
                                icon = Icons.Outlined.Timer,
                                checked = uiState.preferences.autoLockVault,
                                onCheckedChange = viewModel::toggleAutoLockVault
                            )
                        }
                        if (shouldShow("secure", "delete", "overwrite", "shield")) {
                            SettingsSwitch(
                                title = "Secure Delete",
                                subtitle = "Overwrite files before deleting",
                                icon = Icons.Outlined.Shield,
                                checked = uiState.preferences.secureDelete,
                                onCheckedChange = viewModel::toggleSecureDelete
                            )
                        }
                        if (shouldShow("audit", "logs", "history", "security")) {
                            SettingsItem(
                                title = "Audit Logs",
                                subtitle = "Review security events",
                                icon = Icons.Outlined.History,
                                onClick = onNavigateToLogs
                            )
                        }
                    }
                }
            }

            if (shouldShow("wireless", "sharing", "receive", "background", "transfer", "discovery", "wifi", "bluetooth")) {
                item {
                    SettingsSection(title = "Wireless Sharing") {
                        if (shouldShow("auto", "receive")) {
                            SettingsSwitch(
                                title = "Auto Receive",
                                subtitle = "Accept files automatically",
                                icon = Icons.Outlined.Wifi,
                                checked = uiState.preferences.autoReceiveFiles,
                                onCheckedChange = viewModel::toggleAutoReceiveFiles
                            )
                        }
                        if (shouldShow("background", "transfer")) {
                            SettingsSwitch(
                                title = "Background Transfers",
                                subtitle = "Keep transferring when app minimized",
                                icon = Icons.Outlined.CloudSync,
                                checked = uiState.preferences.backgroundTransfers,
                                onCheckedChange = viewModel::toggleBackgroundTransfers
                            )
                        }
                        if (shouldShow("device", "discovery", "radar", "visible")) {
                            SettingsSwitch(
                                title = "Device Discovery",
                                subtitle = "Make this device visible to others",
                                icon = Icons.Outlined.Radar,
                                checked = uiState.preferences.deviceDiscovery,
                                onCheckedChange = viewModel::toggleDeviceDiscovery
                            )
                        }
                    }
                }
            }

            if (shouldShow("maintenance", "clear", "cache", "export", "import", "backup", "restore")) {
                item {
                    SettingsSection(title = "Maintenance") {
                        if (shouldShow("clear", "cache", "temporary")) {
                            SettingsItem(
                                title = "Clear Cache",
                                subtitle = "Free up temporary storage",
                                icon = Icons.Outlined.DeleteSweep,
                                onClick = viewModel::clearCache
                            )
                        }
                        if (shouldShow("export", "backup")) {
                            SettingsItem(
                                title = "Export Settings",
                                subtitle = "Backup your preferences",
                                icon = Icons.Outlined.FileDownload,
                                onClick = viewModel::exportSettings
                            )
                        }
                        if (shouldShow("import", "restore")) {
                            SettingsItem(
                                title = "Import Settings",
                                subtitle = "Restore your preferences",
                                icon = Icons.Outlined.FileUpload,
                                onClick = viewModel::importSettings
                            )
                        }
                    }
                }
            }

            if (shouldShow("language", "region", "date", "time")) {
                item {
                    SettingsSection(title = "Language & Region") {
                        SettingsItem(
                            title = "Language",
                            subtitle = uiState.preferences.language,
                            icon = Icons.Outlined.Language,
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }
            }

            if (shouldShow("about", "version", "name", "app", "premium")) {
                item {
                    SettingsSection(title = "About") {
                        if (shouldShow("name", "app")) {
                            SettingsItem(
                                title = "App Name",
                                subtitle = "Universal File Editor & Viewer",
                                icon = Icons.Outlined.AppShortcut,
                                onClick = {}
                            )
                        }
                        if (shouldShow("version", "premium")) {
                            SettingsItem(
                                title = "Version",
                                subtitle = "1.0.0 (Premium)",
                                icon = Icons.Outlined.Info,
                                onClick = {}
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun AccentColorPicker(
    selectedColor: Int,
    onColorSelect: (Int) -> Unit
) {
    val colors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFF018786),
        Color(0xFFB00020), Color(0xFF3700B3), Color(0xFFFF9800),
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFE91E63)
    )

    Column(Modifier.padding(16.dp)) {
        Text("Accent Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            colors.forEach { color ->
                val isSelected = color.toArgb() == selectedColor
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorSelect(color.toArgb()) }
                )
            }
        }
    }
}

@Composable
fun FontSizeSlider(
    currentMultiplier: Float,
    onMultiplierChange: (Float) -> Unit
) {
    var sliderValue by remember(currentMultiplier) { mutableFloatStateOf(currentMultiplier) }

    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.FormatSize, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text("Font Size", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Text("${(sliderValue * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onMultiplierChange(sliderValue) },
            valueRange = 0.8f..1.5f,
            steps = 6
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (mode == currentMode),
                                onClick = { onSelect(mode) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (mode == currentMode),
                            onClick = null
                        )
                        Text(
                            text = when(mode) {
                                ThemeMode.SYSTEM -> "Follow System"
                                ThemeMode.LIGHT -> "Light Mode"
                                ThemeMode.DARK -> "Dark Mode"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SortOrderSelectionDialog(
    currentOrder: FileSortOrder,
    onDismiss: () -> Unit,
    onSelect: (FileSortOrder) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Sorting") },
        text = {
            LazyColumn(Modifier.selectableGroup().heightIn(max = 400.dp)) {
                items(FileSortOrder.entries) { order ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (order == currentOrder),
                                onClick = { onSelect(order) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (order == currentOrder), onClick = null)
                        Text(
                            text = order.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DensitySelectionDialog(
    currentDensity: UiDensity,
    onDismiss: () -> Unit,
    onSelect: (UiDensity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("UI Density") },
        text = {
            Column(Modifier.selectableGroup()) {
                UiDensity.entries.forEach { density ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (density == currentDensity),
                                onClick = { onSelect(density) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (density == currentDensity), onClick = null)
                        Text(
                            text = density.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf("System", "English", "Spanish", "French", "German", "Hindi", "Bengali")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            LazyColumn(Modifier.selectableGroup().heightIn(max = 400.dp)) {
                items(languages) { lang ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (lang == currentLanguage),
                                onClick = { onSelect(lang) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (lang == currentLanguage), onClick = null)
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}
