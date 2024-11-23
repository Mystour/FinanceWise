package com.starry.greenstash.ui.screens.emotion.composables

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.starry.greenstash.R

@Composable
fun EmotionChart(
    emotionScore: Int,
    modifier: Modifier = Modifier,
) {
    val entries = listOf(
        PieEntry(emotionScore.toFloat(), stringResource(id = R.string.emotion_score)),
        PieEntry((100 - emotionScore).toFloat(), "") // 补充剩余部分
    )
    val colors = listOf(
        Color(0xFFB39DDB), // 淡紫色
        Color(0xFFE0E0E0),  // 浅灰色
    )
    val isPreview = LocalInspectionMode.current
    val color = MaterialTheme.colorScheme.onSecondaryContainer
    val scoreColor = MaterialTheme.colorScheme.onSecondaryContainer.toArgb()
    val labelColor = MaterialTheme.colorScheme.onSecondaryContainer.toArgb()

    println("scoreColor: $color")

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                createPieChart(context, entries, colors.map { it.toArgb() }, scoreColor, labelColor, isPreview)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp) // 增加 padding 以增大图表尺寸
        )
    }
}


private fun createPieChart(
    context: Context,
    entries: List<PieEntry>,
    colors: List<Int>,
    scoreColor: Int,
    labelColor: Int,
    isPreview: Boolean
): PieChart {
    val chart = PieChart(context)
    val tf = Typeface.DEFAULT // 使用系统默认字体

    val dataSet = PieDataSet(entries, "")
    dataSet.colors = colors
    dataSet.setDrawValues(true)
    dataSet.valueTextSize = 16f
    dataSet.valueTextColor = scoreColor // 设置分数颜色
    dataSet.valueTypeface = tf
    dataSet.sliceSpace = 3f
    dataSet.selectionShift = 5f

    // 设置标签在外侧显示
    dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
    dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
    dataSet.valueLinePart1Length = 0.6f
    dataSet.valueLinePart2Length = 0.4f
    dataSet.valueLineColor = labelColor  // 设置标签线颜色
    dataSet.valueLinePart1OffsetPercentage = 30f

    val pieData = PieData(dataSet)
    pieData.setValueFormatter(PercentFormatter(chart))
    pieData.setValueTextSize(16f)
    pieData.setValueTextColor(labelColor )
    pieData.setValueTypeface(tf) // 使用系统默认字体

    chart.isDrawHoleEnabled = false // 不绘制中心空洞
    chart.setDrawCenterText(false)
    chart.setDrawEntryLabels(true)
    chart.description.isEnabled = false
    chart.legend.isEnabled = true
    chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
    chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
    chart.legend.orientation = Legend.LegendOrientation.VERTICAL
    chart.legend.setDrawInside(false)
    chart.legend.textSize = 14f
    chart.legend.typeface = tf
    chart.legend.textColor = labelColor
    chart.setExtraOffsets(20f, 8f, 75f, 8f)
    chart.setBackgroundColor(Color.Transparent.toArgb()) // 设置背景为透明
    chart.dragDecelerationFrictionCoef = 0.75f

    // 检查是否处于预览模式
    if (!isPreview) {
        // 添加动画效果
        chart.animateY(1400, Easing.EaseInOutQuad)
    }

    chart.data = pieData
    chart.invalidate()

    // 添加调试信息
    println("Entries: $entries")
    println("Colors: $colors")

    return chart
}


@Preview(showBackground = true, apiLevel = 34) // 去掉背景
@Composable
fun EmotionChartPreview() {
    EmotionChart(emotionScore = 75)
}
