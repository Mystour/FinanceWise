package com.starry.greenstash.ui.screens.analysis

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.starry.greenstash.ui.components.BillInput
import com.starry.greenstash.ui.components.TopAppBar
import com.google.ai.client.generativeai.GenerativeModel
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch
import com.starry.greenstash.BuildConfig
import com.google.gson.Gson

//import com.ai.financeWise.data.AppDatabase
//import com.ai.financeWise.data.Bill

@Composable
//fun BillAnalyzerScreen(appDatabase: AppDatabase, goals: String?) {
fun BillAnalyzerScreen(goals: String?) {
    val context = LocalContext.current // 获取 Context
    val coroutineScope = rememberCoroutineScope()
    var billText by remember { mutableStateOf(goals ?: "") } // 使用 goals 作为默认值

    println("billText: $billText")

    var analysisResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.apiKey
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = "账单情绪分析") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BillInput(
                billText = billText,
                onBillTextChange = { billText = it }
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        val formattedBill = if (isJson(billText)) {
                            billText // 如果是 JSON 格式，不进行格式化
                        } else {
                            formatBillText(generativeModel, billText)
                        }
                        analyzeBillWithGemini(generativeModel, formattedBill) { result ->
                            analysisResult = result
                            isLoading = false
                        }
                        analyzeBillWithGemini(generativeModel, formattedBill) { result ->
                            analysisResult = result
                            isLoading = false
                        }
                    }
                },
                enabled = billText.isNotBlank()
            ) {
                Text("分析账单")
            }

            if (isLoading) {
                CircularProgressIndicator()
            }

            // 使用 Markwon 渲染 Markdown 文本
            val markwon = Markwon.create(context)
            val spanned = markwon.toMarkdown(analysisResult)

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

suspend fun formatBillText(
    generativeModel: GenerativeModel, // 传入 generativeModel 参数
    billText: String
): String {
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

fun isJson(text: String): Boolean {
    return try {
        Gson().fromJson(text, Any::class.java)
        true
    } catch (e: Exception) {
        false
    }
}

suspend fun analyzeBillWithGemini(
    generativeModel: GenerativeModel,
    billText: String, // 接收格式化后的账单文本
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


// 创建分析提示词
fun createAnalysisPrompt(billText: String): String {
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