package com.starry.greenstash.ui.screens.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.starry.greenstash.BuildConfig
import kotlinx.coroutines.launch

class BillAnalyzerViewModel : ViewModel() {
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.apiKey
        )
    }

    private var _billText = ""
    val billText: String
        get() = _billText

    private var _analysisResult = ""
    val analysisResult: String
        get() = _analysisResult

    private var _emotionScore = 0
    val emotionScore: Int
        get() = _emotionScore

    private var _emotionComment = ""
    val emotionComment: String
        get() = _emotionComment

    private var _isLoading = false
    val isLoading: Boolean
        get() = _isLoading

    fun setBillText(text: String) {
        _billText = text
    }

    fun setAnalysisResult(result: String) {
        _analysisResult = result
    }

    fun setEmotionScore(score: Int) {
        _emotionScore = score
    }

    fun setEmotionComment(comment: String) {
        _emotionComment = comment
    }

    fun setIsLoading(isLoading: Boolean) {
        _isLoading = isLoading
    }

    fun initializeBillText(goals: String?) {
        setBillText(goals ?: "")
    }

    fun analyzeBill() {
        viewModelScope.launch {
            _isLoading = true
            val formattedBill = if (isJson(_billText)) {
                _billText // 如果是 JSON 格式，不进行格式化
            } else {
                formatBillText(_billText)
            }
            analyzeBillWithGemini(formattedBill) { result, score, comment -> // 修改回调函数
                _analysisResult = result
                _emotionScore = score // 更新 emotionScore
                _emotionComment = comment // 更新 emotionComment
                _isLoading = false
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

    private suspend fun analyzeBillWithGemini(
        billText: String,
        callback: (String, Int, String) -> Unit // 修改回调函数，添加情绪分数和评论
    ) {
        try {
            val prompt = createAnalysisPrompt(billText)
            val response = generativeModel.generateContentStream(prompt)
            var analysis = ""
            response.collect { chunk ->
                analysis += chunk.text
                // 从 analysis 中提取情绪分数和评论（可以使用正则表达式或其他方法）
                val (score, comment) = extractEmotionInfo()
                _emotionScore = score
                _emotionComment = comment
                callback(analysis, score, comment)
            }
        } catch (e: Exception) {
            callback("分析出错: ${e.message}", 0, "")
        }
    }

    // 从分析结果中提取情绪分数和评论
    private fun extractEmotionInfo(): Pair<Int, String> {
        // TODO: 使用正则表达式或其他方法从 analysis 中提取情绪分数和评论
        // 例如：
        // val scoreRegex = Regex("情绪分数：(\\d+)")
        // val commentRegex = Regex("情绪评论：(.+)")
        // val score = scoreRegex.find(analysis)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        // val comment = commentRegex.find(analysis)?.groupValues?.getOrNull(1) ?: ""
        // return Pair(score, comment)

        // 这里暂时返回默认值
        return Pair(80, "您最近的消费情绪比较积极，继续保持良好的消费习惯！")
    }

    private fun createAnalysisPrompt(billText: String): String {
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
