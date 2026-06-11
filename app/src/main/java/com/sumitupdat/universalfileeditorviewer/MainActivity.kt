package com.sumitupdat.universalfileeditorviewer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.sumitupdat.universalfileeditorviewer.data.local.AppDatabase
import com.sumitupdat.universalfileeditorviewer.domain.repository.FileRepository
import com.sumitupdat.universalfileeditorviewer.ui.screens.MainScreen
import com.sumitupdat.universalfileeditorviewer.ui.theme.UniversalFileEditorViewerTheme
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    private lateinit var fileViewModel: FileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d(TAG, "onCreate - Device API: ${Build.VERSION.SDK_INT}")

        val database = AppDatabase.getDatabase(this)
        val repository = FileRepository(database.fileDao(), this)
        fileViewModel = androidx.lifecycle.ViewModelProvider(this, object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return FileViewModel(repository) as T
            }
        })[FileViewModel::class.java]

        requestPermissions()

        setContent {
            UniversalFileEditorViewerTheme {
                MainScreen(viewModel = fileViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - Refreshing ViewModel")
        if (::fileViewModel.isInitialized) {
            fileViewModel.refresh()
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Log.d(TAG, "Requesting MANAGE_EXTERNAL_STORAGE")
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.w(TAG, "ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION failed, falling back", e)
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            } else {
                Log.d(TAG, "MANAGE_EXTERNAL_STORAGE already granted")
            }
        } else {
            Log.d(TAG, "Requesting READ/WRITE_EXTERNAL_STORAGE for API < 30")
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d(TAG, "Permissions granted by user")
            fileViewModel.refresh()
        } else {
            Log.w(TAG, "Permissions denied by user")
            Toast.makeText(this, "Permissions required to show files", Toast.LENGTH_LONG).show()
        }
    }
}
