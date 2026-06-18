package com.sumitupdat.universalfileeditorviewer.viewmodel

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DevFile(
    val file: File,
    val content: String,
    val isModified: Boolean = false,
    val language: String = "plaintext"
)

enum class PanelState { HIDDEN, HALF, EXPANDED }

data class DevToolsUiState(
    val openFiles: List<DevFile> = emptyList(),
    val activeFileIndex: Int = -1,
    val terminalOutput: List<String> = listOf("Universal IDE Terminal v2.2", "Type 'help' for commands"),
    val expandedFolders: Set<String> = emptySet(),
    val explorerRoot: File? = null,
    val commonFolders: List<File> = emptyList(),
    val gitBranch: String = "main*",
    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val panelState: PanelState = PanelState.HIDDEN,
    val activeBottomTab: String = "terminal"
)

@HiltViewModel
class DevToolsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevToolsUiState())
    val uiState: StateFlow<DevToolsUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private val MAX_FILE_SIZE = 2 * 1024 * 1024 // 2MB limit for editor

    init {
        loadExplorerRoots()
    }

    private fun loadExplorerRoots() {
        val roots = mutableListOf<File>()
        roots.add(Environment.getExternalStorageDirectory())
        
        val common = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        )
        
        _uiState.update { 
            it.copy(
                explorerRoot = Environment.getExternalStorageDirectory(),
                commonFolders = common.filter { f -> f.exists() }
            )
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

    fun openFile(file: File) {
        val existingIndex = _uiState.value.openFiles.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex != -1) {
            selectTab(existingIndex)
            return
        }

        val isReadOnly = file.length() > MAX_FILE_SIZE
        if (isReadOnly) {
            _uiState.update { it.copy(statusMessage = "File too large (>2MB). Opening in read-only mode.") }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val content = if (file.exists() && file.isFile) {
                    file.bufferedReader().use { it.readText() }.take(MAX_FILE_SIZE)
                } else ""
                
                val newFile = DevFile(
                    file = file,
                    content = content,
                    language = detectLanguage(file.extension)
                )
                _uiState.update { 
                    it.copy(
                        openFiles = it.openFiles + newFile,
                        activeFileIndex = it.openFiles.size,
                        isLoading = false
                    )
                }
                logTerminal("Opened: ${file.name}${if (isReadOnly) " [READ-ONLY]" else ""}")
            } catch (e: Exception) {
                Log.e("DevToolsVM", "Error opening file", e)
                _uiState.update { it.copy(isLoading = false, statusMessage = "Failed to open file: ${e.message}") }
            }
        }
    }

    fun selectTab(index: Int) {
        if (index in 0 until _uiState.value.openFiles.size) {
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
            val devFile = _uiState.value.openFiles[index]
            if (devFile.file.length() > MAX_FILE_SIZE) return // Block edits for read-only large files

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
            saveActiveFile(isAutoSave = true)
        }
    }

    fun saveActiveFile(isAutoSave: Boolean = false) {
        val index = _uiState.value.activeFileIndex
        if (index == -1) return
        
        val devFile = _uiState.value.openFiles.getOrNull(index) ?: return
        if (!devFile.isModified) return

        if (!devFile.file.canWrite()) {
            if (!isAutoSave) _uiState.update { it.copy(statusMessage = "File is read-only. Cannot save.") }
            return
        }

        viewModelScope.launch {
            try {
                devFile.file.writeText(devFile.content)
                _uiState.update { state ->
                    val newList = state.openFiles.toMutableList()
                    val currentIndex = state.openFiles.indexOfFirst { it.file.absolutePath == devFile.file.absolutePath }
                    if (currentIndex != -1) {
                        newList[currentIndex] = newList[currentIndex].copy(isModified = false)
                    }
                    state.copy(openFiles = newList)
                }
                if (!isAutoSave) logTerminal("Saved: ${devFile.file.name}")
            } catch (e: Exception) {
                Log.e("DevToolsVM", "Error saving file", e)
                if (!isAutoSave) _uiState.update { it.copy(statusMessage = "Save failed: ${e.message}") }
            }
        }
    }

    fun toggleFolder(path: String) {
        _uiState.update { state ->
            val newSet = state.expandedFolders.toMutableSet()
            if (newSet.contains(path)) newSet.remove(path) else newSet.add(path)
            state.copy(expandedFolders = newSet)
        }
    }

    fun executeCommand(command: String) {
        val cmd = command.trim()
        val output = when (cmd.lowercase()) {
            "help" -> "Available commands: help, ls, clear, date, whoami, pwd, info"
            "ls" -> {
                _uiState.value.explorerRoot?.listFiles()?.joinToString("\n") { 
                    if (it.isDirectory) "[DIR] ${it.name}" else it.name 
                } ?: "No files"
            }
            "pwd" -> _uiState.value.explorerRoot?.absolutePath ?: "/"
            "whoami" -> "android-developer"
            "date" -> java.util.Date().toString()
            "info" -> "Universal IDE v2.2\nStorage: ${Environment.getExternalStorageState()}\nFiles Open: ${_uiState.value.openFiles.size}"
            "clear" -> {
                _uiState.update { it.copy(terminalOutput = listOf("Universal IDE Terminal v2.2", "Type 'help' for commands")) }
                return
            }
            else -> "Command not found: $cmd"
        }
        logTerminal("> $cmd\n$output")
    }

    private fun logTerminal(message: String) {
        _uiState.update { it.copy(terminalOutput = it.terminalOutput + message) }
    }

    fun searchInProject(query: String) {
        if (query.isBlank()) return
        logTerminal("Searching for: \"$query\"...")
        // Simulated project search across open files
        var foundCount = 0
        _uiState.value.openFiles.forEach { devFile ->
            if (devFile.content.contains(query, ignoreCase = true)) {
                val lines = devFile.content.lines()
                lines.forEachIndexed { index, line ->
                    if (line.contains(query, ignoreCase = true)) {
                        logTerminal("${devFile.file.name}:${index + 1}: ${line.trim()}")
                        foundCount++
                    }
                }
            }
        }
        logTerminal("Search finished. Found $foundCount matches in open files.")
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun detectLanguage(extension: String): String {
        return when (extension.lowercase()) {
            "kt" -> "kotlin"
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
}
