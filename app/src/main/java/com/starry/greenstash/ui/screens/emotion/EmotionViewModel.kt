package com.starry.greenstash.ui.screens.emotion

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.starry.greenstash.BuildConfig
import com.starry.greenstash.R
import com.starry.greenstash.database.core.GoalWithTransactions
import com.starry.greenstash.database.goal.GoalDao // 导入正确的 DAO
import com.starry.greenstash.database.goal.GoalPriority
import com.starry.greenstash.ui.screens.settings.DateStyle
import com.starry.greenstash.utils.PreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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

    // 新增筛选条件的状态变量
    private val _startDate = mutableStateOf("")

    private val _endDate = mutableStateOf("")

    private val _selectedPriority = mutableStateOf(GoalPriority.Normal)

    // 新增标题过滤的状态变量
    private val _titleFilter = mutableStateOf("")
    val titleFilter: String
        get() = _titleFilter.value

    // 新增筛选标准的状态变量
    private val _selectedFilterType = mutableStateOf(FilterType.None)
    val selectedFilterType: FilterType
        get() = _selectedFilterType.value

    // 筛选条件的组合
    private val _filterCriteria = MutableStateFlow(FilterCriteria("", "", "", GoalPriority.Normal, FilterType.None))

    // 动态获取目标列表
    @OptIn(ExperimentalCoroutinesApi::class)
    private val goalsListFlow = _filterCriteria.flatMapLatest { criteria ->
        when (criteria.filterType) {
            FilterType.Title -> goalDao.getGoalsByTitleContains(criteria.query) // 使用新的方法
            FilterType.DateRange -> goalDao.getAllGoalsByDateRange(criteria.startDate, criteria.endDate)
            FilterType.Priority -> goalDao.getAllGoalsByPriority(criteria.selectedPriority)
            FilterType.None -> goalDao.getAllGoalsAsLiveData().asFlow()
        }
    }

    val goals: StateFlow<List<GoalWithTransactions>> = goalsListFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadGoals()
    }

    fun setBillText(text: String) {
        _billText.value = text
    }

    private fun setAnalysisResult(result: String) {
        _analysisResult.value = result
    }

    private fun setEmotionScore(score: Int) {
        _emotionScore.intValue = score
    }

    private fun setEmotionComment(comment: String) {
        _emotionComment.value = comment
    }

    private fun setIsLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun setStartDate(date: String) {
        _startDate.value = date
        updateFilterCriteria()
    }

    fun setEndDate(date: String) {
        _endDate.value = date
        updateFilterCriteria()
    }

    fun setSelectedPriority(priority: GoalPriority) {
        _selectedPriority.value = priority
        updateFilterCriteria()
    }

    fun setSelectedFilterType(filterType: FilterType) {
        _selectedFilterType.value = filterType
        updateFilterCriteria()
    }

    fun setTitleFilter(query: String) {
        _titleFilter.value = query
        updateFilterCriteria()
    }

    // 筛选标准枚举
    enum class FilterType {
        Title, DateRange, Priority, None;
    }


    fun loadGoals() {
        viewModelScope.launch {
            _filterCriteria.value = _filterCriteria.value.copy(query = "", startDate = "", endDate = "", selectedPriority = GoalPriority.Normal, filterType = FilterType.None)
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

    private fun extractEmotionInfo(analysisResult: String): Pair<Int, String> {
        val gson = Gson()
        try {
            val cleanedJson = analysisResult.replace(Regex("```json|```"), "").trim()
            val jsonObject = gson.fromJson(cleanedJson, JsonObject::class.java)

            val score = jsonObject.get("score")?.asInt ?: 0
            val comment = jsonObject.get("comment")?.asString ?: ""
            return Pair(score, comment)
        } catch (e: JsonSyntaxException) {
            // 处理 JSON 解析错误
            return Pair(0, "")
        }
    }

//    private fun extractEmotionInfo(analysis: String): Pair<Int, String> {
//        val scorePattern = context.getString(R.string.score_pattern)
//        val commentPattern = context.getString(R.string.comment_pattern)
//
//        val scoreRegex = Pattern.compile(scorePattern, Pattern.DOTALL or Pattern.MULTILINE)
//        val commentRegex = Pattern.compile(commentPattern, Pattern.DOTALL or Pattern.MULTILINE)
//
//        val scoreMatcher = scoreRegex.matcher(analysis)
//        val commentMatcher = commentRegex.matcher(analysis)
//
//        val score = if (scoreMatcher.find()) {
//            scoreMatcher.group(1)?.toIntOrNull() ?: 0
//        } else {
//            80 // 默认分数
//        }
//
//        val comment = if (commentMatcher.find()) {
//            val initialComment = commentMatcher.group(1)?.replace("*", "") ?: ""
//            if (initialComment.trim().endsWith(":") || initialComment.trim().endsWith("：")) {
//                // 如果评论只包含冒号，继续往下取到第一个句号结束
//                val extendedComment = analysis.substring(initialComment.length).takeWhile { it != '.' }.trim()
//                initialComment + extendedComment
//            } else {
//                initialComment
//            }
//        } else {
//            context.getString(R.string.default_emotion_comment)
//        }
//
//        println("Extracted Score: $score")
//        println("Extracted Comment: $comment")
//
//        return Pair(score, comment)
//    }

    private fun createInitialAnalysisPrompt(billText: String): String {
        val initialPrompt = context.getString(R.string.initial_analysis_prompt)
        return String.format(initialPrompt, billText)
    }

    private fun createDetailedAnalysisPrompt(billText: String): String {
        val detailedPrompt = context.getString(R.string.detailed_analysis_prompt)
        return String.format(detailedPrompt, billText)
    }

    // 更新 filterCriteria 方法
    private fun updateFilterCriteria() {
        _filterCriteria.value = FilterCriteria(
            query = _titleFilter.value,
            startDate = _startDate.value,
            endDate = _endDate.value,
            selectedPriority = _selectedPriority.value,
            filterType = _selectedFilterType.value
        )
    }

    fun addGoalToAnalysis(goal: GoalWithTransactions) {
        // 将目标转换为 JSON 格式
        val gson = Gson()
        val goalJson = gson.toJson(goal)

        _billText.value = "${_billText.value}$goalJson\n"
    }

    fun removeGoalFromAnalysis(goal: GoalWithTransactions) {
        // 将目标转换为 JSON 格式
        val gson = Gson()
        val goalJson = gson.toJson(goal)

        // 从 _billText 中移除目标
        _billText.value = _billText.value.replace("$goalJson\n", "")
    }

    fun reset() {
        _billText.value = ""
        _analysisResult.value = ""
        _emotionScore.intValue = 0
        _emotionComment.value = ""
        _isLoading.value = false
        _titleFilter.value = "" //  添加这行代码
        loadGoals()  // 这行代码也要保留，用于重新加载目标列表
    }

    // 筛选条件的数据类
    data class FilterCriteria(
        val query: String,
        val startDate: String,
        val endDate: String,
        val selectedPriority: GoalPriority,
        val filterType: FilterType
    )

    fun getDateStyleValue() = preferenceUtil.getString(
        PreferenceUtil.DATE_FORMAT_STR, DateStyle.DateMonthYear.pattern
    )
}
