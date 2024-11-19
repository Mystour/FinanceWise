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
import com.starry.greenstash.database.transaction.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RecognitionViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.apiKey
        )
    }

    private var _analysisResult by mutableStateOf("")
    private var _isLoading by mutableStateOf(false)
    private val _analysisStream = MutableStateFlow("")

    val isLoading: Boolean get() = _isLoading
    val analysisStream: StateFlow<String> get() = _analysisStream

    fun analyzeImage(bitmap: Bitmap, onAnalysisComplete: (String, TransactionType) -> Unit) {
        viewModelScope.launch {
            _isLoading = true
            analyzeImageWithGemini(bitmap) { result ->
                _analysisResult = result
                _isLoading = false

                // 解析分析结果，识别交易类型
                val transactionType = determineTransactionType(result)

                // 通过回调通知完成
                onAnalysisComplete(result, transactionType)
            }
        }
    }

    private fun determineTransactionType(analysisResult: String): TransactionType {
        return if (analysisResult.contains("deposit", ignoreCase = true) || analysisResult.contains("存入", ignoreCase = true)) {
            TransactionType.Deposit
        } else if (analysisResult.contains("withdraw", ignoreCase = true) ||
            analysisResult.contains("取出", ignoreCase = true) ||
            analysisResult.contains("支出", ignoreCase = true)) {
            TransactionType.Withdraw
        } else {
            TransactionType.Invalid
        }
    }

    private suspend fun analyzeImageWithGemini(
        bitmap: Bitmap,
        callback: (String) -> Unit
    ) {
        try {
            val prompt = context.getString(R.string.initial_recognition_prompt)
            Timber.d("Generated prompt: $prompt")
            val response = generativeModel.generateContentStream(content { image(bitmap); text(prompt) })
            var analysis = ""
            response.collect { chunk ->
                analysis += chunk.text
                _analysisStream.value = analysis
            }
            callback(analysis)
        } catch (e: Exception) {
            Timber.e(e, "分析图片时发生错误")
            Timber.e("Error details: ${e.localizedMessage}")
            Timber.e("Stack trace: ${e.stackTraceToString()}")
            callback("分析出错: ${e.message}")
        }
    }
}


