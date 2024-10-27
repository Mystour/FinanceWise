   package com.starry.greenstash.ui.screens.analysis.composables

   import android.content.Context
   import androidx.compose.foundation.layout.height
   import androidx.compose.foundation.layout.padding
   import androidx.compose.runtime.Composable
   import androidx.compose.ui.Modifier
   import androidx.compose.ui.graphics.Color
   import androidx.compose.ui.graphics.toArgb
   import androidx.compose.ui.viewinterop.AndroidView
   import androidx.compose.ui.unit.dp
   import com.github.mikephil.charting.charts.PieChart
   import com.github.mikephil.charting.components.Legend
   import com.github.mikephil.charting.data.PieData
   import com.github.mikephil.charting.data.PieDataSet
   import com.github.mikephil.charting.data.PieEntry
   import com.github.mikephil.charting.utils.ColorTemplate

   // 情绪评分图表
   @Composable
   fun EmotionChart(emotionScore: Int) {
       val entries = listOf(
           PieEntry(emotionScore.toFloat(), "情绪评分"),
           PieEntry((100 - emotionScore).toFloat(), "") // 补充剩余部分
       )

       val dataSet = PieDataSet(entries, "")
       dataSet.colors = listOf(Color(0xFF6200EE).toArgb(), Color(0x00000000).toArgb()) // 设置环形图颜色
       dataSet.sliceSpace = 3f // 设置饼图切片之间的间距
       dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE // 设置值的位置
       dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

       val pieData = PieData(dataSet)

       AndroidView(
           factory = { context ->
               createAndConfigurePieChart(context, pieData, emotionScore)
           },
           modifier = Modifier
               .height(200.dp)
               .padding(16.dp)
       )
   }

   private fun createAndConfigurePieChart(context: Context, pieData: PieData, emotionScore: Int): PieChart {
       val chart = PieChart(context)
       chart.setUsePercentValues(false) // 不使用百分比显示
       chart.description.isEnabled = false // 关闭描述
       chart.setExtraOffsets(5f, 10f, 5f, 5f) // 设置额外偏移
       chart.dragDecelerationFrictionCoef = 0.95f // 设置拖动减速摩擦系数
       chart.isDrawHoleEnabled = true // 开启中心孔
       chart.setHoleColor(Color(0x00000000).toArgb()) // 设置中心孔颜色
       chart.setTransparentCircleColor(Color(0x00000000).toArgb()) // 设置透明圆圈颜色
       chart.setTransparentCircleAlpha(110) // 设置透明圆圈透明度
       chart.holeRadius = 58f // 设置中心孔半径
       chart.transparentCircleRadius = 61f // 设置透明圆圈半径
       chart.setDrawCenterText(true) // 开启中心文字
       chart.centerText = "情绪评分\n$emotionScore" // 设置中心文字
       chart.setCenterTextSize(14f) // 设置中心文字大小
       chart.setDrawEntryLabels(false) // 关闭标签
       chart.legend.isEnabled = false // 关闭图例
       chart.data = pieData // 设置数据
       chart.invalidate() // 刷新图表
       return chart
   }
   