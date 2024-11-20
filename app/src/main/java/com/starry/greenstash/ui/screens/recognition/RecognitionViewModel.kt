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
    private var _transactionType by mutableStateOf(TransactionType.Invalid)
    private var _amount by mutableStateOf("")
    private var _note by mutableStateOf("")

    val transactionType: TransactionType get() = _transactionType
    val amount: String get() = _amount
    val note: String get() = _note

    val isLoading: Boolean get() = _isLoading
    val analysisStream: StateFlow<String> get() = _analysisStream

    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading = true
            analyzeImageWithGemini(bitmap) { result ->
                _analysisResult = result
                _isLoading = false

                // 解析分析结果，识别交易类型、金额和备注
                val (type, amount, note) = determineTransactionTypeAndDetails(result)
                _transactionType = type
                _amount = amount
                _note = note
            }
        }
    }

    private fun determineTransactionTypeAndDetails(analysisResult: String): Triple<TransactionType, String, String> {
        // 去除字符串中的 * 符号
        val cleanedResult = analysisResult.replace("*", "")

        val type = if (cleanedResult.contains("不明确", ignoreCase = true)) {
            TransactionType.Invalid
        } else if (cleanedResult.contains("deposit", ignoreCase = true) || cleanedResult.contains("存入", ignoreCase = true)) {
            TransactionType.Deposit
        } else if (cleanedResult.contains("withdraw", ignoreCase = true) ||
            cleanedResult.contains("取出", ignoreCase = true) ||
            cleanedResult.contains("支出", ignoreCase = true)) {
            TransactionType.Withdraw
        } else {
            TransactionType.Invalid
        }

        val amount = extractAmount(cleanedResult)
        val note = extractNote(cleanedResult)

        return Triple(type, amount, note)
    }


    private fun extractAmount(analysisResult: String): String {
        val currencySymbols = listOf("$", "€", "£", "¥", "₹", "元", "美元", "欧元", "英镑", "日元", "卢比")
        val amountPattern = Regex("\\b\\d+(\\.\\d+)?\\s*(?:" + currencySymbols.joinToString("|") { Regex.escape(it) } + ")?")
        val matchResult = amountPattern.find(analysisResult)
        return matchResult?.value?.replace(Regex("\\s*[$€£¥₹元美欧英镑日卢比]\\s*"), "") ?: ""
    }


    private fun extractNote(analysisResult: String): String {
        val notePattern = Regex("(?<=备注:|note:).+")
        val matchResult = notePattern.find(analysisResult)
        return matchResult?.value?.trim() ?: ""
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
