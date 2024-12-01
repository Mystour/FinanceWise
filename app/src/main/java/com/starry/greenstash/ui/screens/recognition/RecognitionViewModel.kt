package com.starry.greenstash.ui.screens.recognition


import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
//import com.google.ai.client.generativeai
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.starry.greenstash.BuildConfig
import com.starry.greenstash.R
import com.starry.greenstash.database.transaction.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecognitionViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema(
                    name = "transactionDetails",
                    description = "Transaction details",
                    type = FunctionType.OBJECT,
                    properties = mapOf(
                        "transactionType" to Schema(
                            name = "transactionType",
                            description = "Type of transaction",
                            type = FunctionType.STRING,
                            nullable = false
                        ),
                        "amount" to Schema(
                            name = "amount",
                            description = "Amount of transaction",
                            type = FunctionType.NUMBER,
                            nullable = false
                        ),
                        "note" to Schema(
                            name = "note",
                            description = "Note for transaction",
                            type = FunctionType.STRING,
                            nullable = false
                        )
                    ),
                    required = listOf("transactionType", "amount", "note")
                )
            }
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

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val _isAnalysisSuccessful = MutableStateFlow(false)
    val isAnalysisSuccessful = _isAnalysisSuccessful.asStateFlow()


    fun analyzeImage(bitmap: Bitmap?, inputText: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _isAnalysisSuccessful.value = false
            _isLoading = true

            analyzeImageWithGemini(bitmap, inputText) { result ->
                _analysisResult = result
                _isLoading = false
                _isAnalyzing.value = false

                val (type, amount, note) = determineTransactionTypeAndDetails(result)
                _transactionType = type
                _amount = amount
                _note = note

                println("Transaction Type: $_transactionType, Amount: $_amount, Note: $_note")

                _isAnalysisSuccessful.value = amount.isNotEmpty() && note.isNotEmpty()
            }
        }
    }


    private fun determineTransactionTypeAndDetails(analysisResult: String): Triple<TransactionType, String, String> {
        val gson = Gson()
        try {
            // 尝试解析 JSON 对象
            val jsonObject = gson.fromJson(analysisResult, JsonObject::class.java)

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
        } catch (e: Exception) {
            println(e.message)
            Timber.e(e, "其他错误: $analysisResult")
            println("其他错误: $analysisResult")
            return Triple(TransactionType.Invalid, "", "") // 其他错误，返回默认值
        }
    }


    private suspend fun analyzeImageWithGemini(
        bitmap: Bitmap?,
        inputText: String,
        callback: (String) -> Unit
    ) {
        try {
            val prompt = context.getString(R.string.initial_recognition_prompt, inputText)

            val response = if (bitmap != null) {
                generativeModel.generateContent(content { image(bitmap); text(prompt) })
            } else {
                generativeModel.generateContent(prompt)
            }

            Timber.d("Generated prompt: $prompt")

            val analysis = response.text
            if (analysis != null) {
                _analysisStream.value = analysis
            }
            analysis?.let { callback(it) }
        } catch (e: Exception) {
            Timber.e(e, "分析图片时发生错误")
            Timber.e("Error details: ${e.localizedMessage}")
            Timber.e("Stack trace: ${e.stackTraceToString()}")
            callback("分析出错: ${e.message}")
            _isLoading = false
            _isAnalyzing.value = false
        } finally {
            _isLoading = false
            _isAnalyzing.value = false
        }
    }


    fun showSnackbar(message: String, snackbarHostState: SnackbarHostState) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }
}

