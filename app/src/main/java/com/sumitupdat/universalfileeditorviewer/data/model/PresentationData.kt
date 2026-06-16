package com.sumitupdat.universalfileeditorviewer.data.model

import android.graphics.Bitmap

data class PresentationData(
    val slides: List<SlideData>
)

data class SlideData(
    val index: Int,
    val title: String,
    val textContent: String,
    val preview: Bitmap? = null
)
