package com.starry.greenstash.ui.components

import android.app.Activity
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.starry.greenstash.MainViewModel
import com.starry.greenstash.database.core.AppDatabase
import com.starry.greenstash.ui.screens.home.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun SendDatabaseDataButton(
    navController: NavController,
//    appDatabase: AppDatabase,
    viewModel: HomeViewModel
) {
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
                coroutineScope.launch {
                    println("Starting fetchAndSetBillAnalyzerParams")
                    viewModel.fetchAndSetBillAnalyzerParams()
                    val params = viewModel.billAnalyzerParams.value
                    if (params != null) {
                        println("Params: $params")
                        navController.navigate("billAnalyzerScreen/$params")
                    } else {
                        println("Params is null")
                        Toast.makeText(Activity(), "无法获取数据", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Text("进行AI分析")
        }
    }
}
