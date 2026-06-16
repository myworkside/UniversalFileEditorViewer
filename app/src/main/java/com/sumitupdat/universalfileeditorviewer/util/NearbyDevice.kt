    package com.sumitupdat.universalfileeditorviewer.util

    import android.annotation.SuppressLint
    import android.bluetooth.BluetoothDevice
    import android.net.wifi.p2p.WifiP2pDevice

    sealed class NearbyDevice {
        abstract val name: String
        abstract val id: String
        abstract val status: String

        data class Bluetooth(
            val device: BluetoothDevice,
            @get:SuppressLint("MissingPermission")
            override val name: String = try { device.name ?: "Unknown Bluetooth Device" } catch (e: SecurityException) { "Restricted Device" },
            override val id: String = device.address,
            override val status: String = "Available",
            val bonded: Boolean = false
        ) : NearbyDevice()

        data class WifiDirect(
            val device: WifiP2pDevice,
            override val name: String = device.deviceName ?: "Unknown Wi-Fi Device",
            override val id: String = device.deviceAddress,
            override val status: String = when(device.status) {
                WifiP2pDevice.AVAILABLE -> "Available"
                WifiP2pDevice.CONNECTED -> "Connected"
                WifiP2pDevice.FAILED -> "Failed"
                WifiP2pDevice.INVITED -> "Invited"
                WifiP2pDevice.UNAVAILABLE -> "Unavailable"
                else -> "Unknown"
            }
        ) : NearbyDevice()
    }
