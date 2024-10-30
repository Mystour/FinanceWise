package com.starry.greenstash.ui.screens.analysis

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.starry.greenstash.BuildConfig
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class BillAnalyzerViewModel : ViewModel() {
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
        get() = _emotionScore.value

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
        _emotionScore.value = score
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
                _billText.value // 如果是 JSON 格式，不进行格式化
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
        val prompt = """
            请将以下账单文本转换为规范的表格格式，包含以下列：日期、项目、金额、地点（如果可以提取）。

            账单文本：
            $billText
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: billText // 如果 Gemini 无法格式化，则返回原始文本
        } catch (e: Exception) {
            billText // 如果出现错误，则返回原始文本
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

    // 从分析结果中提取情绪分数和评论
    private fun extractEmotionInfo(analysis: String): Pair<Int, String> {
        val scoreRegex = Pattern.compile("情绪分数：\\s*(\\d+)", Pattern.DOTALL or Pattern.MULTILINE)
        val commentRegex = Pattern.compile("情绪评论：\\s*(.+?)(?=\\n\\*|\\n\\n|\\n$)", Pattern.DOTALL or Pattern.MULTILINE)

        val scoreMatcher = scoreRegex.matcher(analysis)
        val commentMatcher = commentRegex.matcher(analysis)

        val score = if (scoreMatcher.find()) {
            scoreMatcher.group(1)?.toIntOrNull() ?: 0
        } else {
            80
        }

        val comment = if (commentMatcher.find()) {
            commentMatcher.group(1) ?: ""
        } else {
            ""
        }

        // 添加日志输出以便调试
        println("Extracted Score: $score")
        println("Extracted Comment: $comment")

        return Pair(score, comment)
    }



    private fun createInitialAnalysisPrompt(billText: String): String {
        return """
            以下是我最近一个月的账单信息：

            $billText

            请帮我分析一下我最近一个月的消费情况和情绪：

            1. 我的整体消费水平如何？
            2. 我在哪些方面的消费占比最高？
            3. 根据我的消费情况，推断我最近可能的情绪状态，例如我是否感到焦虑、压力大，或者消费过度？

            请在分析结果中明确指出情绪分数（0-100分）和情绪评论。
        """.trimIndent()
    }

    private fun createDetailedAnalysisPrompt(billText: String): String {
        return """
            以下是我最近一个月的账单信息：

            $billText

            请帮我分析一下我最近一个月的消费情况和情绪：

            1. 我的整体消费水平如何？
            2. 我在哪些方面的消费占比最高？
            3. 根据我的消费情况，推断我最近可能的情绪状态，例如我是否感到焦虑、压力大，或者消费过度？
            4. 根据我的消费情况和情绪分析，请给我一些具体的建议，帮助我更好地管理我的财务，例如如何减少不必要的开支，如何制定合理的预算，如何进行储蓄和投资？
            5. 请帮我制定一个未来一个月的消费规划，根据我的目标和目前的消费状态，制定一个合理且具有挑战性的消费计划来实现最近的那个目标（从goal字段获取，尤其关注title和additionalNotes，一般能够得到准确的目标），并计算距离我实现目标还需要多久，能否在预期时间内完成目标。
        """.trimIndent()
    }
}
