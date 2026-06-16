package com.sumitupdat.universalfileeditorviewer.util

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val BT_SERVER = "BT_SERVER"
private const val BT_CLIENT = "BT_CLIENT"
private const val BT_TRANSFER = "BT_TRANSFER"

@Singleton
class AppBluetoothManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _discoveredDevices = MutableStateFlow<List<NearbyDevice.Bluetooth>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val SERVICE_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    private val APP_NAME = "UniversalFileEditor"

    private var serverJob: Job? = null
    private var serverSocket: BluetoothServerSocket? = null

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        val currentList = _discoveredDevices.value
                        if (currentList.none { it.id == device.address }) {
                            _discoveredDevices.value = currentList + NearbyDevice.Bluetooth(device)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        
        _discoveredDevices.value = emptyList()
        _isScanning.value = true
        bluetoothAdapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun startServer(
        onIncomingTransfer: (String, Long, String) -> Unit,
        onProgress: (Long, Long) -> Unit,
        onFileReceived: (File, String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (bluetoothAdapter == null) {
            onError("Bluetooth not supported")
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            onError("Bluetooth disabled")
            return
        }

        serverJob?.cancel()
        serverJob = scope.launch {
            try {
                Log.d(BT_SERVER, "Starting server with UUID: $SERVICE_UUID")
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, SERVICE_UUID)
                Log.d(BT_SERVER, "Server started, waiting for connection...")
                
                while (isActive) {
                    val socket = try {
                        serverSocket?.accept()
                    } catch (e: IOException) {
                        Log.e(BT_SERVER, "Socket accept failed: ${e.message}")
                        null
                    } ?: break
                    
                    Log.d(BT_SERVER, "Connection accepted from ${socket.remoteDevice.name ?: socket.remoteDevice.address}")
                    handleIncomingConnection(socket, onIncomingTransfer, onProgress, onFileReceived, onError)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(BT_SERVER, "Server error: ${e.message}")
                    onError("Receiver unavailable")
                }
            } finally {
                stopServer()
            }
        }
    }

    private suspend fun handleIncomingConnection(
        socket: BluetoothSocket,
        onIncomingTransfer: (String, Long, String) -> Unit,
        onProgress: (Long, Long) -> Unit,
        onFileReceived: (File, String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val dis = DataInputStream(socket.inputStream)
            
            Log.d(BT_TRANSFER, "Reading metadata...")
            val fileName = dis.readUTF()
            val fileSize = dis.readLong()
            val checksum = dis.readUTF()
            Log.d(BT_TRANSFER, "Metadata received: $fileName ($fileSize bytes)")
            
            val sanitizedName = SecurityUtils.sanitizeFileName(fileName)
            onIncomingTransfer(sanitizedName, fileSize, checksum)
            
            val receivedDir = File(context.getExternalFilesDir(null), "Received")
            if (!receivedDir.exists()) receivedDir.mkdirs()
            val receivedFile = File(receivedDir, sanitizedName)
            
            val fos = FileOutputStream(receivedFile)
            val buffer = ByteArray(16384)
            var totalRead = 0L
            var lastUpdate = 0L

            Log.d(BT_TRANSFER, "Transfer started...")
            while (totalRead < fileSize) {
                val toRead = minOf(buffer.size.toLong(), fileSize - totalRead).toInt()
                val read = try {
                    dis.read(buffer, 0, toRead)
                } catch (e: IOException) {
                    Log.e(BT_TRANSFER, "Read failed at $totalRead bytes: ${e.message}")
                    -1
                }
                
                if (read == -1) {
                    Log.e(BT_TRANSFER, "Premature EOF at $totalRead bytes")
                    break
                }
                
                fos.write(buffer, 0, read)
                totalRead += read
                
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 500) {
                    onProgress(totalRead, fileSize)
                    lastUpdate = now
                }
            }
            fos.flush()
            fos.close()
            
            if (totalRead == fileSize) {
                Log.d(BT_TRANSFER, "Transfer completed successfully")
                onFileReceived(receivedFile, checksum)
            } else {
                Log.e(BT_TRANSFER, "Transfer failed: $totalRead / $fileSize bytes read")
                onError("Transfer interrupted")
            }
        } catch (e: Exception) {
            Log.e(BT_TRANSFER, "Handle incoming failed: ${e.message}")
            onError("Transfer failed")
        } finally {
            try {
                socket.close()
                Log.d(BT_SERVER, "Socket closed")
            } catch (e: IOException) {
                Log.e(BT_SERVER, "Error closing socket: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun sendFile(
        device: BluetoothDevice,
        uri: Uri,
        onProgress: (Long, Long) -> Unit,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) = withContext(Dispatchers.IO) {
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            onError("Device not paired")
            return@withContext
        }

        var socket: BluetoothSocket? = null
        try {
            Log.d(BT_CLIENT, "Connecting to ${device.name ?: device.address}...")
            bluetoothAdapter?.cancelDiscovery()
            socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
            
            try {
                socket.connect()
                Log.d(BT_CLIENT, "Connected successfully")
            } catch (e: IOException) {
                Log.e(BT_CLIENT, "Connection failed: ${e.message}")
                onError("Connection timeout")
                return@withContext
            }
            
            val dos = DataOutputStream(socket.outputStream)
            val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open Uri")
            
            val fileName = getFileName(uri) ?: "unnamed_file"
            val fileSize = getFileSize(uri)
            
            Log.d(BT_TRANSFER, "Calculating checksum...")
            val checksum = SecurityUtils.calculateChecksum(context.contentResolver.openInputStream(uri)!!)
            
            Log.d(BT_TRANSFER, "Sending metadata for $fileName...")
            dos.writeUTF(fileName)
            dos.writeLong(fileSize)
            dos.writeUTF(checksum)
            dos.flush()
            
            val buffer = ByteArray(16384)
            var totalSent = 0L
            var lastUpdate = 0L
            
            Log.d(BT_TRANSFER, "Transfer started...")
            while (isActive) {
                val read = inputStream.read(buffer)
                if (read == -1) break
                
                try {
                    dos.write(buffer, 0, read)
                } catch (e: IOException) {
                    Log.e(BT_TRANSFER, "Write failed at $totalSent bytes: ${e.message}")
                    throw e
                }
                
                totalSent += read
                
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 500) {
                    onProgress(totalSent, fileSize)
                    lastUpdate = now
                }
            }
            dos.flush()
            inputStream.close()
            
            if (totalSent == fileSize) {
                Log.d(BT_TRANSFER, "Transfer completed")
                onSuccess()
            } else {
                Log.e(BT_TRANSFER, "Transfer interrupted: $totalSent / $fileSize sent")
                onError("Transfer interrupted")
            }
            
        } catch (e: Exception) {
            Log.e(BT_TRANSFER, "Send failed: ${e.message}")
            onError("Transfer failed")
        } finally {
            try {
                socket?.close()
                Log.d(BT_CLIENT, "Socket closed")
            } catch (e: IOException) {
                Log.e(BT_CLIENT, "Error closing socket: ${e.message}")
            }
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

    fun stopServer() {
        Log.d(BT_SERVER, "Stopping server...")
        serverJob?.cancel()
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(BT_SERVER, "Error closing server socket: ${e.message}")
        }
        serverSocket = null
    }

    fun onDestroy() {
        stopDiscovery()
        stopServer()
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {}
    }
}
