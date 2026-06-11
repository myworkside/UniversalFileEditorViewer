package com.sumitupdat.universalfileeditorviewer.ui.screens

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.ui.components.formatSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(onBack: () -> Unit) {
    val stat = StatFs(Environment.getExternalStorageDirectory().path)
    val totalBytes = stat.totalBytes
    val availableBytes = stat.availableBytes
    val usedBytes = totalBytes - availableBytes
    val usedPercentage = (usedBytes.toDouble() / totalBytes.toDouble() * 100).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Analyzer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Internal Storage", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { usedPercentage / 100f },
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Used: ${formatSize(usedBytes)} / ${formatSize(totalBytes)} ($usedPercentage%)")
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { /* TODO: Find duplicates */ }) {
                Text("Find Duplicate Files")
            }
        }
    }
}
