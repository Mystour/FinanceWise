package com.starry.greenstash.ui.screens.emotion

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.starry.greenstash.BuildConfig
import com.starry.greenstash.R
import com.starry.greenstash.database.core.GoalWithTransactions
import com.starry.greenstash.database.goal.GoalDao
import com.starry.greenstash.database.goal.GoalPriority
import com.starry.greenstash.ui.screens.settings.DateStyle
import com.starry.greenstash.utils.PreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmotionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val goalDao: GoalDao,
    private val preferenceUtil: PreferenceUtil,
) : ViewModel() {

    private  var _apiKey = MutableStateFlow("")

    // 使用 backing property 安全地声明 _goals
    private val _goals = MutableStateFlow(emptyList<GoalWithTransactions>())
    // 确保 goals 是 StateFlow
    val goals: StateFlow<List<GoalWithTransactions>> = _goals


    init {
        _apiKey.value = preferenceUtil.getString(PreferenceUtil.API_KEY, "") ?: ""
        println("API Key: ${_apiKey.value}")
        loadGoals()
    }


    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = _apiKey.value, // 使用 Elvis 操作符提供默认值
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
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

    // 将筛选条件存储在一个列表中
    private val appliedFilters = mutableStateListOf<FilterType>()

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
        if (date.isNotBlank()) {
            appliedFilters.remove(FilterType.DateRange) // 移除已有的日期范围筛选器
            appliedFilters.add(FilterType.DateRange) // 添加日期范围筛选器
        } else {
            appliedFilters.remove(FilterType.DateRange) // 移除日期范围筛选器
        }
        updateFilterCriteria()
    }

    fun setEndDate(date: String) {
        _endDate.value = date
        if (date.isNotBlank()) {
            appliedFilters.remove(FilterType.DateRange) // 移除已有的日期范围筛选器
            appliedFilters.add(FilterType.DateRange) // 添加日期范围筛选器
        } else {
            appliedFilters.remove(FilterType.DateRange) // 移除日期范围筛选器
        }
        updateFilterCriteria()
    }


    fun setSelectedPriority(priority: GoalPriority) {
        _selectedPriority.value = priority
        appliedFilters.remove(FilterType.Priority) // 移除已有的优先级筛选器
        appliedFilters.add(FilterType.Priority) // 添加优先级筛选器
        updateFilterCriteria()
    }



    fun setTitleFilter(query: String) {
        _titleFilter.value = query
        if (query.isNotBlank()) {
            appliedFilters.remove(FilterType.Title) // 移除已有的标题筛选器
            appliedFilters.add(FilterType.Title) // 添加标题筛选器
        } else {
            appliedFilters.remove(FilterType.Title) // 移除标题筛选器
        }
        updateFilterCriteria()
    }


    private fun updateFilterCriteria() {
        viewModelScope.launch {
            val allGoals = goalDao.getAllGoalsAsLiveData().asFlow().first()

            val filteredLists = mutableListOf<List<GoalWithTransactions>>()

            if (appliedFilters.contains(FilterType.Title) && _titleFilter.value.isNotBlank()) {
                filteredLists.add(goalDao.getGoalsByTitleContains(_titleFilter.value).first())
            }
            if (appliedFilters.contains(FilterType.DateRange) && (_startDate.value.isNotBlank() || _endDate.value.isNotBlank())) {
                filteredLists.add(goalDao.getAllGoalsByDateRange(_startDate.value, _endDate.value).first())
            }
            if (appliedFilters.contains(FilterType.Priority)) {
                filteredLists.add(goalDao.getAllGoalsByPriority(_selectedPriority.value).first())
            }

            // 计算交集
            val filteredList = if (appliedFilters.isEmpty()) {
                allGoals
            } else if (filteredLists.isEmpty()) {
                emptyList()
            } else {
                filteredLists.reduce { acc, list ->
                    acc.intersect(list.toSet()).toList() // 取交集
                }
            }

            println("Filtered List: $filteredList")

            // 更新 goals 的状态
            _goals.value = filteredList
            _filterCriteria.value = FilterCriteria(
                query = _titleFilter.value,
                startDate = _startDate.value,
                endDate = _endDate.value,
                selectedPriority = _selectedPriority.value,
                filterType = _selectedFilterType.value
            )
        }
    }



    fun setSelectedFilterType(filterType: FilterType) {
        if (_selectedFilterType.value != filterType) {
            // 如果当前筛选类型与新筛选类型不同，则更新筛选类型
            _selectedFilterType.value = filterType

            // 根据新的筛选类型更新 appliedFilters
            when (filterType) {
                FilterType.Title -> {
                    if (_titleFilter.value.isNotBlank()) {
                        appliedFilters.add(FilterType.Title)
                    }
                }
                FilterType.DateRange -> {
                    if (_startDate.value.isNotBlank() || _endDate.value.isNotBlank()) {
                        appliedFilters.add(FilterType.DateRange)
                    }
                }
                FilterType.Priority -> {
                    if (_selectedPriority.value != GoalPriority.Normal) {
                        appliedFilters.add(FilterType.Priority)
                    }
                }
                FilterType.None -> {
                    appliedFilters.clear()
                }
            }

            // 更新 filterCriteria
            updateFilterCriteria()
        }
    }




    // 筛选标准枚举
    enum class FilterType {
        Title, DateRange, Priority, None;
    }


    fun loadGoals() {
        viewModelScope.launch {
            // 使用 getOrNull() 确保即使数据库返回 null 也不会崩溃
            val initialGoals = goalDao.getAllGoalsAsLiveData().asFlow().firstOrNull() ?: emptyList()
            _goals.value = initialGoals
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

    private fun createInitialAnalysisPrompt(billText: String): String {
        val initialPrompt = context.getString(R.string.initial_analysis_prompt)
        return String.format(initialPrompt, billText)
    }

    private fun createDetailedAnalysisPrompt(billText: String): String {
        val detailedPrompt = context.getString(R.string.detailed_analysis_prompt)
        return String.format(detailedPrompt, billText)
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
        _titleFilter.value = ""
        _startDate.value = ""
        _endDate.value = ""
        _selectedFilterType.value = FilterType.None
        appliedFilters.clear()
        updateFilterCriteria()
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