package com.starry.greenstash.ui.screens.emotion.composables

import android.text.method.LinkMovementMethod
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.toSpanned
import androidx.lifecycle.viewmodel.compose.viewModel
import com.starry.greenstash.R
import com.starry.greenstash.ui.screens.emotion.EmotionViewModel
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillAnalyzerScreen(goals: String?, viewModel: EmotionViewModel = viewModel(factory = EmotionViewModelFactory(LocalContext.current))) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(goals) {
        scope.launch(Dispatchers.IO) {
            viewModel.initializeBillText(goals)
            withContext(Dispatchers.Main) {
                println("billText: ${viewModel.billText}")
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = stringResource(id = R.string.emotion_analysis_title)) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
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

                // 显示描述
                val description = stringResource(id = R.string.emotion_analysis_desc)
                val annotatedDescription = buildAnnotatedString {
                    append(description)
                    // 可以在这里添加更多的样式，例如加粗某些部分
                    // pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    // append("重要部分")
                    // pop()
                }

                Text(
                    text = annotatedDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.padding(16.dp)
                )

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

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun BillAnalyzerScreenPreview() {
    val context = LocalContext.current

    // 创建一个模拟的 EmotionViewModel
    val previewViewModel = EmotionViewModel(context).apply {
        // 使用公共方法设置状态
        setBillText("示例账单文本")
        setAnalysisResult("## 分析结果\n**这是加粗的文本**\n这是普通文本")
        setEmotionScore(80)
        setEmotionComment("：这是一个情绪评论")
        setIsLoading(false)
    }

    // 使用预览提供的 ViewModel
    BillAnalyzerScreen(goals = "示例账单文本", viewModel = previewViewModel)
}
