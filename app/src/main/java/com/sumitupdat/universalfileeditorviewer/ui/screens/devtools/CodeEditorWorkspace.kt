package com.sumitupdat.universalfileeditorviewer.ui.screens.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.sumitupdat.universalfileeditorviewer.viewmodel.DevFile
import com.sumitupdat.universalfileeditorviewer.viewmodel.DevToolsViewModel

class SyntaxHighlightTransformation(val language: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlightCode(text.text, language),
            OffsetMapping.Identity
        )
    }
}

fun highlightCode(code: String, language: String): AnnotatedString {
    val keywords = when(language) {
        "kotlin", "java" -> listOf("package", "import", "class", "fun", "val", "var", "override", "if", "else", "when", "return", "true", "false", "this", "super", "private", "public", "protected", "interface", "object", "sealed", "enum", "data", "suspend", "coroutine", "inline", "infix", "internal")
        "python" -> listOf("def", "class", "import", "from", "if", "else", "elif", "return", "True", "False", "None", "try", "except", "with", "as", "for", "in", "while", "lambda", "yield", "async", "await")
        "javascript", "html" -> listOf("var", "let", "const", "function", "class", "if", "else", "return", "true", "false", "null", "undefined", "this", "new", "try", "catch", "finally", "import", "export", "async", "await")
        "cpp" -> listOf("include", "namespace", "using", "std", "int", "float", "double", "bool", "void", "char", "class", "struct", "template", "typename", "public", "private", "protected", "if", "else", "while", "for", "switch", "case", "break", "continue", "return", "new", "delete", "try", "catch", "throw")
        "xml", "html_tag" -> listOf("xml", "version", "encoding", "DOCTYPE", "html", "head", "body", "div", "span", "p", "h1", "h2", "h3", "a", "img", "ul", "ol", "li", "script", "style", "meta", "link", "title", "input", "button", "form", "label")
        else -> emptyList()
    }
    
    return buildAnnotatedString {
        var lastIndex = 0
        val regex = Regex("""\b(${keywords.joinToString("|")})\b|"(.*?)"|'.*?'|//.*|/\*.*?\*/|#.*|<.*?>""", RegexOption.DOT_MATCHES_ALL)
        
        regex.findAll(code).forEach { match ->
            append(code.substring(lastIndex, match.range.first))
            
            val style = when {
                match.value.startsWith("\"") || match.value.startsWith("'") -> SpanStyle(color = Color(0xFF6A8759)) // String
                match.value.startsWith("//") || match.value.startsWith("/*") || match.value.startsWith("#") -> SpanStyle(color = Color.Gray) // Comment
                match.value.startsWith("<") && match.value.endsWith(">") -> SpanStyle(color = Color(0xFF569CD6)) // Tag
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

@Composable
fun CodeEditorWorkspace(
    modifier: Modifier = Modifier,
    viewModel: DevToolsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeIndex = uiState.activeFileIndex
    
    if (activeIndex == -1) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF1E1E1E)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Code, null, modifier = Modifier.size(64.dp), tint = Color.DarkGray)
                Spacer(Modifier.height(16.dp))
                Text("Select a file from Explorer to start editing", color = Color.Gray)
            }
        }
        return
    }

    val activeFile = uiState.openFiles[activeIndex]

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        // Tab Bar
        EditorTabBar(
            tabs = uiState.openFiles,
            selectedTab = activeIndex,
            onTabClick = { viewModel.selectTab(it) },
            onCloseClick = { viewModel.closeFile(it) }
        )
        
        // Editor Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Line Numbers
                LineNumbers(lineCount = activeFile.content.lines().size)
                
                // Main Editor
                TextField(
                    value = activeFile.content,
                    onValueChange = { viewModel.updateActiveFileContent(it) },
                    modifier = Modifier.fillMaxSize(),
                    visualTransformation = SyntaxHighlightTransformation(activeFile.language),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = Color(0xFFD4D4D4)
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF007ACC),
                        selectionColors = TextSelectionColors(
                            handleColor = Color(0xFF007ACC),
                            backgroundColor = Color(0xFF007ACC).copy(alpha = 0.4f)
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun EditorTabBar(tabs: List<DevFile>, selectedTab: Int, onTabClick: (Int) -> Unit, onCloseClick: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 0.dp,
        containerColor = Color(0xFF252526),
        divider = {},
        indicator = { tabPositions ->
            if (selectedTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF007ACC)
                )
            }
        }
    ) {
        tabs.forEachIndexed { index, devFile ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabClick(index) },
                content = {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val title = devFile.file.name
                        Icon(
                            imageVector = getFileIcon(title),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = getFileIconColor(title)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title, 
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedTab == index) Color.White else Color.Gray
                        )
                        if (devFile.isModified) {
                            Box(Modifier.padding(start = 4.dp).size(6.dp).background(Color.White, CircleShape))
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onCloseClick(index) }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                        }
                    }
                }
            )
        }
    }
}

fun getFileIcon(fileName: String): ImageVector {
    return when {
        fileName.endsWith(".kt") || fileName.endsWith(".java") -> Icons.Default.Code
        fileName.endsWith(".xml") || fileName.endsWith(".html") -> Icons.Default.Html
        fileName.endsWith(".json") -> Icons.Default.DataObject
        else -> Icons.Default.Description
    }
}

fun getFileIconColor(fileName: String): Color {
    return when {
        fileName.endsWith(".kt") -> Color(0xFF7F52FF)
        fileName.endsWith(".java") -> Color(0xFF007396)
        fileName.endsWith(".xml") -> Color(0xFFFFA500)
        fileName.endsWith(".html") -> Color(0xFFE34F26)
        else -> Color.Gray
    }
}

@Composable
fun LineNumbers(lineCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(40.dp)
            .background(Color(0xFF1E1E1E))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.End
    ) {
        for (i in 1..lineCount) {
            Text(
                text = i.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                modifier = Modifier.padding(end = 8.dp),
                fontSize = 12.sp
            )
        }
    }
}
