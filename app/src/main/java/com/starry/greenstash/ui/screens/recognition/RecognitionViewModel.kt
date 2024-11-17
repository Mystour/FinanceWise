package com.starry.greenstash.ui.screens.recognition

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.starry.greenstash.BuildConfig
import com.starry.greenstash.R
import com.starry.greenstash.database.goal.GoalDao
import com.starry.greenstash.utils.PreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RecognitionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
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
            // 从资源文件中读取提示文本
            val prompt = context.getString(R.string.initial_recognition_prompt)
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
