package com.sumitupdat.universalfileeditorviewer.ui.screens.viewers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.data.model.ArchiveEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveViewerScreen(
    entries: List<ArchiveEntry>,
    onExtract: (ArchiveEntry) -> Unit,
    onExtractAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredEntries = remember(searchQuery, entries) {
        if (searchQuery.isEmpty()) entries
        else entries.filter { it.path.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f).padding(8.dp),
                placeholder = { Text("Search in archive...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            TextButton(onClick = onExtractAll, modifier = Modifier.padding(end = 8.dp)) {
                Text("Extract All")
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredEntries) { entry ->
                ListItem(
                    headlineContent = { Text(entry.name) },
                    supportingContent = { Text(entry.path) },
                    leadingContent = {
                        Icon(
                            imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    },
                    trailingContent = {
                        if (!entry.isDirectory) {
                            IconButton(onClick = { onExtract(entry) }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Extract")
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
