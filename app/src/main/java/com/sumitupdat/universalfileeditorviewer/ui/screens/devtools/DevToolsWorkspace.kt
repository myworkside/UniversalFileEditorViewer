package com.sumitupdat.universalfileeditorviewer.ui.screens.devtools

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sumitupdat.universalfileeditorviewer.viewmodel.DevToolsUiState
import com.sumitupdat.universalfileeditorviewer.viewmodel.DevToolsViewModel
import com.sumitupdat.universalfileeditorviewer.viewmodel.PanelState
import java.io.File

@Composable
fun DevToolsWorkspace(
    onBack: () -> Unit,
    viewModel: DevToolsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeSidebarTab by remember { mutableStateOf("explorer") }
    var isSidebarExpanded by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val vsCodeDark = Color(0xFF1E1E1E)
    val sidebarBg = Color(0xFF252526)
    val activityBarBg = Color(0xFF333333)
    val accentColor = Color(0xFF007ACC)

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = vsCodeDark) {
            Row(modifier = Modifier.fillMaxSize()) {
                ActivityBar(
                    activeTab = activeSidebarTab,
                    onTabClick = { 
                        if (activeSidebarTab == it) isSidebarExpanded = !isSidebarExpanded
                        else {
                            activeSidebarTab = it
                            isSidebarExpanded = true
                        }
                    },
                    onBack = onBack,
                    bg = activityBarBg
                )

                AnimatedVisibility(
                    visible = isSidebarExpanded,
                    enter = expandHorizontally(),
                    exit = shrinkHorizontally()
                ) {
                    SidebarContent(
                        activeTab = activeSidebarTab,
                        viewModel = viewModel,
                        bg = sidebarBg,
                        accent = accentColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    IDEWorkspaceHeader(activeSidebarTab, viewModel)
                    
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (activeSidebarTab) {
                            "explorer" -> CodeEditorWorkspace(viewModel = viewModel)
                            "search" -> SearchWorkspace(viewModel)
                            "git" -> GitWorkspace()
                            "logs" -> LogcatWorkspace()
                            "database" -> DatabaseWorkspace()
                            "debug" -> ApkAnalyzerWorkspace()
                            else -> CodeEditorWorkspace(viewModel = viewModel)
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.panelState != PanelState.HIDDEN,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        BottomPanel(viewModel, accentColor)
                    }
                    StatusBar(uiState, viewModel, accentColor)
                }
            }
        }
    }
}

@Composable
fun IDEWorkspaceHeader(activeTab: String, viewModel: DevToolsViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(35.dp),
        color = Color(0xFF1E1E1E)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when(activeTab) {
                    "explorer" -> Icons.Default.FolderOpen
                    "search" -> Icons.Default.Search
                    "git" -> Icons.Outlined.Source
                    "logs" -> Icons.AutoMirrored.Filled.ListAlt
                    else -> Icons.Default.Code
                },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.Gray
            )
            Spacer(Modifier.width(8.dp))
            Text("Universal IDE", color = Color.Gray, fontSize = 11.sp)
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
            Text(activeTab.lowercase(), color = Color.Gray, fontSize = 11.sp)
            
            Spacer(Modifier.weight(1f))
            
            if (activeTab == "explorer") {
                Icon(
                    Icons.Default.Save, 
                    null, 
                    modifier = Modifier.size(16.dp).clickable { viewModel.saveActiveFile() }, 
                    tint = Color.Gray
                )
                Spacer(Modifier.width(16.dp))
                Icon(
                    Icons.Default.PlayArrow, 
                    null, 
                    modifier = Modifier.size(16.dp).clickable { /* Run simulation */ }, 
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun SidebarContent(activeTab: String, viewModel: DevToolsViewModel, bg: Color, accent: Color) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(bg)
            .padding(top = 12.dp)
    ) {
        Text(
            text = activeTab.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold
        )

        when (activeTab) {
            "explorer" -> {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "STORAGE", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = accent,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                    uiState.explorerRoot?.let { root ->
                        ExplorerTreeView(
                            file = root,
                            expandedFolders = uiState.expandedFolders,
                            onFileClick = { if (it.isFile) viewModel.openFile(it) else viewModel.toggleFolder(it.absolutePath) },
                            depth = 0
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "COMMON FOLDERS", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = accent,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                    uiState.commonFolders.forEach { folder ->
                        ExplorerTreeView(
                            file = folder,
                            expandedFolders = uiState.expandedFolders,
                            onFileClick = { if (it.isFile) viewModel.openFile(it) else viewModel.toggleFolder(it.absolutePath) },
                            depth = 0
                        )
                    }
                }
            }
            "search" -> SearchWorkspace(viewModel)
            "git" -> GitWorkspace()
            "logs" -> LogcatWorkspace()
            "database" -> DatabaseSidebar()
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Coming Soon", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SearchWorkspace(viewModel: DevToolsViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("SEARCH", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search", color = Color.DarkGray) },
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007ACC)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.searchInProject(searchQuery) })
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.searchInProject(searchQuery) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC))
        ) {
            Text("Search in Files", fontSize = 12.sp)
        }
    }
}

