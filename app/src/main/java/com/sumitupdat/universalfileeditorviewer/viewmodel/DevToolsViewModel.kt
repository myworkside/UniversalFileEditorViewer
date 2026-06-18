package com.sumitupdat.universalfileeditorviewer.viewmodel

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumitupdat.universalfileeditorviewer.player.ApkAnalyzer
import com.sumitupdat.universalfileeditorviewer.player.ApkInfo
import com.sumitupdat.universalfileeditorviewer.util.IDELogger
import com.sumitupdat.universalfileeditorviewer.util.LogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DevFile(
    val file: File?,
    val uri: Uri? = null,
    val name: String,
    val content: String,
    val isModified: Boolean = false,
    val language: String = "plaintext",
    val cursorPosition: Int = 0
)

enum class PanelState { HIDDEN, HALF, EXPANDED }

data class DevToolsUiState(
    val openFiles: List<DevFile> = emptyList(),
    val activeFileIndex: Int = -1,
    val terminalOutput: List<String> = listOf("Universal IDE v3.0 Ready."),
    val explorerRootUri: Uri? = null,
    val explorerFiles: List<DocumentFile> = emptyList(),
    val gitBranch: String = "main",
    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val panelState: PanelState = PanelState.HIDDEN,
    val activeBottomTab: String = "terminal",
    val logs: List<LogEntry> = emptyList(),
    val dbTables: List<String> = emptyList(),
    val dbQueryResults: List<List<String>> = emptyList(),
    val dbSchema: String = "",
    val apkAnalysis: ApkInfo? = null,
    val isSearching: Boolean = false,
    val searchResults: List<SearchResult> = emptyList()
)

data class SearchResult(val fileName: String, val line: Int, val preview: String, val uri: Uri)

