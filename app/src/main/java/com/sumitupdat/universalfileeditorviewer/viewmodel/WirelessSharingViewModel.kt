package com.sumitupdat.universalfileeditorviewer.viewmodel

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumitupdat.universalfileeditorviewer.data.local.TransferEntity
import com.sumitupdat.universalfileeditorviewer.data.local.TransferStatus
import com.sumitupdat.universalfileeditorviewer.data.local.TransferType
import com.sumitupdat.universalfileeditorviewer.data.repository.TransferRepository
import com.sumitupdat.universalfileeditorviewer.service.FileTransferService
import com.sumitupdat.universalfileeditorviewer.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject

private const val TAG = "WS_VM"

data class WirelessSharingUiState(
    val discoveredDevices: List<NearbyDevice> = emptyList(),
    val isScanning: Boolean = false,
    val activeTransfers: Map<Long, TransferProgress> = emptyMap(),
    val transferHistory: List<TransferEntity> = emptyList(),
    val totalSharedData: Long = 0,
    val totalSent: Int = 0,
    val totalReceived: Int = 0,
    val averageSpeed: Double = 0.0,
    val errorMessage: String? = null
)

@HiltViewModel
class WirelessSharingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothManager: AppBluetoothManager,
    private val wifiDirectManager: AppWifiDirectManager,
    private val transferRepository: TransferRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WirelessSharingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        combine(
            bluetoothManager.discoveredDevices,
            wifiDirectManager.peers,
            bluetoothManager.isScanning
        ) { btDevices, wifiPeers, scanning ->
            _uiState.update { it.copy(
                discoveredDevices = btDevices + wifiPeers,
                isScanning = scanning
            ) }
        }.launchIn(viewModelScope)

        transferRepository.allTransfers.onEach { history ->
            _uiState.update { it.copy(transferHistory = history) }
        }.launchIn(viewModelScope)

        transferRepository.totalSharedData.onEach { data ->
            _uiState.update { it.copy(totalSharedData = data ?: 0) }
        }.launchIn(viewModelScope)

        transferRepository.totalSentCount.onEach { count ->
            _uiState.update { it.copy(totalSent = count) }
        }.launchIn(viewModelScope)

        transferRepository.totalReceivedCount.onEach { count ->
            _uiState.update { it.copy(totalReceived = count) }
        }.launchIn(viewModelScope)

        transferRepository.averageSpeed.onEach { speed ->
            _uiState.update { it.copy(averageSpeed = speed ?: 0.0) }
        }.launchIn(viewModelScope)
    }

    fun startServer() {
        Log.d(TAG, "Attempting to start servers...")
        bluetoothManager.startServer(
            onIncomingTransfer = { name, size, checksum -> handleIncoming("Bluetooth", name, size, checksum) },
            onProgress = { read, total -> updateIncomingProgress(read, total) },
            onFileReceived = { file, checksum -> finalizeIncoming(file, checksum) },
            onError = { msg -> _uiState.update { it.copy(errorMessage = msg) } }
        )
        
        wifiDirectManager.startServer(
            onIncomingTransfer = { name, size, checksum -> handleIncoming("Wi-Fi Direct", name, size, checksum) },
            onProgress = { read, total -> updateIncomingProgress(read, total) },
            onFileReceived = { file, checksum -> finalizeIncoming(file, checksum) },
            onError = { msg -> _uiState.update { it.copy(errorMessage = msg) } }
        )
    }

    private var currentIncomingId: Long? = null

    private fun handleIncoming(type: String, name: String, size: Long, checksum: String) {
        viewModelScope.launch {
            val entity = TransferEntity(
                fileName = name,
                fileSize = size,
                deviceName = "Nearby $type Device",
                transferType = TransferType.RECEIVE,
                transferStatus = TransferStatus.TRANSFERRING,
                checksum = checksum
            )
            currentIncomingId = transferRepository.insertTransfer(entity)
            startForegroundService(name)
            Log.d(TAG, "Incoming transfer started: $name ($size bytes)")
        }
    }

    private fun updateIncomingProgress(read: Long, total: Long) {
        val id = currentIncomingId ?: return
        _uiState.update { state ->
            val updated = state.activeTransfers.toMutableMap()
            updated[id] = TransferProgress(id, "Incoming", read, total, 0, 0, "Transferring")
            state.copy(activeTransfers = updated)
        }
    }

    private fun finalizeIncoming(file: File, expectedChecksum: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = currentIncomingId ?: return@launch
            Log.d(TAG, "Finalizing incoming transfer: ${file.name}")
            
            // Recalculate checksum for integrity verification
            val actualChecksum = try {
                SecurityUtils.calculateChecksum(FileInputStream(file))
            } catch (e: IOException) {
                Log.e(TAG, "Checksum calculation failed: ${e.message}")
                null
            }
            
            val status = if (actualChecksum == expectedChecksum) {
                Log.d(TAG, "Checksum verified successfully")
                TransferStatus.COMPLETED
            } else {
                Log.e(TAG, "Checksum mismatch! Expected: $expectedChecksum, Actual: $actualChecksum")
                // Reject corrupted files
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Corrupted file deleted")
                }
                TransferStatus.FAILED
            }
            
            val entity = transferRepository.getTransferById(id)?.copy(
                transferStatus = status,
                filePath = if (status == TransferStatus.COMPLETED) file.absolutePath else null
            )
            entity?.let { transferRepository.updateTransfer(it) }
            
            _uiState.update { it.copy(activeTransfers = it.activeTransfers - id) }
            withContext(Dispatchers.Main) { stopForegroundService() }
        }
    }

    fun startDiscovery() {
        bluetoothManager.startDiscovery()
        wifiDirectManager.discoverPeers()
    }

    fun sendFileBluetooth(device: BluetoothDevice, uri: Uri) {
        viewModelScope.launch {
            val fileName = getFileName(uri) ?: "unnamed_file"
            val fileSize = getFileSize(uri)
            
            Log.d(TAG, "Preparing to send $fileName via Bluetooth to ${device.address}")
            
            val id = transferRepository.insertTransfer(TransferEntity(
                fileName = fileName,
                fileSize = fileSize,
                deviceName = device.name ?: device.address,
                transferType = TransferType.SEND,
                transferStatus = TransferStatus.CONNECTING
            ))
            
            startForegroundService(fileName)
            
            bluetoothManager.sendFile(
                device = device,
                uri = uri,
                onProgress = { sent, total ->
                    _uiState.update { state ->
                        val updated = state.activeTransfers.toMutableMap()
                        updated[id] = TransferProgress(id, fileName, sent, total, 0, 0, "Sending")
                        state.copy(activeTransfers = updated)
                    }
                },
                onError = { msg ->
                    Log.e(TAG, "Bluetooth send failed: $msg")
                    viewModelScope.launch {
                        transferRepository.getTransferById(id)?.let {
                            transferRepository.updateTransfer(it.copy(transferStatus = TransferStatus.FAILED))
                        }
                    }
                    _uiState.update { it.copy(errorMessage = msg, activeTransfers = it.activeTransfers - id) }
                    stopForegroundService()
                },
                onSuccess = {
                    Log.d(TAG, "Bluetooth send successful")
                    viewModelScope.launch {
                        transferRepository.getTransferById(id)?.let {
                            transferRepository.updateTransfer(it.copy(transferStatus = TransferStatus.COMPLETED))
                        }
                    }
                    _uiState.update { it.copy(activeTransfers = it.activeTransfers - id) }
                    stopForegroundService()
                }
            )
        }
    }

    private fun getFileName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) return cursor.getString(nameIndex)
        }
        return uri.path?.let { File(it).name }
    }

    private fun getFileSize(uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) return cursor.getLong(sizeIndex)
        }
        return 0
    }

    private fun startForegroundService(fileName: String) {
        val intent = Intent(context, FileTransferService::class.java).apply {
            putExtra("FILE_NAME", fileName)
        }
        context.startForegroundService(intent)
    }

    private fun stopForegroundService() {
        val intent = Intent(context, FileTransferService::class.java)
        context.stopService(intent)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.onDestroy()
        wifiDirectManager.onDestroy()
    }
}
