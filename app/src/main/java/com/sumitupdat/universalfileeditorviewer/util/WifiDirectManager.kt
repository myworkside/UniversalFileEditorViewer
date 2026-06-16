package com.sumitupdat.universalfileeditorviewer.util

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.p2p.*
import android.provider.OpenableColumns
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

private const val WIFI_P2P = "WIFI_P2P"
private const val WIFI_TRANSFER = "WIFI_TRANSFER"

@Singleton
class AppWifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: WifiP2pManager.Channel? = manager?.initialize(context, context.mainLooper, null)

    private val _peers = MutableStateFlow<List<NearbyDevice.WifiDirect>>(emptyList())
    val peers = _peers.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo = _connectionInfo.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val PORT = 8888
    private var serverJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d(WIFI_P2P, "Peers changed, requesting peers...")
                    manager?.requestPeers(channel) { peerList ->
                        _peers.value = peerList.deviceList.map { NearbyDevice.WifiDirect(it) }
                        Log.d(WIFI_P2P, "Found ${_peers.value.size} peers")
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    Log.d(WIFI_P2P, "Connection changed, requesting info...")
                    manager?.requestConnectionInfo(channel) { info ->
                        _connectionInfo.value = info
                        Log.d(WIFI_P2P, "Group formed: ${info.groupFormed}, Owner: ${info.isGroupOwner}")
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        Log.d(WIFI_P2P, "Starting peer discovery...")
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d(WIFI_P2P, "Discovery started successfully") }
            override fun onFailure(reason: Int) { Log.e(WIFI_P2P, "Discovery failed: $reason") }
        })
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice) {
        Log.d(WIFI_P2P, "Connecting to ${device.deviceName}...")
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d(WIFI_P2P, "Connect initiated") }
            override fun onFailure(reason: Int) { Log.e(WIFI_P2P, "Connect failed: $reason") }
        })
    }

    fun startServer(
        onIncomingTransfer: (String, Long, String) -> Unit,
        onProgress: (Long, Long) -> Unit,
        onFileReceived: (File, String) -> Unit,
        onError: (String) -> Unit
    ) {
        serverJob?.cancel()
        serverJob = scope.launch {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(WIFI_TRANSFER, "Server started on port $PORT, waiting for connection...")
                while (isActive) {
                    val client = serverSocket.accept()
                    Log.d(WIFI_TRANSFER, "Connection accepted from ${client.inetAddress.hostAddress}")
                    handleIncomingTransfer(client, onIncomingTransfer, onProgress, onFileReceived, onError)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(WIFI_TRANSFER, "Server error: ${e.message}")
                    onError("Wi-Fi Server error")
                }
            } finally {
                serverSocket?.close()
                Log.d(WIFI_TRANSFER, "Server socket closed")
            }
        }
    }

    private suspend fun handleIncomingTransfer(
        socket: Socket,
        onIncomingTransfer: (String, Long, String) -> Unit,
        onProgress: (Long, Long) -> Unit,
        onFileReceived: (File, String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val dis = DataInputStream(socket.getInputStream())
            
            Log.d(WIFI_TRANSFER, "Reading metadata...")
            val fileName = dis.readUTF()
            val fileSize = dis.readLong()
            val checksum = dis.readUTF()
            Log.d(WIFI_TRANSFER, "Metadata: $fileName ($fileSize bytes)")
            
            val sanitizedName = SecurityUtils.sanitizeFileName(fileName)
            onIncomingTransfer(sanitizedName, fileSize, checksum)
            
            val receivedDir = File(context.getExternalFilesDir(null), "Received")
            receivedDir.mkdirs()
            val receivedFile = File(receivedDir, sanitizedName)
            
            val fos = FileOutputStream(receivedFile)
            val buffer = ByteArray(65536)
            var totalRead = 0L
            var lastUpdate = 0L

            Log.d(WIFI_TRANSFER, "Transfer started...")
            while (totalRead < fileSize) {
                val toRead = minOf(buffer.size.toLong(), fileSize - totalRead).toInt()
                val read = try {
                    dis.read(buffer, 0, toRead)
                } catch (e: IOException) {
                    Log.e(WIFI_TRANSFER, "Read error: ${e.message}")
                    -1
                }
                
                if (read == -1) break
                
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
                Log.d(WIFI_TRANSFER, "Transfer completed")
                onFileReceived(receivedFile, checksum)
            } else {
                Log.e(WIFI_TRANSFER, "Transfer incomplete: $totalRead / $fileSize")
                onError("Wi-Fi Transfer incomplete")
            }
        } catch (e: Exception) {
            Log.e(WIFI_TRANSFER, "Handle incoming failed: ${e.message}")
            onError("Wi-Fi Receive failed")
        } finally {
            socket.close()
            Log.d(WIFI_TRANSFER, "Client socket closed")
        }
    }

    suspend fun sendFile(
        host: String,
        uri: Uri,
        onProgress: (Long, Long) -> Unit,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val socket = Socket()
        try {
            Log.d(WIFI_TRANSFER, "Connecting to $host:$PORT...")
            socket.connect(InetSocketAddress(host, PORT), 10000)
            Log.d(WIFI_TRANSFER, "Connected successfully")
            
            val dos = DataOutputStream(socket.getOutputStream())
            
            val fileName = getFileName(uri) ?: "unnamed_file"
            val fileSize = getFileSize(uri)
            
            Log.d(WIFI_TRANSFER, "Calculating checksum...")
            val checksum = SecurityUtils.calculateChecksum(context.contentResolver.openInputStream(uri)!!)
            
            Log.d(WIFI_TRANSFER, "Sending metadata for $fileName...")
            dos.writeUTF(fileName)
            dos.writeLong(fileSize)
            dos.writeUTF(checksum)
            dos.flush()
            
            val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open Uri")
            val buffer = ByteArray(65536)
            var totalSent = 0L
            var lastUpdate = 0L

            Log.d(WIFI_TRANSFER, "Transfer started...")
            while (isActive) {
                val read = inputStream.read(buffer)
                if (read == -1) break
                
                try {
                    dos.write(buffer, 0, read)
                } catch (e: IOException) {
                    Log.e(WIFI_TRANSFER, "Write error: ${e.message}")
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
                Log.d(WIFI_TRANSFER, "Transfer successful")
                onSuccess()
            } else {
                Log.e(WIFI_TRANSFER, "Transfer interrupted: $totalSent / $fileSize")
                onError("Wi-Fi Transfer interrupted")
            }
        } catch (e: Exception) {
            Log.e(WIFI_TRANSFER, "Send failed: ${e.message}")
            onError("Wi-Fi Send failed")
        } finally {
            socket.close()
            Log.d(WIFI_TRANSFER, "Socket closed")
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

    fun onDestroy() {
        Log.d(WIFI_P2P, "Destroying WifiDirectManager")
        serverJob?.cancel()
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {}
    }
}
