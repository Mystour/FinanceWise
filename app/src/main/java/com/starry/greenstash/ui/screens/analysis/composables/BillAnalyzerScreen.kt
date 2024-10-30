package com.starry.greenstash.ui.screens.analysis.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.starry.greenstash.ui.screens.analysis.BillAnalyzerViewModel
import io.noties.markwon.Markwon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillAnalyzerScreen(goals: String?) {
    val viewModel: BillAnalyzerViewModel = viewModel()
    val context = LocalContext.current // 获取 Context
    rememberCoroutineScope()

    LaunchedEffect(goals) {
        viewModel.initializeBillText(goals)
        println("billText: ${viewModel.billText}")
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("账单情绪分析") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            BillInput(
                billText = viewModel.billText,
                onBillTextChange = { viewModel.setBillText(it) }
            )

            Button(
                onClick = {
                    viewModel.analyzeBill()
                },
                enabled = viewModel.billText.isNotBlank()
            ) {
                Text("分析账单")
            }

            if (viewModel.isLoading) {
                CircularProgressIndicator()
            } else {
                // 分析结束后才显示图表和评论
                if (viewModel.analysisResult.isNotBlank()) { // 检查 analysisResult 是否为空
                    EmotionChart(emotionScore = viewModel.emotionScore)
                    Text(text = viewModel.emotionComment)
                }

                // 使用 Markwon 渲染 Markdown 文本
                val markwon = Markwon.create(context)
                val spanned = markwon.toMarkdown(viewModel.analysisResult)

                // 使用 AnnotatedString 显示 Markdown 内容，并自定义样式
                val annotatedString = buildAnnotatedString {
                    append(spanned)
                    // 可以在这里添加更多自定义样式，例如标题加粗等
                }

                Text(
                    text = annotatedString,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
