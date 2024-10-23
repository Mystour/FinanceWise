package com.starry.greenstash.ui.screens.analysis

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.starry.greenstash.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.launch

class BillAnalyzerViewModel : ViewModel() {
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.apiKey
        )
    }

    private var _billText by mutableStateOf("")
    private var _analysisResult by mutableStateOf("")
    private var _isLoading by mutableStateOf(false)

    val billText: String get() = _billText
    val analysisResult: String get() = _analysisResult
    val isLoading: Boolean get() = _isLoading

    fun setBillText(text: String) {
        _billText = text
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
            analyzeBillWithGemini(formattedBill) { result ->
                _analysisResult = result
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
        callback: (String) -> Unit
    ) {
        try {
            val prompt = createAnalysisPrompt(billText) // 使用格式化后的账单文本创建提示词
            val response = generativeModel.generateContent(prompt)
            val analysis = response.text ?: "无法分析账单"
            callback(analysis)
        } catch (e: Exception) {
            callback("分析出错: ${e.message}")
        }
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
