package com.sumitupdat.universalfileeditorviewer.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sumitupdat.universalfileeditorviewer.data.local.TransferEntity
import com.sumitupdat.universalfileeditorviewer.data.local.TransferStatus
import com.sumitupdat.universalfileeditorviewer.data.local.TransferType
import com.sumitupdat.universalfileeditorviewer.util.NearbyDevice
import com.sumitupdat.universalfileeditorviewer.util.TransferProgress
import com.sumitupdat.universalfileeditorviewer.viewmodel.WirelessSharingViewModel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessSharingScreen(
    onBack: () -> Unit,
    viewModel: WirelessSharingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedDeviceForTransfer by remember { mutableStateOf<NearbyDevice?>(null) }
    
    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            viewModel.startDiscovery()
            viewModel.startServer()
        } else {
            Toast.makeText(context, "Permissions denied", Toast.LENGTH_LONG).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val device = selectedDeviceForTransfer
        if (uris.isNotEmpty() && device != null) {
            uris.forEach { uri ->
                when (device) {
                    is NearbyDevice.Bluetooth -> viewModel.sendFileBluetooth(device.device, uri)
                    is NearbyDevice.WifiDirect -> {
                        // For Wi-Fi Direct, we need the group owner IP which is usually in connectionInfo
                        // This is simplified; real impl needs to wait for connection
                        Toast.makeText(context, "Connecting to Wi-Fi device...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        selectedDeviceForTransfer = null
    }

    LaunchedEffect(Unit) {
        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            viewModel.startDiscovery()
            viewModel.startServer()
        } else permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wireless Sharing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.startDiscovery() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SharingActionCard(
                        title = "Send File",
                        icon = Icons.Outlined.FileUpload,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            Toast.makeText(context, "Select a device below first", Toast.LENGTH_SHORT).show()
                        }
                    )
                    SharingActionCard(
                        title = "Receive Mode",
                        icon = Icons.Outlined.FileDownload,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            Toast.makeText(context, "Listening for incoming files...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Active Transfers
            if (uiState.activeTransfers.isNotEmpty()) {
                item {
                    SectionHeader(title = "Active Transfers", icon = Icons.Default.Sensors)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.activeTransfers.values.forEach { progress ->
                            ActiveTransferCard(progress)
                        }
                    }
                }
            }

            // Available Devices
            item {
                SectionHeader(title = "Nearby Devices", icon = Icons.Default.Bluetooth)
            }
            if (uiState.discoveredDevices.isEmpty()) {
                item {
                    Text("Searching for nearby devices...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                items(uiState.discoveredDevices) { device ->
                    DeviceItem(device) {
                        selectedDeviceForTransfer = device
                        filePickerLauncher.launch(arrayOf("*/*"))
                    }
                }
            }

            // Recent Transfers
            if (uiState.transferHistory.isNotEmpty()) {
                item {
                    SectionHeader(title = "Transfer History", icon = Icons.Default.History)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            uiState.transferHistory.take(10).forEach { transfer ->
                                TransferRow(transfer)
                            }
                        }
                    }
                }
            }

            // Statistics
            item {
                SectionHeader(title = "Transfer Statistics", icon = Icons.Default.BarChart)
                StatisticsGrid(
                    totalShared = formatSize(uiState.totalSharedData),
                    avgSpeed = "${String.format(Locale.US, "%.1f", uiState.averageSpeed / 1024 / 1024)} MB/s"
                )
            }
        }
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Transfer Error") },
            text = { Text(uiState.errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ActiveTransferCard(progress: TransferProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (progress.status == "Sending") Icons.Default.Upload else Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(progress.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Text("${(progress.percentage * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.percentage },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.height(4.dp))
            Row {
                Text(
                    text = "${formatSize(progress.bytesTransferred)} / ${formatSize(progress.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun SharingActionCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DeviceItem(device: NearbyDevice, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(device.name) },
        supportingContent = { 
            val type = if (device is NearbyDevice.Bluetooth) "Bluetooth" else "Wi-Fi Direct"
            Text("$type • ${device.status}") 
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (device is NearbyDevice.Bluetooth) Icons.Default.Bluetooth else Icons.Default.Wifi,
                        contentDescription = null
                    )
                }
            }
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        },
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() }
    )
}

@Composable
fun TransferRow(transfer: TransferEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (transfer.transferType == TransferType.SEND) Icons.Default.Upload else Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (transfer.transferStatus == TransferStatus.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transfer.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("${formatSize(transfer.fileSize)} • ${transfer.deviceName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Text(
            text = when(transfer.transferStatus) {
                TransferStatus.COMPLETED -> "Done"
                TransferStatus.FAILED -> "Failed"
                TransferStatus.CANCELLED -> "Cancelled"
                else -> "Unknown"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (transfer.transferStatus == TransferStatus.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun StatisticsGrid(totalShared: String, avgSpeed: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Total Shared", totalShared, Icons.Default.CloudUpload, Modifier.weight(1f))
        StatCard("Avg Speed", avgSpeed, Icons.Default.Speed, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