@HiltViewModel
class DevToolsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevToolsUiState())
    val uiState: StateFlow<DevToolsUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB limit
    private val TAG = "DevToolsViewModel"

    init {
        viewModelScope.launch {
            IDELogger.logs.collect { newLogs ->
                _uiState.update { it.copy(logs = newLogs) }
            }
        }
        IDELogger.i(TAG, "IDE Core Initialized")
    }

    fun setExplorerRoot(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, 
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            _uiState.update { it.copy(explorerRootUri = uri) }
            loadExplorerFiles(uri)
            IDELogger.i(TAG, "Project root set: $uri")
        } catch (e: Exception) {
            IDELogger.e(TAG, "Failed to set project root: ${e.message}")
        }
    }

    private fun loadExplorerFiles(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val root = DocumentFile.fromTreeUri(context, uri)
            // Lazy load first 100 items for performance
            val files = root?.listFiles()?.sortedWith(
                compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name?.lowercase() }
            )?.take(100) ?: emptyList()
            
            _uiState.update { it.copy(explorerFiles = files, isLoading = false) }
        }
    }

    fun openFile(docFile: DocumentFile) {
        val existingIndex = _uiState.value.openFiles.indexOfFirst { it.uri == docFile.uri }
        if (existingIndex != -1) {
            selectTab(existingIndex)
            return
        }

        if (docFile.length() > MAX_FILE_SIZE) {
            _uiState.update { it.copy(statusMessage = "File too large. Opening first 5MB.") }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val content = context.contentResolver.openInputStream(docFile.uri)?.bufferedReader()?.use { it.readText() }?.take(MAX_FILE_SIZE) ?: ""
                
                val newFile = DevFile(
                    file = null,
                    uri = docFile.uri,
                    name = docFile.name ?: "unnamed",
                    content = content,
                    language = detectLanguage(docFile.name?.substringAfterLast('.') ?: "")
                )
                _uiState.update { 
                    it.copy(
                        openFiles = it.openFiles + newFile,
                        activeFileIndex = it.openFiles.size,
                        isLoading = false
                    )
                }
                IDELogger.d(TAG, "Opened file: ${docFile.name}")
            } catch (e: Exception) {
                IDELogger.e(TAG, "Failed to open file: ${e.message}")
                _uiState.update { it.copy(isLoading = false, statusMessage = "Error: ${e.message}") }
            }
        }
    }

    fun selectTab(index: Int) {
        if (index in _uiState.value.openFiles.indices) {
            _uiState.update { it.copy(activeFileIndex = index) }
        }
    }

    fun closeFile(index: Int) {
        _uiState.update { state ->
            val newList = state.openFiles.filterIndexed { i, _ -> i != index }
            val newActiveIndex = when {
                newList.isEmpty() -> -1
                state.activeFileIndex == index -> if (index >= newList.size) newList.size - 1 else index
                state.activeFileIndex > index -> state.activeFileIndex - 1
                else -> state.activeFileIndex
            }
            state.copy(openFiles = newList, activeFileIndex = newActiveIndex)
        }
    }

    fun updateActiveFileContent(newContent: String) {
        val index = _uiState.value.activeFileIndex
        if (index != -1) {
            _uiState.update { state ->
                val newList = state.openFiles.toMutableList()
                newList[index] = newList[index].copy(content = newContent, isModified = true)
                state.copy(openFiles = newList)
            }
            startAutoSaveTimer()
        }
    }

    private fun startAutoSaveTimer() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000)
            saveActiveFile()
        }
    }

    fun saveActiveFile() {
        val index = _uiState.value.activeFileIndex
        if (index == -1) return
        val devFile = _uiState.value.openFiles[index]
        if (!devFile.isModified || devFile.uri == null) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Atomic save simulation using temp file is complex with SAF, 
                // but we can at least ensure write success.
                context.contentResolver.openOutputStream(devFile.uri, "wt")?.use { 
                    it.write(devFile.content.toByteArray())
                }
                _uiState.update { state ->
                    val newList = state.openFiles.toMutableList()
                    if (index < newList.size) {
                        newList[index] = newList[index].copy(isModified = false)
                    }
                    state.copy(openFiles = newList)
                }
                IDELogger.i(TAG, "Saved: ${devFile.name}")
            } catch (e: Exception) {
                IDELogger.e(TAG, "Save failed: ${e.message}")
            }
        }
    }

    fun analyzeApk(docFile: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val info = ApkAnalyzer(context).analyze(docFile.uri)
            _uiState.update { it.copy(apkAnalysis = info, isLoading = false, activeBottomTab = "debug", panelState = PanelState.HALF) }
        }
    }

    fun browseDatabase(docFile: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val tempFile = File.createTempFile("db_browse_", ".db", context.cacheDir)
                context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                
                val db = SQLiteDatabase.openDatabase(tempFile.path, null, SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
                val tables = mutableListOf<String>()
                while (cursor.moveToNext()) tables.add(cursor.getString(0))
                cursor.close()
                db.close()
                tempFile.delete()
                
                _uiState.update { it.copy(dbTables = tables, activeBottomTab = "database", panelState = PanelState.HALF, isLoading = false) }
            } catch (e: Exception) {
                IDELogger.e(TAG, "DB Error: ${e.message}")
                _uiState.update { it.copy(isLoading = false, statusMessage = "DB Error: ${e.message}") }
            }
        }
    }

    fun executeCommand(command: String) {
        val cmd = command.trim()
        if (cmd.isEmpty()) return
        
        val output = when (cmd.lowercase().split(" ")[0]) {
            "help" -> "Available: help, ls, pwd, clear, info, logs"
            "ls" -> _uiState.value.explorerFiles.joinToString("\n") { (if (it.isDirectory) "[D] " else "[F] ") + (it.name ?: "") }
            "pwd" -> _uiState.value.explorerRootUri?.toString() ?: "No project root"
            "clear" -> {
                _uiState.update { it.copy(terminalOutput = listOf("Terminal cleared.")) }
                return
            }
            "info" -> "Universal IDE v3.1\nFiles Open: ${_uiState.value.openFiles.size}\nRoot: ${_uiState.value.explorerRootUri}"
            "logs" -> "Showing last 10 entries:\n" + _uiState.value.logs.takeLast(10).joinToString("\n") { "[${it.level}] ${it.message}" }
            else -> "Unknown command: $cmd"
        }
        _uiState.update { it.copy(terminalOutput = it.terminalOutput + "> $cmd" + output) }
    }

    fun searchInFiles(query: String) {
        if (query.length < 2) return
        val rootUri = _uiState.value.explorerRootUri ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSearching = true, searchResults = emptyList()) }
            val root = DocumentFile.fromTreeUri(context, rootUri) ?: return@launch
            val results = mutableListOf<SearchResult>()
            
            fun searchRecursive(dir: DocumentFile) {
                dir.listFiles().forEach { file ->
                    if (file.isDirectory) searchRecursive(file)
                    else {
                        try {
                            context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { reader ->
                                reader.lineSequence().forEachIndexed { index, line ->
                                    if (line.contains(query, ignoreCase = true)) {
                                        results.add(SearchResult(file.name ?: "unnamed", index + 1, line.trim(), file.uri))
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
            searchRecursive(root)
            _uiState.update { it.copy(isSearching = false, searchResults = results) }
            IDELogger.i(TAG, "Search finished: ${results.size} matches")
        }
    }

    fun cyclePanelState() {
        _uiState.update { state ->
            val nextState = when (state.panelState) {
                PanelState.HIDDEN -> PanelState.HALF
                PanelState.HALF -> PanelState.EXPANDED
                PanelState.EXPANDED -> PanelState.HIDDEN
            }
            state.copy(panelState = nextState)
        }
    }

    fun setPanelState(state: PanelState) {
        _uiState.update { it.copy(panelState = state) }
    }

    fun setBottomTab(tab: String) {
        _uiState.update { 
            it.copy(
                activeBottomTab = tab, 
                panelState = if (it.panelState == PanelState.HIDDEN) PanelState.HALF else it.panelState
            ) 
        }
    }

    private fun detectLanguage(ext: String): String {
        return when (ext.lowercase()) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "c", "cpp", "h", "hpp" -> "cpp"
            "js", "ts" -> "javascript"
            "html" -> "html"
            "css" -> "css"
            "json" -> "json"
            "xml" -> "xml"
            "yaml", "yml" -> "yaml"
            "md" -> "markdown"
            else -> "plaintext"
        }
    }

    fun clearStatusMessage() = _uiState.update { it.copy(statusMessage = null) }
}
