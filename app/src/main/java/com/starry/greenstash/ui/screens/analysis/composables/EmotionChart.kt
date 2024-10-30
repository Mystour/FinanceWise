package com.starry.greenstash.ui.screens.analysis.composables

import android.content.Context
import android.graphics.Typeface
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

    AndroidView(
        factory = { context ->
            createRingPieChart(context, entries, colors.map { it.toArgb() }, emotionScore)
        },
        modifier = Modifier
            .height(200.dp)
            .padding(16.dp)
    )
}

private fun createRingPieChart(
   context: Context,
   entries: List<PieEntry>,
   colors: List<Int>,
   emotionScore: Int
): PieChart {
   val chart = PieChart(context)
   val tf = Typeface.createFromAsset(context.assets, "OpenSans-Regular.ttf")

   val dataSet = PieDataSet(entries, "")
   dataSet.colors = colors
   dataSet.setDrawValues(true)
   dataSet.valueTextSize = 14f
   dataSet.valueTextColor = Color.Red.toArgb()
   dataSet.valueTypeface = tf
   dataSet.valueLinePart1Length = 0.4f
   dataSet.valueLinePart2Length = 0.4f
   dataSet.valueLinePart1OffsetPercentage = 80f
   dataSet.valueLineColor = Color.Gray.toArgb()
   dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
   dataSet.sliceSpace = 0f
   dataSet.selectionShift = 5f

   val pieData = PieData(dataSet)
   pieData.setValueFormatter(PercentFormatter(chart))
   pieData.setValueTextSize(14f)
   pieData.setValueTextColor(Color.Red.toArgb())
   pieData.setValueTypeface(tf) // 修复这里

   chart.setDrawHoleEnabled(true)
   chart.holeRadius = 65f // 设置环的宽度
   chart.setTransparentCircleAlpha(0) // 完全透明
   chart.setDrawCenterText(true)
   chart.centerText = "情绪评分\n$emotionScore"
   chart.setCenterTextSize(18f)
   chart.setCenterTextTypeface(tf)
   chart.setDrawEntryLabels(false)
   chart.description.isEnabled = false
   chart.legend.isEnabled = false
   chart.setExtraOffsets(20f, 8f, 75f, 8f)
   chart.setBackgroundColor(Color.Transparent.toArgb())
   chart.dragDecelerationFrictionCoef = 0.75f

   // 设置图例
   val legend = chart.legend
   legend.isEnabled = false //关闭图例

   chart.data = pieData
   chart.invalidate()
   return chart
}


@Preview(showBackground = true)
@Composable
fun EmotionChartPreview() {
    EmotionChart(emotionScore = 75)
}
