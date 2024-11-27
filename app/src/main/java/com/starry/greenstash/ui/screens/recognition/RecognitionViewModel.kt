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
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.starry.greenstash.BuildConfig
import com.starry.greenstash.R
import com.starry.greenstash.database.transaction.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val _isAnalysisSuccessful = MutableStateFlow(false)
    val isAnalysisSuccessful = _isAnalysisSuccessful.asStateFlow()


    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAnalyzing.value = true  // 开始分析
            _isAnalysisSuccessful.value = false // 重置分析成功状态
            _isLoading = true
            analyzeImageWithGemini(bitmap) { result ->
                _analysisResult = result
                _isLoading = false
                _isAnalyzing.value = false  // 分析结束

                // 解析分析结果
                val (type, amount, note) = determineTransactionTypeAndDetails(result)
                _transactionType = type
                _amount = amount
                _note = note

                println("Transaction Type: $_transactionType, Amount: $_amount, Note: $_note")

                // 根据解析结果设置分析成功状态
                _isAnalysisSuccessful.value = amount.isNotEmpty() && note.isNotEmpty()
            }
        }
    }


    private fun determineTransactionTypeAndDetails(analysisResult: String): Triple<TransactionType, String, String> {
        val gson = Gson()
        try {
            // 使用正则表达式提取 JSON 字符串
            val jsonPattern = Regex("`{3}json\\s*(.*?)\\s*`{3}", RegexOption.DOT_MATCHES_ALL)
            val matchResult = jsonPattern.find(analysisResult)
            val cleanedJson = matchResult?.groupValues?.get(1)?.trim() ?: ""

            println("Cleaned JSON: $cleanedJson")  // 打印清理后的 Json
            val jsonObject = gson.fromJson(cleanedJson, JsonObject::class.java)

            val typeString = jsonObject.get("transactionType")?.asString ?: "不明确"
            val type = when (typeString.lowercase()) {
                "存入", "deposit" -> TransactionType.Deposit
                "取出", "withdraw" -> TransactionType.Withdraw
                else -> TransactionType.Invalid
            }

            val amount = when (val amountJson: JsonElement? = jsonObject.get("amount")) {
                is com.google.gson.JsonPrimitive -> {
                    if (amountJson.isNumber) amountJson.asNumber.toString() else amountJson.asString
                }
                else -> ""
            }
            val note = jsonObject.get("note")?.asString ?: ""

            return Triple(type, amount, note)

        } catch (e: JsonSyntaxException) {
            println(e.message)
            Timber.e(e, "JSON 解析错误: $analysisResult")
            println("JSON 解析错误: $analysisResult")
            return Triple(TransactionType.Invalid, "", "") // 解析失败，返回默认值
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
