package com.sumitupdat.universalfileeditorviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sumitupdat.universalfileeditorviewer.ui.theme.UniversalFileEditorViewerTheme

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumitupdat.universalfileeditorviewer.data.local.AppDatabase
import com.sumitupdat.universalfileeditorviewer.domain.repository.FileRepository
import com.sumitupdat.universalfileeditorviewer.ui.screens.FileBrowserScreen
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel
import androidx.compose.runtime.remember

import com.sumitupdat.universalfileeditorviewer.ui.screens.MainScreen
import com.sumitupdat.universalfileeditorviewer.ui.theme.UniversalFileEditorViewerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestPermissions()

        setContent {
            UniversalFileEditorViewerTheme {
                val database = AppDatabase.getDatabase(this)
                val repository = FileRepository(database.fileDao())
                val fileViewModel: FileViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return FileViewModel(repository) as T
                        }
                    }
                )

                MainScreen(viewModel = fileViewModel)
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        } else {
            // Request READ/WRITE for older versions
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
    ) { _ ->
        // Handle results if needed
    }
}