@Composable
fun ApkAnalyzerWorkspace() {
    var selectedTab by remember { mutableStateOf("Overview") }
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        Row(
            modifier = Modifier.fillMaxWidth().height(35.dp).background(Color(0xFF252526)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Overview", "Manifest", "Permissions", "Resources").forEach { tab ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier.clickable { selectedTab = tab }.padding(horizontal = 16.dp).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = tab, fontSize = 11.sp, color = if (selected) Color.White else Color.Gray)
                    if (selected) Box(Modifier.align(Alignment.BottomCenter).width(30.dp).height(1.dp).background(Color(0xFF007ACC)))
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            when (selectedTab) {
                "Overview" -> {
                    Text("APK SIZE BREAKDOWN", color = Color(0xFF007ACC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    ApkSizeRow("classes.dex", "8.2 MB", 0.53f)
                    ApkSizeRow("resources.arsc", "2.1 MB", 0.14f)
                    ApkSizeRow("res/", "3.5 MB", 0.22f)
                    ApkSizeRow("lib/", "1.4 MB", 0.09f)
                    ApkSizeRow("assets/", "0.8 MB", 0.05f)
                }
                "Manifest" -> {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(12.dp)) {
                        Text(
                            text = """
                                <?xml version="1.0" encoding="utf-8"?>
                                <manifest xmlns:android="..." package="...">
                                    <application 
                                        android:label="@string/app_name"
                                        android:icon="@mipmap/ic_launcher">
                                        
                                        <activity android:name=".MainActivity" />
                                        <service android:name=".TransferService" />
                                        
                                    </application>
                                </manifest>
                            """.trimIndent(),
                            color = Color(0xFFD4D4D4),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                "Permissions" -> {
                    listOf("MANAGE_EXTERNAL_STORAGE", "INTERNET", "BLUETOOTH", "WIFI_STATE", "POST_NOTIFICATIONS").forEach { perm ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Text(perm, color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                }
                "Resources" -> {
                    Text("Package: com.sumitupdat.universalfileeditorviewer", color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    listOf("drawable/", "layout/", "values/", "mipmap/", "xml/").forEach { res ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, modifier = Modifier.size(14.dp), tint = Color(0xFFD4A017))
                            Spacer(Modifier.width(12.dp))
                            Text(res, color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityBar(activeTab: String, onTabClick: (String) -> Unit, onBack: () -> Unit, bg: Color) {
    NavigationRail(
        containerColor = bg,
        header = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
        },
        modifier = Modifier.width(52.dp)
    ) {
        val tabs = listOf(
            SidebarTab("explorer", Icons.Default.CopyAll),
            SidebarTab("search", Icons.Default.Search),
            SidebarTab("git", Icons.Outlined.Source),
            SidebarTab("debug", Icons.Default.BugReport),
            SidebarTab("extensions", Icons.Default.Apps),
            SidebarTab("database", Icons.Default.Storage),
            SidebarTab("logs", Icons.AutoMirrored.Filled.ListAlt)
        )

        tabs.forEach { tab ->
            NavigationRailItem(
                selected = activeTab == tab.id,
                onClick = { onTabClick(tab.id) },
                icon = { Icon(tab.icon, null, tint = if (activeTab == tab.id) Color.White else Color.Gray) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.Gray
                )
            )
        }

        Spacer(Modifier.weight(1f))
        IconButton(onClick = { onTabClick("settings") }) {
            Icon(Icons.Default.Settings, null, tint = Color.Gray)
        }
    }
}

@Composable
fun ExplorerTreeView(file: File, expandedFolders: Set<String>, onFileClick: (File) -> Unit, depth: Int) {
    val isExpanded = expandedFolders.contains(file.absolutePath)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp).clickable { onFileClick(file) }.padding(start = (depth * 12 + 12).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (file.isDirectory) {
                if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight
            } else getFileIcon(file.name)
            val iconTint = if (file.isDirectory) Color(0xFFD4A017) else getFileIconColor(file.name)
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = iconTint)
            Spacer(Modifier.width(6.dp))
            Text(file.name, color = Color.LightGray, fontSize = 13.sp, maxLines = 1)
        }
        if (file.isDirectory && isExpanded) {
            val children = remember(file) {
                file.listFiles()
                    ?.sortedWith(compareBy<File> { it.isFile }.thenBy { it.name.lowercase() })
                    ?.take(100)
                    ?: emptyList()
            }
            children.forEach { child -> ExplorerTreeView(child, expandedFolders, onFileClick, depth + 1) }
            if (children.size == 100) {
                Text("...", color = Color.DarkGray, fontSize = 11.sp, modifier = Modifier.padding(start = ((depth + 1) * 12 + 12).dp))
            }
        }
    }
}

@Composable
fun BottomPanel(viewModel: DevToolsViewModel, accent: Color) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val panelHeight = when (uiState.panelState) {
        PanelState.HALF -> screenHeight * 0.35f
        PanelState.EXPANDED -> screenHeight * 0.60f
        PanelState.HIDDEN -> 0.dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(Color(0xFF1E1E1E))
            .border(width = 1.dp, color = Color(0xFF252526))
    ) {
        // Tab Row
        Row(
            modifier = Modifier.fillMaxWidth().height(35.dp).background(Color(0xFF252526)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("PROBLEMS", "OUTPUT", "DEBUG CONSOLE", "TERMINAL")
            tabs.forEach { tab ->
                val selected = uiState.activeBottomTab == tab.lowercase()
                Box(
                    modifier = Modifier
                        .clickable { viewModel.setBottomTab(tab.lowercase()) }
                        .padding(horizontal = 16.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        color = if (selected) Color.White else Color.Gray,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (selected) {
                        Box(Modifier.align(Alignment.BottomCenter).width(40.dp).height(1.dp).background(accent))
                    }
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            // Expand/Restore Toggle
            IconButton(
                onClick = { 
                    val nextState = if (uiState.panelState == PanelState.HALF) PanelState.EXPANDED else PanelState.HALF
                    viewModel.setPanelState(nextState)
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (uiState.panelState == PanelState.HALF) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }

            IconButton(
                onClick = { viewModel.setPanelState(PanelState.HIDDEN) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            when (uiState.activeBottomTab) {
                "terminal" -> TerminalContent(viewModel)
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No output to show.", color = Color.DarkGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun TerminalContent(viewModel: DevToolsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var commandText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        uiState.terminalOutput.forEach { line -> Text(line, color = Color.LightGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("sumit@android:~$ ", color = Color(0xFF4CAF50), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            BasicTextField(
                value = commandText, onValueChange = { commandText = it },
                textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(Color.White), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (commandText.isNotEmpty()) { viewModel.executeCommand(commandText); commandText = "" } }),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StatusBar(uiState: DevToolsUiState, viewModel: DevToolsViewModel, accent: Color) {
    Surface(modifier = Modifier.fillMaxWidth().height(22.dp), color = accent) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.cyclePanelState() },
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = when (uiState.panelState) {
                        PanelState.HIDDEN -> Icons.Default.KeyboardArrowUp
                        PanelState.HALF -> Icons.Default.KeyboardDoubleArrowUp
                        PanelState.EXPANDED -> Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = "Toggle Panel",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            
            Icon(Icons.Default.Sync, null, modifier = Modifier.size(12.dp), tint = Color.White)
            Spacer(Modifier.width(4.dp))
            Text(uiState.gitBranch, color = Color.White, fontSize = 11.sp)
            
            Spacer(Modifier.width(16.dp))
            Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(12.dp), tint = Color.White)
            Text(" ${uiState.errorCount}", color = Color.White, fontSize = 11.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.WarningAmber, null, modifier = Modifier.size(12.dp), tint = Color.White)
            Text(" ${uiState.warningCount}", color = Color.White, fontSize = 11.sp)

            Spacer(Modifier.weight(1f))

            val activeFile = uiState.openFiles.getOrNull(uiState.activeFileIndex)
            if (activeFile != null) {
                Text("Ln ${activeFile.content.lines().size}, Col 1", color = Color.White, fontSize = 11.sp)
                Spacer(Modifier.width(12.dp))
                Text("UTF-8", color = Color.White, fontSize = 11.sp)
                Spacer(Modifier.width(12.dp))
                Text(activeFile.language.replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable fun GitWorkspace() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("SOURCE CONTROL", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
            Spacer(Modifier.width(8.dp))
            Text("Everything is up to date.", color = Color.LightGray, fontSize = 13.sp)
        }
        Text("STAGED CHANGES", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("No staged changes.", color = Color.DarkGray, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))
        Text("COMMITS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f)) {
            items(listOf("Initial commit", "Added vault support", "Updated editor theme")) { commit ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text(commit, color = Color.LightGray, fontSize = 13.sp)
                }
            }
        }
        OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Commit message", fontSize = 12.sp) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007ACC)))
    }
}

@Composable fun LogcatWorkspace() {
    var logFilter by remember { mutableStateOf("All") }
    val logs = remember { listOf("10:24:05.123 D/MainActivity: onCreate started", "10:24:05.150 I/VaultManager: Hardware key loaded successfully", "10:24:06.001 W/FileTransfer: Bluetooth discovery slow", "10:24:08.452 E/Database: Failed to find favorite with path: /root/data", "10:24:10.111 D/Settings: User changed theme to DARK") }
    val filteredLogs = remember(logFilter) { if (logFilter == "All") logs else logs.filter { it.contains(" $logFilter/") } }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        Row(modifier = Modifier.fillMaxWidth().height(35.dp).background(Color(0xFF252526)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("LOGCAT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(16.dp))
            listOf("All", "D", "I", "W", "E").forEach { level ->
                Text(text = level, fontSize = 11.sp, color = if (logFilter == level) Color.White else Color.Gray, modifier = Modifier.clickable { logFilter = level }.padding(horizontal = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp).clickable { }, tint = Color.Gray)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(filteredLogs) { log ->
                val color = when { log.contains(" E/") -> Color(0xFFF44336); log.contains(" W/") -> Color(0xFFFFC107); log.contains(" I/") -> Color(0xFF2196F3); log.contains(" D/") -> Color(0xFF4CAF50); else -> Color.LightGray }
                Text(text = log, color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

@Composable fun DatabaseWorkspace() {
    var selectedTable by remember { mutableStateOf("favorites") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        Row(modifier = Modifier.fillMaxWidth().height(35.dp).background(Color(0xFF252526)), verticalAlignment = Alignment.CenterVertically) {
            listOf("favorites", "recent_files", "file_index", "vault_files").forEach { table ->
                val selected = selectedTable == table
                Box(modifier = Modifier.clickable { selectedTable = table }.padding(horizontal = 12.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(text = table, fontSize = 11.sp, color = if (selected) Color.White else Color.Gray)
                    if (selected) Box(Modifier.align(Alignment.BottomCenter).width(20.dp).height(1.dp).background(Color(0xFF007ACC)))
                }
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("QUERY CONSOLE", color = Color(0xFF007ACC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(80.dp).background(Color.Black).padding(8.dp)) { Text("SELECT * FROM $selectedTable LIMIT 100;", color = Color(0xFF4CAF50), fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
            Spacer(Modifier.height(16.dp))
            Text("RESULT SET", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().border(1.dp, Color.DarkGray)) {
                item { Row(Modifier.background(Color(0xFF252526)).padding(8.dp)) { Text("id", modifier = Modifier.width(40.dp), color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("name", modifier = Modifier.weight(1f), color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("path", modifier = Modifier.weight(2f), color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) } }
                items(5) { i -> Row(Modifier.padding(8.dp)) { Text("$i", modifier = Modifier.width(40.dp), color = Color.LightGray, fontSize = 12.sp); Text("file_$i.txt", modifier = Modifier.weight(1f), color = Color.LightGray, fontSize = 12.sp); Text("/storage/emulated/0/data/...", modifier = Modifier.weight(2f), color = Color.DarkGray, fontSize = 11.sp) } }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) { TextButton(onClick = { }) { Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Export CSV", fontSize = 12.sp) } }
        }
    }
}

@Composable fun DatabaseSidebar() {
    Column(Modifier.padding(16.dp)) {
        Text("DATABASES", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storage, null, modifier = Modifier.size(16.dp), tint = Color(0xFF007ACC))
            Spacer(Modifier.width(8.dp))
            Text("file_database.db", color = Color.LightGray, fontSize = 13.sp)
        }
    }
}

@Composable fun ApkSizeRow(name: String, size: String, progress: Float) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = Color.LightGray, fontSize = 13.sp)
            Text(size, color = Color.Gray, fontSize = 12.sp)
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp), color = Color(0xFF007ACC), trackColor = Color.DarkGray)
    }
}

data class SidebarTab(val id: String, val icon: ImageVector)
