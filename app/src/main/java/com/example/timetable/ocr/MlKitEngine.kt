package com.example.timetable.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * 离线中文 OCR：Google ML Kit（无需联网、无需 Key）。
 */
class MlKitEngine : OcrEngine {

    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    override val label: String = "离线识别（ML Kit）"

    override suspend fun recognize(bitmap: Bitmap): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = recognizer.process(image).await()
        val lines = mutableListOf<OcrLine>()
        text.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                val b = line.boundingBox
                if (b != null) {
                    lines.add(
                        OcrLine(
                            text = line.text,
                            x = b.left, y = b.top, w = b.width(), h = b.height()
                        )
                    )
                }
            }
        }
        return OcrResult(lines, bitmap.width, bitmap.height)
    }
}
