package com.starry.greenstash.ui.screens.home.composables

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.starry.greenstash.R
import com.starry.greenstash.ui.navigation.NormalScreens

@Composable
fun SendDatabaseDataButton(
    navController: NavController,
    activity: Activity // 添加 activity 参数
) {
    val errorMessage = stringResource(id = R.string.emotion_analysis_button_error) // 提前获取字符串资源

    Spacer(modifier = Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Button(
            onClick = {
                navController.navigate(NormalScreens.EmotionScreen)
            }
        ) {
            Text(text = stringResource(id = R.string.emotion_analysis_button))
        }
    }
}
