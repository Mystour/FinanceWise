package com.starry.greenstash.ui.screens.recognition.composables

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.starry.greenstash.R
import com.starry.greenstash.database.transaction.TransactionType
import com.starry.greenstash.ui.navigation.NormalScreens
import com.starry.greenstash.ui.screens.recognition.RecognitionViewModel
import com.starry.greenstash.utils.ImageUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(
    viewModel: RecognitionViewModel = hiltViewModel(),
    navController: NavController,
    goalId: Long
) {
    val context = LocalContext.current

    var selectedImage by remember { mutableStateOf<Uri?>(null) }  // Store the Uri
    var displayedImage by remember { mutableStateOf<Bitmap?>(null) } // For displaying the image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImage = uri
        uri?.let {
            displayedImage = ImageUtils.uriToBitmap(it, context, 200) // Use ImageUtils for Bitmap
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "未获得必要的图片访问权限", Toast.LENGTH_SHORT).show()
        }
    }

    val analysisStream by viewModel.analysisStream.collectAsState()
    val listState = rememberLazyListState()
    var selectedTransactionType by remember { mutableStateOf<TransactionType?>(null) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isAnalysisSuccessful by viewModel.isAnalysisSuccessful.collectAsState()

    LaunchedEffect(analysisStream) {
        listState.scrollToItem(0) // 滚动到顶部
        delay(100) // 等待一段时间，确保内容更新
        listState.scrollToItem(listState.layoutInfo.visibleItemsInfo.size - 1) // 滚动到底部
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            // 显示标题
            Text(
                text = stringResource(id = R.string.bill_recognition_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        item {
            Button(onClick = {
                permissionLauncher.launch(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                })
            }) {
                Text(stringResource(id = R.string.select_image_button))
            }
        }

        item {
            displayedImage?.let { bitmap ->  // Display image if available
                Image(
                    bitmap = bitmap.asImageBitmap(), // 直接使用 bitmap.asImageBitmap()
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        item {
            // 分析图片按钮
            Button(onClick = {
                selectedImage?.let { uri ->
                    val bitmap = ImageUtils.uriToBitmap(uri, context, 1024) // Use ImageUtils for analysis Bitmap
                    viewModel.analyzeImage(bitmap)
                }
            }, enabled = selectedImage != null && !isAnalyzing) {
                Text(stringResource(id = R.string.analyze_image_button))
            }
        }

        item {
            if (viewModel.isLoading) {
                CircularProgressIndicator()
            }
        }

        item {
            if (analysisStream.isNotEmpty()) {
                Text(analysisStream)
            }
        }

        item {
            // 新增的按钮
            if (isAnalysisSuccessful) { // 仅当分析成功时显示
                Button(onClick = {
                    if (selectedImage != null && !viewModel.isLoading) {
                        val transactionType = viewModel.transactionType
                        amount = viewModel.amount
                        note = viewModel.note
                        if (transactionType == TransactionType.Invalid) {
                            showTransactionDialog = true // Set the state to true to show the dialog
                        } else {
                            navController.navigate(NormalScreens.DWScreen(goalId.toString(), transactionType.name, amount, note))
                        }
                    }
                }, enabled = !isAnalyzing) {  // 仅当不在分析时启用
                    Text(stringResource(id = R.string.add_to_transaction_button))
                }
            }
        }

        item {
            if (showTransactionDialog) {
                ShowTransactionTypeSelectionDialog { selectedType ->
                    selectedTransactionType = selectedType
                    navController.navigate(NormalScreens.DWScreen(goalId.toString(), selectedType.name, amount, note))
                    showTransactionDialog = false // Hide the dialog after selection
                }
            }
        }

        item {
            // 使用 Box 布局来包裹 Text 组件，并将其对齐到 Box 的底部中心
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = stringResource(id = R.string.bill_recognition_desc),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                )
            }
        }
    }
}



// 预览功能
//@Preview(showBackground = true)
//@Composable
//fun RecognitionScreenPreview() {
//    RecognitionScreen()
//}
