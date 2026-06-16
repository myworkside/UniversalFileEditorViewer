package com.sumitupdat.universalfileeditorviewer.ui.screens.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class KotlinVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlightKotlin(text.text),
            OffsetMapping.Identity
        )
    }
}

fun highlightKotlin(code: String): AnnotatedString {
    val keywords = listOf("package", "import", "class", "fun", "val", "var", "override", "if", "else", "when", "return", "true", "false", "this", "super")
    
    return buildAnnotatedString {
        var lastIndex = 0
        val regex = Regex("""\b(${keywords.joinToString("|")})\b|"(.*?)"|//.*|/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        
        regex.findAll(code).forEach { match ->
            // Add normal text before match
            append(code.substring(lastIndex, match.range.first))
            
            val style = when {
                match.value.startsWith("\"") -> SpanStyle(color = Color(0xFF6A8759)) // String
                match.value.startsWith("//") || match.value.startsWith("/*") -> SpanStyle(color = Color.Gray) // Comment
                else -> SpanStyle(color = Color(0xFFCC7832), fontWeight = FontWeight.Bold) // Keyword
            }
            
            withStyle(style) {
                append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        append(code.substring(lastIndex))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorWorkspace(onBack: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf(0) }
    val openFiles = remember { mutableStateListOf("MainActivity.kt", "build.gradle", "AndroidManifest.xml") }
    
    var codeText by remember { mutableStateOf("""
        package com.sumitupdat.universalfileeditorviewer

        import android.os.Bundle
        import androidx.activity.ComponentActivity
        import androidx.activity.compose.setContent
        import androidx.compose.material3.MaterialTheme

        /**
         * VS Code inspired workspace for Universal File Editor & Viewer
         * Supports multi-tab editing, line numbers, and syntax highlights.
         */
        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    MaterialTheme {
                        // Your App Code Here
                    }
                }
            }
        }
    """.trimIndent()) }

    var showGoToLine by remember { mutableStateOf(false) }
    var goToLineText by remember { mutableStateOf("") }

    if (showGoToLine) {
        AlertDialog(
            onDismissRequest = { showGoToLine = false },
            title = { Text("Go to Line") },
            text = {
                TextField(
                    value = goToLineText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) goToLineText = it },
                    placeholder = { Text("Enter line number") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showGoToLine = false }) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { showGoToLine = false }) { Text("Cancel") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Project Explorer", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                NavigationDrawerItem(
                    label = { Text("app") },
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Folder, null) }
                )
                NavigationDrawerItem(
                    label = { Text("  src") },
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Folder, null) },
                    modifier = Modifier.padding(start = 16.dp)
                )
                NavigationDrawerItem(
                    label = { Text("    main") },
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Folder, null) },
                    modifier = Modifier.padding(start = 32.dp)
                )
                NavigationDrawerItem(
                    label = { Text("      MainActivity.kt") },
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Code, null, tint = Color(0xFF7F52FF)) },
                    modifier = Modifier.padding(start = 48.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Workspace", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        var showMenu by remember { mutableStateOf(false) }
                        var wordWrap by remember { mutableStateOf(false) }

                        IconButton(onClick = { /* Save */ }) { Icon(Icons.Default.Save, "Save") }
                        IconButton(onClick = { /* Run */ }) { Icon(Icons.Default.PlayArrow, "Run", tint = Color(0xFF4CAF50)) }
                        
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Go To Line") },
                                    onClick = { showGoToLine = true; showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Numbers, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Search & Replace") },
                                    onClick = { showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Search, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Word Wrap: ${if (wordWrap) "On" else "Off"}") },
                                    onClick = { wordWrap = !wordWrap; showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.WrapText, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Format Code") },
                                    onClick = { showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignLeft, null) }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Dark Theme") },
                                    onClick = { showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.DarkMode, null) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            },
            bottomBar = {
                EditorStatusBar()
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Tab Bar
                EditorTabBar(
                    tabs = openFiles,
                    selectedTab = selectedTab,
                    onTabClick = { selectedTab = it },
                    onCloseClick = { if (openFiles.size > 1) openFiles.removeAt(it) }
                )
                
                // Editor Area
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Line Numbers
                        LineNumbers(lineCount = codeText.lines().size)
                        
                        // Main Editor
                        TextField(
                            value = codeText,
                            onValueChange = { codeText = it },
                            modifier = Modifier.fillMaxSize(),
                            visualTransformation = KotlinVisualTransformation(),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorTabBar(tabs: List<String>, selectedTab: Int, onTabClick: (Int) -> Unit, onCloseClick: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        divider = {}
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabClick(index) },
                content = {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (title.endsWith(".kt")) Icons.Default.Code else Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (title.endsWith(".kt")) Color(0xFF7F52FF) else Color.Gray
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(title, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onCloseClick(index) }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun LineNumbers(lineCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.End
    ) {
        for (i in 1..lineCount) {
            Text(
                text = i.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 8.dp),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun EditorStatusBar() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Sync, null, modifier = Modifier.size(12.dp), tint = Color.White)
            Spacer(Modifier.width(4.dp))
            Text("Main", style = MaterialTheme.typography.labelSmall, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text("UTF-8", style = MaterialTheme.typography.labelSmall, color = Color.White)
            Spacer(Modifier.width(12.dp))
            Text("Kotlin", style = MaterialTheme.typography.labelSmall, color = Color.White)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = Color.White)
        }
    }
}
