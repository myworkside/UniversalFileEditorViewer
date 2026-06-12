package com.sumitupdat.universalfileeditorviewer.ui.screens.viewers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sumitupdat.universalfileeditorviewer.data.model.PresentationData
import com.sumitupdat.universalfileeditorviewer.ui.components.ZoomableBox

@Composable
fun PptxViewerScreen(
    data: PresentationData,
    modifier: Modifier = Modifier
) {
    var currentSlideIndex by remember { mutableIntStateOf(0) }
    val currentSlide = data.slides.getOrNull(currentSlideIndex)

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            currentSlide?.let { slide ->
                ZoomableBox(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = slide.textContent,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }

        // Navigation Controls
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentSlideIndex > 0) currentSlideIndex-- },
                    enabled = currentSlideIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Slide")
                }

                Text(
                    text = "Slide ${currentSlideIndex + 1} of ${data.slides.size}",
                    style = MaterialTheme.typography.labelLarge
                )

                IconButton(
                    onClick = { if (currentSlideIndex < data.slides.size - 1) currentSlideIndex++ },
                    enabled = currentSlideIndex < data.slides.size - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Slide")
                }
            }
        }
    }
}
