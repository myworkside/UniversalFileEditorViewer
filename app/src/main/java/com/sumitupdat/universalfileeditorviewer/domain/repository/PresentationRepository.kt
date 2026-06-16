package com.sumitupdat.universalfileeditorviewer.domain.repository

import android.content.Context
import android.util.Log
import com.sumitupdat.universalfileeditorviewer.data.model.PresentationData
import com.sumitupdat.universalfileeditorviewer.data.model.SlideData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import java.io.File
import java.io.FileInputStream

private const val TAG = "PresentationRepository"

class PresentationRepository(private val context: Context) {

    suspend fun readPresentation(file: File): PresentationData = withContext(Dispatchers.IO) {
        val slides = mutableListOf<SlideData>()
        try {
            FileInputStream(file).use { fis ->
                val slideshow = XMLSlideShow(fis)
                slideshow.slides.forEachIndexed { index, slide ->
                    val textContent = StringBuilder()
                    slide.shapes.forEach { shape ->
                        if (shape is XSLFTextShape) {
                            textContent.append(shape.text).append("\n")
                        }
                    }
                    slides.add(SlideData(
                        index = index,
                        title = slide.title ?: "Slide ${index + 1}",
                        textContent = textContent.toString()
                    ))
                }
                slideshow.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading presentation: ${file.name}", e)
        }
        PresentationData(slides)
    }
}
