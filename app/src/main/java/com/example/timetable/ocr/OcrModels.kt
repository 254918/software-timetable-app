package com.example.timetable.ocr

/**
 * OCR 行级结果（带坐标），ML Kit 与百度云端统一为同一结构，
 * 供版面分析使用。坐标相对原图。
 */
data class OcrLine(
    val text: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int
)

data class OcrResult(
    val lines: List<OcrLine>,
    val imageWidth: Int,
    val imageHeight: Int
)
