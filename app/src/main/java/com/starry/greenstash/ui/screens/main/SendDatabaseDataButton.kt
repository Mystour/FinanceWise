package com.starry.greenstash.ui.screens.main

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Button
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.starry.greenstash.database.core.AppDatabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SendDatabaseDataButton(activity: Activity, appDatabase: AppDatabase) {
    val coroutineScope = rememberCoroutineScope()

    Spacer(modifier = Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    // 查询数据库
                    val goals = appDatabase.getGoalDao().getAllGoals()

                    // 切换到主线程以启动活动
                    withContext(Dispatchers.Main) {
                        // 构建意图
                        val intent = Intent().apply {
                            setClassName("com.ai.financeWise", "com.ai.financeWise.MainActivity")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                            // 将数据放入意图
                            putExtra("goals", ArrayList(goals))
                        }

                        try {
                            activity.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                            // 提示用户目标应用未安装或无法找到
                            Toast.makeText(activity, "目标应用未安装或无法找到", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        ) {
            Text("进行AI分析")
        }
    }
}

