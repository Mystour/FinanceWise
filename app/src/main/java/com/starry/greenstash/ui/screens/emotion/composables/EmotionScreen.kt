package com.starry.greenstash.ui.screens.emotion.composables

import android.content.Context
import android.text.method.LinkMovementMethod
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.toSpanned
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionResult
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.gson.Gson
import com.starry.greenstash.R
import com.starry.greenstash.database.core.GoalWithTransactions
import com.starry.greenstash.ui.screens.emotion.EmotionViewModel
import com.starry.greenstash.ui.theme.greenstashFont
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionScreen(
   viewModel: EmotionViewModel = hiltViewModel(),
   navController: NavController
) {
   val context = LocalContext.current
   val scope = rememberCoroutineScope()
   val scrollState = rememberScrollState()
   val snackBarHostState = remember { SnackbarHostState() }
   val goalsState = viewModel.goals.collectAsState(initial = emptyList())
   val searchQuery = remember { mutableStateOf("") }
   val keyboardController = LocalSoftwareKeyboardController.current

   LaunchedEffect(Unit) {
       viewModel.loadGoals()
   }

   // 监听 searchQuery 的变化，自动调用 filterGoals
   LaunchedEffect(searchQuery.value) {
       viewModel.filterGoals(searchQuery.value)
   }

   Scaffold(
       topBar = {
           TopAppBar(
               title = {
                   Text(text = stringResource(id = R.string.emotion_analysis_title))
               },
               actions = {
                   TextField(
                       value = searchQuery.value,
                       onValueChange = { query ->
                           searchQuery.value = query
                       },
                       placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                       modifier = Modifier
                           .fillMaxWidth(0.5f)
                           .padding(horizontal = 8.dp),
                       keyboardOptions = KeyboardOptions.Default.copy(
                           imeAction = ImeAction.Done
                       ),
                       keyboardActions = KeyboardActions(
                           onDone = {
                               viewModel.filterGoals(searchQuery.value)
                               keyboardController?.hide() // 隐藏键盘
                           }
                       )
                   )

                   // 添加重置筛选条件的按钮
                   IconButton(
                       onClick = {
                           searchQuery.value = ""
                           viewModel.reset()
                       }
                   ) {
                       Icon(
                           imageVector = Icons.Default.Refresh,
                           contentDescription = stringResource(id = R.string.reset_filter)
                       )
                   }
               }
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

                   // 去除评论中的米字符
                   val cleanedComment = viewModel.emotionComment.replace("米", "")

                   // 使用 Card 组件显示评论
                   Card(
                       modifier = Modifier
                           .fillMaxWidth()
                           .padding(vertical = 8.dp),
                       shape = MaterialTheme.shapes.medium,
                       elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                   ) {
                       Text(
                           text = stringResource(id = R.string.score_text, emotionScore, cleanedComment),
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
