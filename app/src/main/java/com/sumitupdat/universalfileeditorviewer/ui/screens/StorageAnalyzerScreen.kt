package com.sumitupdat.universalfileeditorviewer.ui.screens

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.ui.components.formatSize

@Composable
fun StorageAnalyzerScreen(onBack: () -> Unit) {
    val stat = StatFs(Environment.getExternalStorageDirectory().path)
    val totalBytes = stat.totalBytes
    val availableBytes = stat.availableBytes
    val usedBytes = totalBytes - availableBytes
    val usedPercentage = (usedBytes.toDouble() / totalBytes.toDouble() * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Internal Storage", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { usedPercentage / 100f },
            modifier = Modifier.fillMaxWidth().height(24.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Used: ${formatSize(usedBytes)} / ${formatSize(totalBytes)} ($usedPercentage%)", style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { /* TODO: Find duplicates */ },
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Find Duplicate Files")
        }
    }
}
