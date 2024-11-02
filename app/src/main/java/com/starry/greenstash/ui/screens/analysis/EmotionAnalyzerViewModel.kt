package com.starry.greenstash.ui.screens.analysis

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.starry.greenstash.BuildConfig
import com.starry.greenstash.R
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class EmotionAnalyzerViewModel(private val context: Context) : ViewModel() {
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.apiKey
        )
    }

    private val _billText = mutableStateOf("")
    val billText: String
        get() = _billText.value

    private val _analysisResult = mutableStateOf("")
    val analysisResult: String
        get() = _analysisResult.value

    private val _emotionScore = mutableIntStateOf(0)
    val emotionScore: Int
        get() = _emotionScore.intValue

    private val _emotionComment = mutableStateOf("")
    val emotionComment: String
        get() = _emotionComment.value

    private val _isLoading = mutableStateOf(false)
    val isLoading: Boolean
        get() = _isLoading.value

    fun setBillText(text: String) {
        _billText.value = text
    }

    fun setAnalysisResult(result: String) {
        _analysisResult.value = result
    }

    fun setEmotionScore(score: Int) {
        _emotionScore.intValue = score
    }

    fun setEmotionComment(comment: String) {
        _emotionComment.value = comment
    }

    fun setIsLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun initializeBillText(goals: String?) {
        setBillText(goals ?: "")
    }

    fun analyzeBill() {
        viewModelScope.launch {
            setIsLoading(true)
            val formattedBill = if (isJson(_billText.value)) {
                _billText.value
            } else {
                formatBillText(_billText.value)
            }

            // 第一步：生成包含情绪分数和评论的初步分析结果
            val initialAnalysis = generateInitialAnalysis(formattedBill)
            val (score, comment) = extractEmotionInfo(initialAnalysis)
            setEmotionScore(score)
            setEmotionComment(comment)

            // 第二步：生成详细的分析内容
            generateDetailedAnalysis(formattedBill) { result ->
                setAnalysisResult(result)
                setIsLoading(false)
            }
        }
    }

    private suspend fun formatBillText(billText: String): String {
        val prompt = context.getString(R.string.format_bill_text_prompt, billText)

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: billText
        } catch (e: Exception) {
            billText
        }
    }


    private fun isJson(text: String): Boolean {
        return try {
            Gson().fromJson(text, Any::class.java)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun generateInitialAnalysis(billText: String): String {
        val prompt = createInitialAnalysisPrompt(billText)
        val response = generativeModel.generateContent(prompt)
        println("Gemini Response: ${response.text}")
        return response.text ?: ""
    }

    private suspend fun generateDetailedAnalysis(billText: String, callback: (String) -> Unit) {
        val prompt = createDetailedAnalysisPrompt(billText)
        val response = generativeModel.generateContentStream(prompt)
        var analysis = ""
        response.collect { chunk ->
            analysis += chunk.text
            callback(analysis)
        }
    }

    private fun extractEmotionInfo(analysis: String): Pair<Int, String> {
        val scorePattern = context.getString(R.string.score_pattern)
        val commentPattern = context.getString(R.string.comment_pattern)


        val scoreRegex = Pattern.compile(scorePattern, Pattern.DOTALL or Pattern.MULTILINE)
        val commentRegex = Pattern.compile(commentPattern, Pattern.DOTALL or Pattern.MULTILINE)


        val scoreMatcher = scoreRegex.matcher(analysis)
        val commentMatcher = commentRegex.matcher(analysis)

        val score = if (scoreMatcher.find()) {
            scoreMatcher.group(1)?.toIntOrNull() ?: 0
        } else {
            80 // 默认分数
        }

        val comment = if (commentMatcher.find()) {
            commentMatcher.group(1)?.replace("*", "") ?: ""
        } else {
            context.getString(R.string.default_emotion_comment)
        }

        println("Extracted Score: $score")
        println("Extracted Comment: $comment")

        return Pair(score, comment)
    }

    private fun createInitialAnalysisPrompt(billText: String): String {
        val initialPrompt = context.getString(R.string.initial_analysis_prompt)
        return String.format(initialPrompt, billText)
    }

    private fun createDetailedAnalysisPrompt(billText: String): String {
        val detailedPrompt = context.getString(R.string.detailed_analysis_prompt)
        return String.format(detailedPrompt, billText)
    }
}
