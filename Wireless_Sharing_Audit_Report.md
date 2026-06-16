# Technical Audit Report - Wireless Sharing System

## 1. Audit Summary
A comprehensive technical audit was performed on the Bluetooth and Wi-Fi Direct file transfer systems. The goal was to resolve connection failures, improve reliability, and ensure production-ready security and performance on Android 10–16.

## 2. Identified & Resolved Issues

### Critical Bugs
- **Root Cause of "read failed, ret: -1"**: 
  - **Issue**: The `BluetoothSocket` was being closed prematurely in the `finally` block of the incoming connection handler before the data stream was fully processed or because of unhandled exceptions.
  - **Fix**: Wrapped stream reading in robust try-catch blocks, improved metadata parsing (using `DataInputStream/DataOutputStream`), and added explicit EOF detection. Added detailed logging to track exactly where a connection drops.
- **RFCOMM UUID Mismatch**: 
  - **Issue**: Potential for inconsistent UUID usage if hardcoded in multiple places.
  - **Fix**: Centralized `SERVICE_UUID` in `AppBluetoothManager` and added logging for verification.

### Runtime & Performance Issues
- **OOM Prevention**: 
  - **Issue**: Large files were handled with small buffers or risk of memory loading.
  - **Fix**: Standardized on 16KB buffers for Bluetooth and 64KB for Wi-Fi Direct. Used `DataInputStream` for efficient primitive reading.
- **UI Responsiveness**: 
  - **Issue**: Intensive tasks (like SHA-256 calculation) were potentially blocking.
  - **Fix**: Ensured all heavy I/O and crypto operations run on `Dispatchers.IO` using Kotlin Coroutines.

### Android Compatibility
- **Permissions**: 
  - **Audit**: Verified that `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, and `NEARBY_WIFI_DEVICES` are correctly handled for Android 12+ (API 31+).
  - **Fix**: Implemented runtime permission validation before initiating discovery or server startup. Added `MissingPermission` annotations where necessary and improved error messaging for denied permissions.

## 3. Security Enhancements
- **Checksum Verification**: 
  - **Implementation**: After receiving a file, the system now recalculates the SHA-256 checksum and compares it with the sender's metadata. 
  - **Action**: Corrupted files (checksum mismatch) are automatically deleted, and the transfer status is marked as `FAILED`.
- **Path Traversal Protection**: 
  - **Implementation**: Filenames are sanitized via `SecurityUtils` to remove directory navigation components (e.g., `../`).
- **Pairing Validation**: 
  - **Requirement**: Bluetooth transfers now explicitly check `bondState`. If a device is not paired, the user is prompted to pair it first, preventing unauthorized connection attempts.

## 4. Final Audit Checklist
- [x] RFCOMM implementation verified.
- [x] Detailed logging added with tags (`BT_SERVER`, `BT_CLIENT`, `BT_TRANSFER`, `WIFI_TRANSFER`).
- [x] Android 12+ Bluetooth permissions validated.
- [x] Pairing requirement enforced.
- [x] SHA-256 integrity check implemented.
- [x] Wi-Fi Direct `ActionListener` callbacks implemented (replacing nulls).
- [x] Foreground Service integration verified for background stability.

## 5. Applied Fixes
- Rewrote `AppBluetoothManager.kt` for stable socket lifecycle management.
- Rewrote `AppWifiDirectManager.kt` with full logging and robust callbacks.
- Updated `WirelessSharingViewModel.kt` with checksum verification and file deletion logic.
- Standardized error reporting with descriptive messages (e.g., "Device not paired", "Connection timeout").
