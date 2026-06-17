package com.sumitupdat.universalfileeditorviewer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sumitupdat.universalfileeditorviewer.data.local.ThemeMode
import com.sumitupdat.universalfileeditorviewer.ui.screens.MainScreen
import com.sumitupdat.universalfileeditorviewer.ui.theme.UniversalFileEditorViewerTheme
import com.sumitupdat.universalfileeditorviewer.viewmodel.FileViewModel
import com.sumitupdat.universalfileeditorviewer.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val fileViewModel: FileViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d(TAG, "onCreate - Device API: ${Build.VERSION.SDK_INT}")

        requestPermissions()

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsState()
            val prefs = settingsState.preferences
            
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

            // Security: Disable screenshots/recording and hide from recents in Vault
            LaunchedEffect(currentRoute) {
                if (currentRoute == "vault") {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            UniversalFileEditorViewerTheme(
                darkTheme = when (prefs.theme) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
                dynamicColor = prefs.useDynamicColors,
                isAmoled = prefs.isAmoled
            ) {
                MainScreen(viewModel = fileViewModel, navController = navController)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - Refreshing ViewModel")
        fileViewModel.refresh()
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
