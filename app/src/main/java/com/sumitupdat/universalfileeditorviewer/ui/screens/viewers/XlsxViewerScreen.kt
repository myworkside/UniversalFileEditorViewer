package com.sumitupdat.universalfileeditorviewer.ui.screens.viewers

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumitupdat.universalfileeditorviewer.data.model.SpreadsheetData
import com.sumitupdat.universalfileeditorviewer.ui.components.ZoomableBox

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XlsxViewerScreen(
    data: SpreadsheetData,
    modifier: Modifier = Modifier
) {
    var selectedSheetIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    val currentSheet = data.sheets.getOrNull(selectedSheetIndex)

    Column(modifier = modifier.fillMaxSize()) {
        // Sheet Selector
        ScrollableTabRow(
            selectedTabIndex = selectedSheetIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            data.sheets.forEachIndexed { index, sheet ->
                Tab(
                    selected = selectedSheetIndex == index,
                    onClick = { selectedSheetIndex = index },
                    text = { Text(sheet.name) }
                )
            }
        }

        if (isSearchVisible) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                placeholder = { Text("Search in sheet...") },
                trailingIcon = {
                    IconButton(onClick = { isSearchVisible = false; searchQuery = "" }) {
                        Icon(Icons.Default.Search, contentDescription = "Close Search")
                    }
                },
                singleLine = true
            )
        }

        currentSheet?.let { sheet ->
            ZoomableBox(modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sheet.rows) { row ->
                        LazyRow {
                            items(row.cells) { cell ->
                                val isMatch = searchQuery.isNotEmpty() && cell.value.contains(searchQuery, ignoreCase = true)
                                
                                Surface(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(35.dp),
                                    border = BorderStroke(0.5.dp, Color.LightGray),
                                    color = if (isMatch) Color.Yellow.copy(alpha = 0.3f) else Color.White
                                ) {
                                    Text(
                                        text = cell.value,
                                        modifier = Modifier.padding(4.dp),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        fontWeight = if (cell.isBold) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available")
        }
    }
}
