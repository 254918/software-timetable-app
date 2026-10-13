package com.example.timetable.ocr

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** OCR 引擎统一接口 */
interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): OcrResult
    val label: String
}
