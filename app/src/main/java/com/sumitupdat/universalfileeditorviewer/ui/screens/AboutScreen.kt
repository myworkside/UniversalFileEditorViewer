package com.sumitupdat.universalfileeditorviewer.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    val versionManager = remember {
        object {
            val name = "2.2.0"
            val code = "12"
            val buildDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("About", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. App Information
            item {
                AboutSectionTitle("App Information")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        AboutListItem(
                            title = "App Name",
                            subtitle = "Universal File Editor & Viewer",
                            icon = Icons.Outlined.Description
                        )
                        AboutListItem(
                            title = "Version Name",
                            subtitle = versionManager.name,
                            icon = Icons.Outlined.Info
                        )
                        AboutListItem(
                            title = "Version Code",
                            subtitle = versionManager.code,
                            icon = Icons.Outlined.Numbers
                        )
                        AboutListItem(
                            title = "Build Date",
                            subtitle = versionManager.buildDate,
                            icon = Icons.Outlined.CalendarToday
                        )
                    }
                }
            }

            // 2. Developer
            item {
                AboutSectionTitle("Developer")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        AboutListItem(
                            title = "Developer",
                            subtitle = "Sumit Mondal",
                            icon = Icons.Outlined.Person
                        )
                        AboutListItem(
                            title = "GitHub Profile",
                            subtitle = "github.com/myworkside",
                            icon = Icons.Outlined.Code,
                            onClick = { openUrl(context, "https://github.com/myworkside/UniversalFileEditorViewer") }
                        )
                        AboutListItem(
                            title = "Instagram",
                            subtitle = "@sumitupdat",
                            icon = Icons.Outlined.Link,
                            onClick = { openInstagram(context) }
                        )
                    }
                }
            }

            // 3. Project
            item {
                AboutSectionTitle("Project")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        AboutListItem(
                            title = "Open Source Project",
                            subtitle = "Proudly open source and community driven",
                            icon = Icons.Outlined.Handshake
                        )
                        AboutListItem(
                            title = "View Source Code",
                            subtitle = "Explore the codebase on GitHub",
                            icon = Icons.Outlined.Source,
                            onClick = { openUrl(context, "https://github.com/myworkside/UniversalFileEditorViewer") }
                        )
                        AboutListItem(
                            title = "Report Bug",
                            subtitle = "Help us improve by reporting issues",
                            icon = Icons.Outlined.BugReport,
                            onClick = { openUrl(context, "https://github.com/myworkside/UniversalFileEditorViewer/issues") }
                        )
                        AboutListItem(
                            title = "Request Feature",
                            subtitle = "Share your ideas for new features",
                            icon = Icons.Outlined.Lightbulb,
                            onClick = { openUrl(context, "https://github.com/myworkside/UniversalFileEditorViewer/issues") }
                        )
                    }
                }
            }

            // 4. Features
            item {
                AboutSectionTitle("Features")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val features = listOf(
                            "Universal File Management", "Private Encrypted Vault",
                            "Wireless Sharing", "Storage Analyzer", "Developer Tools",
                            "Document Viewer", "Media Player"
                        )
                        features.forEach { feature ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(feature, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // 5. Technology Stack
            item {
                AboutSectionTitle("Technology Stack")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val techStack = listOf(
                            "Kotlin", "Jetpack Compose", "Material Design 3",
                            "Room Database", "Coroutines", "Hilt Dependency Injection"
                        )
                        techStack.forEach { tech ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Memory, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(tech, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // 6. Support
            item {
                AboutSectionTitle("Support")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        AboutListItem(
                            title = "Rate App",
                            subtitle = "Show your support on Play Store",
                            icon = Icons.Outlined.StarRate,
                            onClick = { /* Open Play Store */ }
                        )
                        AboutListItem(
                            title = "Share App",
                            subtitle = "Invite your friends to use the app",
                            icon = Icons.Outlined.Share,
                            onClick = { shareApp(context) }
                        )
                        AboutListItem(
                            title = "Privacy Policy",
                            subtitle = "How we protect your data",
                            icon = Icons.Outlined.PrivacyTip,
                            onClick = { openUrl(context, "https://github.com/myworkside/UniversalFileEditorViewer/blob/main/PRIVACY_POLICY.md") }
                        )
                        AboutListItem(
                            title = "Terms of Service",
                            subtitle = "App usage rules and guidelines",
                            icon = Icons.Outlined.Gavel,
                            onClick = { /* Open TOS */ }
                        )
                    }
                }
            }

            // 7. Footer
            item {
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Made with ❤️ by Sumit Mondal",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "© ${Calendar.getInstance().get(Calendar.YEAR)} Universal File Editor. All rights reserved.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AboutSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun AboutListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = onClick?.let { { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline) } },
        modifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}

private fun openInstagram(context: Context) {
    val uri = Uri.parse("https://www.instagram.com/sumitupdat")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.instagram.android")
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/sumitupdat")))
    }
}

private fun shareApp(context: Context) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Check out Universal File Editor & Viewer: https://github.com/myworkside/UniversalFileEditorViewer")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
