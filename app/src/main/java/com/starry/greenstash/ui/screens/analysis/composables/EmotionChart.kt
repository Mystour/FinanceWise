package com.starry.greenstash.ui.screens.analysis.composables

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

@Composable
fun EmotionChart(emotionScore: Int) {
    val entries = listOf(
        PieEntry(emotionScore.toFloat(), "情绪评分"),
        PieEntry((100 - emotionScore).toFloat(), "") // 补充剩余部分
    )
    val colors = listOf(Color(0xFF6200EE), Color.LightGray)

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                createPieChart(context, entries, colors.map { it.toArgb() })
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

private fun createPieChart(
    context: Context,
    entries: List<PieEntry>,
    colors: List<Int>
): PieChart {
    val chart = PieChart(context)
    val tf = Typeface.DEFAULT // 使用系统默认字体

    val dataSet = PieDataSet(entries, "")
    dataSet.colors = colors
    dataSet.setDrawValues(true)
    dataSet.valueTextSize = 14f
    dataSet.valueTextColor = Color.Red.toArgb()
    dataSet.valueTypeface = tf
    dataSet.sliceSpace = 0f
    dataSet.selectionShift = 5f

    val pieData = PieData(dataSet)
    pieData.setValueFormatter(PercentFormatter(chart))
    pieData.setValueTextSize(14f)
    pieData.setValueTextColor(Color.Red.toArgb())
    pieData.setValueTypeface(tf) // 使用系统默认字体

    chart.setDrawHoleEnabled(false) // 不绘制中心空洞
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
    chart.setExtraOffsets(20f, 8f, 75f, 8f)
    chart.setBackgroundColor(Color.Transparent.toArgb())
    chart.dragDecelerationFrictionCoef = 0.75f

    chart.data = pieData
    chart.invalidate()

    // 添加调试信息
    println("Entries: $entries")
    println("Colors: $colors")

    return chart
}

@Preview(showBackground = true, apiLevel = 34)
@Composable
fun EmotionChartPreview() {
    EmotionChart(emotionScore = 75)
}
