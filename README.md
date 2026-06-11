# Universal File Editor & Viewer

Universal File Editor & Viewer is a powerful Android application that allows users to browse, open, view, edit, manage, and share multiple file formats from a single app.

The goal of this project is to provide an all-in-one file management solution for Android devices, eliminating the need for multiple applications to handle different file types.

---

## Features

### File Manager

* Browse Internal Storage
* Browse SD Card Storage
* Folder Navigation
* File Search
* Copy Files
* Move Files
* Rename Files
* Delete Files
* Share Files
* Multi-Select Operations
* Recent Files
* Favorites

### Document Support

* TXT
* PDF
* DOCX
* XLSX
* PPTX
* RTF
* ODT

### Image Support

* JPG
* PNG
* GIF
* BMP
* WEBP
* HEIC
* SVG
* TIFF

### Audio Support

* MP3
* WAV
* AAC
* FLAC
* OGG
* M4A

### Video Support

* MP4
* MKV
* AVI
* MOV
* WEBM
* FLV
* 3GP

### Archive Support

* ZIP
* RAR
* 7Z
* TAR
* GZ
* ISO

### Programming File Support

* Java
* Kotlin
* Python
* C
* C++
* HTML
* CSS
* JavaScript
* PHP
* JSON
* XML
* YAML

### Database Support

* SQLite
* DB
* MDB
* SQL
* ACCDB

### Android Support Files

* APK
* AAB
* DEX
* OBB

### System & Configuration Files

* INI
* CFG
* CONF
* LOG
* XML
* JSON
* YAML

---

## Technology Stack

### Language

* Kotlin

### UI Framework

* Jetpack Compose
* Material Design 3

### Architecture

* MVVM Architecture

### Android Components

* ViewModel
* StateFlow
* Navigation Compose
* Room Database
* Coroutines

### Storage APIs

* Storage Access Framework (SAF)
* MediaStore
* FileProvider

---

## Project Structure

```text
app/
├── data/
│   ├── repository/
│   └── database/
│
├── ui/
│   ├── screens/
│   ├── components/
│   └── theme/
│
├── viewmodel/
│
├── filemanager/
│
├── viewer/
│
├── editor/
│
└── MainActivity.kt
```

---

## How It Works

### Step 1: Permission Handling

The app requests storage permissions.

Android 10:

* READ_EXTERNAL_STORAGE
* WRITE_EXTERNAL_STORAGE

Android 11+:

* MANAGE_EXTERNAL_STORAGE
* Storage Access Framework

### Step 2: File Scanning

The repository scans:

```text
Internal Storage
SD Card
Downloads
Documents
Pictures
Movies
Music
```

The file list is loaded into ViewModel.

### Step 3: UI Updates

ViewModel sends data to Compose UI using StateFlow.

```text
Repository
    ↓
ViewModel
    ↓
StateFlow
    ↓
Compose UI
```

### Step 4: Open Files

The app detects file type automatically.

Examples:

```text
PDF  → PDF Viewer
JPG  → Image Viewer
MP3  → Audio Player
MP4  → Video Player
TXT  → Text Editor
APK  → APK Analyzer
ZIP  → Archive Viewer
```

---

## Build Requirements

### Software

* Android Studio Narwhal or newer
* JDK 17
* Gradle 8+
* Android SDK 35

### Minimum Android Version

```text
Min SDK : 29
Target SDK : 35
```

---

## Installation

Clone the repository:

```bash
git clone https://github.com/myworkside/UniversalFileEditorViewer.git
```

Open Android Studio:

```text
File
→ Open
→ UniversalFileEditorViewer
```

Sync Gradle:

```text
File
→ Sync Project with Gradle Files
```

Run:

```text
Shift + F10
```

---

## Development Guide

### Add New File Types

1. Create Viewer
2. Create Editor
3. Register MIME Type
4. Update File Repository
5. Update UI Icons

### Example

```kotlin
when(extension) {
    "pdf" -> openPdf()
    "txt" -> openText()
    "jpg" -> openImage()
}
```

---

## Common Issues

### No Files Showing

Possible causes:

* Storage permission denied
* SAF permission not granted
* Repository scan failure
* ViewModel not refreshing state

### App Crashes

Check:

```text
Logcat
Build Output
AndroidManifest.xml
```

### File Not Opening

Verify:

```text
MIME type
FileProvider
URI permissions
```

---

## Roadmap

### Planned Features

* Cloud Storage Integration
* Google Drive Support
* OneDrive Support
* Dropbox Support
* Root File Explorer
* Dual Pane Explorer
* Advanced Code Editor
* AI File Assistant
* File Encryption
* Password Vault
* Built-in Media Player

---

## Contributing

Contributions are welcome.

Steps:

1. Fork Repository
2. Create Branch

```bash
git checkout -b feature-name
```

3. Commit Changes

```bash
git commit -m "Added new feature"
```

4. Push Branch

```bash
git push origin feature-name
```

5. Open Pull Request

---

## License

MIT License

You are free to use, modify, and distribute this project.

---

## Author

Sumit Mondal

GitHub:
https://github.com/myworkside

Project:
Universal File Editor & Viewer

One App. All Files.
