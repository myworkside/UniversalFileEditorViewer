package com.sumitupdat.universalfileeditorviewer.ui.screens.devtools

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun DevToolsScreen(
    onBack: () -> Unit
) {
    DevToolsWorkspace(onBack = onBack)
}
