package com.starry.greenstash.ui.screens.emotion.composables

import android.text.method.LinkMovementMethod
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.toSpanned
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.maxkeppeler.sheets.calendar.models.CalendarStyle
import com.starry.greenstash.R
import com.starry.greenstash.database.goal.GoalPriority
import com.starry.greenstash.ui.navigation.NormalScreens
import com.starry.greenstash.ui.screens.emotion.EmotionViewModel
import com.starry.greenstash.utils.weakHapticFeedback
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionScreen(
    viewModel: EmotionViewModel = hiltViewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    remember { SnackbarHostState() }
    val goalsState = viewModel.goals.collectAsState(initial = emptyList())
    remember { mutableStateOf("") }
    LocalSoftwareKeyboardController.current
    val localView = LocalView.current
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Date variables and state
    val selectedStartDate = remember { mutableStateOf<LocalDate?>(null) }
    val selectedEndDate = remember { mutableStateOf<LocalDate?>(null) }
    val startDateCalendarState = rememberUseCaseState(visible = false, true)
    val endDateCalendarState = rememberUseCaseState(visible = false, true)


    LaunchedEffect(Unit) {
        viewModel.loadGoals()
    }

    val showTitle = viewModel.selectedFilterType != EmotionViewModel.FilterType.Title

    Scaffold(
        topBar = {
            EmotionTopAppBar(
                title = stringResource(id = R.string.emotion_analysis_title),
                showTitle = showTitle,
                searchText = viewModel.titleFilter,
                onSearchInputChange = { viewModel.setTitleFilter(it) },
                onSearchAction = { viewModel.setTitleFilter(viewModel.titleFilter) }, // Correct search action
                filterType = viewModel.selectedFilterType,
                onFilterTypeChange = { viewModel.setSelectedFilterType(it) },
                onRefreshClick = { viewModel.reset() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState), // 使整个 Column 可滚动
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            when (viewModel.selectedFilterType) {
                EmotionViewModel.FilterType.Title -> {
                }


                EmotionViewModel.FilterType.DateRange -> {

                    // Start Date Input
                    OutlinedTextField(
                        value = selectedStartDate.value?.format(DateTimeFormatter.ofPattern(viewModel.getDateStyleValue())) ?: "", // Display formatted date or empty string
                        onValueChange = {  }, // Make it read-only
                        label = { Text(stringResource(id = R.string.start_date)) },
                        trailingIcon = {
                            IconButton(onClick = { startDateCalendarState.show() }) {
                                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false, // Make the field read-only to prevent keyboard input
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.onBackground,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f), // Adjust as needed
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                    )


                    // End Date Input (similar to Start Date)
                    OutlinedTextField(
                        value = selectedEndDate.value?.format(DateTimeFormatter.ofPattern(viewModel.getDateStyleValue())) ?: "", // Display formatted date or empty string
                        onValueChange = {  },
                        label = { Text(stringResource(id = R.string.end_date)) },
                        trailingIcon = {
                            IconButton(onClick = { endDateCalendarState.show() }) {
                                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors( // ... same colors as above
                        )
                    )

                    // Start Date Calendar Dialog
                    CalendarDialog(
                        state = startDateCalendarState,
                        config = CalendarConfig(
                            yearSelection = true,
                            monthSelection = true,
                            style = CalendarStyle.MONTH
                        ),
                        selection = CalendarSelection.Date { newDate ->
                            selectedStartDate.value = newDate
                            viewModel.setStartDate(newDate.format(DateTimeFormatter.ofPattern(viewModel.getDateStyleValue())))
                        }
                    )

                    // End Date Calendar Dialog (similar to Start Date)
                    CalendarDialog(
                        state = endDateCalendarState,
                        config = CalendarConfig(
                            yearSelection = true,
                            monthSelection = true,
                            style = CalendarStyle.MONTH
                        ),
                        selection = CalendarSelection.Date { newDate ->
                            selectedEndDate.value = newDate
                            viewModel.setEndDate(newDate.format(DateTimeFormatter.ofPattern(viewModel.getDateStyleValue())))
                        }
                    )
                }


                EmotionViewModel.FilterType.Priority -> {
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GoalPriority.entries.forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority.name) },
                                onClick = {
                                    viewModel.setSelectedPriority(priority)
                                    isDropdownExpanded = false // 关闭下拉菜单
                                }
                            )
                        }
                    }

                    // 添加一个按钮或其他方式来触发下拉菜单的展开
                    Button(onClick = { isDropdownExpanded = true }) {
                        Text(context.getString(R.string.select_priority_button))
                    }
                }

                EmotionViewModel.FilterType.None -> {
                    // 不显示任何筛选控件
                }
            }

            BillInput(
                billText = viewModel.billText,
                onBillTextChange = { viewModel.setBillText(it) }
            )

            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        viewModel.analyzeBill()
                    }
                },
                enabled = viewModel.billText.isNotBlank()
            ) {
                Text(stringResource(id = R.string.emotion_analysis_button))
            }

            if (viewModel.isLoading) {
                CircularProgressIndicator()
            } else {
                // 分析结束后才显示图表和评论
                if (viewModel.analysisResult.isNotBlank()) { // 检查 analysisResult 是否为空
                    val emotionScore = viewModel.emotionScore
                    EmotionChart(
                        emotionScore = emotionScore,
                        modifier = Modifier
                            .height(300.dp) // 设置高度为 300dp
                            .fillMaxWidth() // 占据整个宽度
                    )

                    // 使用 Card 组件显示评论
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.score_text, emotionScore, viewModel.emotionComment),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // 使用 Markwon 处理 Markdown 文本
                val markwon = Markwon.create(context)
                val spanned = remember(viewModel.analysisResult) {
                    markwon.toMarkdown(viewModel.analysisResult)
                }

                // 使用 buildAnnotatedString 添加自定义样式
                val annotatedString = remember(spanned) {
                    buildAnnotatedString {
                        val markdownText = spanned.toString()
                        var currentIndex = 0

                        while (currentIndex < markdownText.length) {
                            val titleStart = markdownText.indexOf("## ", currentIndex)
                            if (titleStart == -1) {
                                append(markdownText.substring(currentIndex))
                                break
                            }

                            append(markdownText.substring(currentIndex, titleStart))
                            val titleEnd = markdownText.indexOf('\n', titleStart)
                            if (titleEnd == -1) {
                                append(markdownText.substring(titleStart))
                                break
                            }

                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                            append(markdownText.substring(titleStart + 3, titleEnd))
                            pop()
                            currentIndex = titleEnd + 1
                        }
                    }
                }

                // 使用 AndroidView 组件显示 AnnotatedString 对象
                AndroidView(
                    factory = { context ->
                        androidx.appcompat.widget.AppCompatTextView(context).apply {
                            movementMethod = LinkMovementMethod.getInstance()
                        }
                    },
                    update = { textView ->
                        textView.text = annotatedString.toSpanned()
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 显示描述
            val description = stringResource(id = R.string.emotion_analysis_desc)
            val annotatedDescription = buildAnnotatedString {
                append(description)
                // 可以在这里添加更多的样式，例如加粗某些部分
                // pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                // append("重要部分")
                // pop()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = annotatedDescription,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color.Gray
                    ),
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 显示过滤后的目标列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp) // 限制 LazyColumn 的最大高度
            ) {
                items(goalsState.value) { goal ->
                    val gson = Gson()
                    val isAdded = viewModel.billText.contains(gson.toJson(goal))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = goal.goal.title, // 只显示目标的 title
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        // 信息按钮
                        IconButton(
                            onClick = {
                                localView.weakHapticFeedback()
                                navController.navigate(
                                    NormalScreens.GoalInfoScreen(goalId = goal.goal.goalId.toString())
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(id = R.string.info_button)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isAdded) {
                                    viewModel.removeGoalFromAnalysis(goal)
                                } else {
                                    viewModel.addGoalToAnalysis(goal)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isAdded) Icons.Filled.Remove else Icons.Default.Add,
                                contentDescription = if (isAdded) stringResource(id = R.string.remove_from_analysis) else stringResource(id = R.string.add_to_analysis)
                            )
                        }
                    }
                }
            }

            // 监听分析结果的变化并自动滚动
            LaunchedEffect(viewModel.analysisResult) {
                if (viewModel.analysisResult.isNotBlank()) {
                    scope.launch {
                        delay(300) // 延迟一段时间，确保内容已经渲染完成
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            }
        }
    }
}
