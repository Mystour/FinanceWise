package com.starry.greenstash.ui.screens.visualfinance

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starry.greenstash.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull.content
import java.io.ByteArrayOutputStream
import timber.log.Timber

class VisualViewModel : ViewModel() {
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.apiKey
        )
    }

    private var _analysisResult by mutableStateOf("")
    private var _isLoading by mutableStateOf(false)

    val analysisResult: String get() = _analysisResult
    val isLoading: Boolean get() = _isLoading

    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading = true
            analyzeImageWithGemini(bitmap) { result ->
                _analysisResult = result
                _isLoading = false
            }
        }
    }

    private suspend fun analyzeImageWithGemini(
        bitmap: Bitmap,
        callback: (String) -> Unit
    ) {
        try {
            val prompt = "请分析以下图片的内容，并尽可能详细地描述图片中的物品、场景、人物等信息，并推测图片背后的故事或含义。"
            Timber.d("Generated prompt: $prompt")
            val response = generativeModel.generateContent(content { image(bitmap); text(prompt) })
            Timber.d("Received response: $response")
            val analysis = response.text ?: "无法分析图片"
            callback(analysis)
        } catch (e: Exception) {
            Timber.e(e, "分析图片时发生错误")
            Timber.e("Error details: ${e.localizedMessage}")
            Timber.e("Stack trace: ${e.stackTraceToString()}")
            callback("分析出错: ${e.message}")
        }
    }

}
