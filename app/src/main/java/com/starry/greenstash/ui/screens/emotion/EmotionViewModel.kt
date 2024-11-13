package com.starry.greenstash.ui.screens.emotion

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.starry.greenstash.BuildConfig
import com.starry.greenstash.R
import com.starry.greenstash.database.core.GoalWithTransactions
import com.starry.greenstash.database.goal.GoalDao // 导入正确的 DAO
import com.starry.greenstash.database.goal.GoalPriority
import com.starry.greenstash.utils.PreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.regex.Pattern

@HiltViewModel
class EmotionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val goalDao: GoalDao,
    private val preferenceUtil: PreferenceUtil
) : ViewModel() {
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

    private val _goals = MutableStateFlow<List<GoalWithTransactions>>(emptyList())
    val goals: StateFlow<List<GoalWithTransactions>>
        get() = _goals

    init {
        loadGoals()
    }

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

    // 新增筛选条件的状态变量
    private val _startDate = mutableStateOf("")
    val startDate: String
        get() = _startDate.value

    private val _endDate = mutableStateOf("")
    val endDate: String
        get() = _endDate.value

    private val _selectedPriority = mutableStateOf(GoalPriority.Normal)
    val selectedPriority: GoalPriority
        get() = _selectedPriority.value

    // 设置筛选条件的方法
    fun setStartDate(date: String) {
        _startDate.value = date
    }

    fun setEndDate(date: String) {
        _endDate.value = date
    }

    fun setSelectedPriority(priority: GoalPriority) {
        _selectedPriority.value = priority
    }

    // 新增筛选标准的状态变量
    private val _selectedFilterType = mutableStateOf(FilterType.None)
    val selectedFilterType: FilterType
        get() = _selectedFilterType.value

    // 设置筛选标准的方法
    fun setSelectedFilterType(filterType: FilterType) {
        _selectedFilterType.value = filterType
    }

    // 筛选标准枚举
    enum class FilterType {
        Title, DateRange, Priority, None;
    }

    fun loadGoals() {
        viewModelScope.launch {
            val goalsList = goalDao.getAllGoals() // 使用 GoalDao 的方法
            _goals.value = goalsList
        }
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
            val initialComment = commentMatcher.group(1)?.replace("*", "") ?: ""
            if (initialComment.trim().endsWith(":") || initialComment.trim().endsWith("：")) {
                // 如果评论只包含冒号，继续往下取到第一个句号结束
                val extendedComment = analysis.substring(initialComment.length).takeWhile { it != '.' }.trim()
                initialComment + extendedComment
            } else {
                initialComment
            }
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

    fun getDefaultCurrency(): String {
        return preferenceUtil.getString(PreferenceUtil.DEFAULT_CURRENCY_STR, "")!!
    }

    fun getDateFormatPattern(): String {
        return preferenceUtil.getString(PreferenceUtil.DATE_FORMAT_STR, "")!!
    }

    // 更新 filterGoals 方法
    fun filterGoals(query: String, startDate: String = "", endDate: String = "", priority: GoalPriority = GoalPriority.Normal) {
        // 如果查询为空字符串，则返回所有目标
        if (query.isBlank() && startDate.isEmpty() && endDate.isEmpty() && priority == GoalPriority.Normal) {
            viewModelScope.launch {
                val goalsList = goalDao.getAllGoals() // 使用 GoalDao 的方法
                _goals.value = goalsList
            }
        } else {
            // 根据查询、日期范围和优先级过滤目标
            val filteredGoals = _goals.value.filter { goal ->
                val titleMatch = goal.goal.title.contains(query, ignoreCase = true)
                val dateMatch = when {
                    startDate.isNotEmpty() && endDate.isNotEmpty() -> goal.goal.deadline >= startDate && goal.goal.deadline <= endDate
                    startDate.isNotEmpty() -> goal.goal.deadline >= startDate
                    endDate.isNotEmpty() -> goal.goal.deadline <= endDate
                    else -> true
                }
                val priorityMatch = if (priority == GoalPriority.Normal) true else goal.goal.priority == priority

                titleMatch && dateMatch && priorityMatch
            }
            _goals.value = filteredGoals
        }
    }

    fun addGoalToAnalysis(goal: GoalWithTransactions) {
        // 将目标转换为 JSON 格式
        val gson = Gson()
        val goalJson = gson.toJson(goal)

        _billText.value = "${_billText.value}\n$goalJson"
    }

    fun removeGoalFromAnalysis(goal: GoalWithTransactions) {
        // 将目标转换为 JSON 格式
        val gson = Gson()
        val goalJson = gson.toJson(goal)

        // 从 _billText 中移除目标
        _billText.value = _billText.value.replace(goalJson, "")
    }

    fun reset() {
        // 重置情绪分析结果
        _billText.value = ""
        _analysisResult.value = ""
        _emotionScore.intValue = 0
        _emotionComment.value = ""

        // 重置加载状态
        _isLoading.value = false

        // 重新加载所有目标
        loadGoals()
    }
}
