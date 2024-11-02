package com.starry.greenstash.ui.screens.recognition.composables

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.starry.greenstash.R
import com.starry.greenstash.ui.screens.recognition.RecognitionViewModel
import com.starry.greenstash.utils.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualScreen() {
    val viewModel: RecognitionViewModel = viewModel()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 显示描述
        val description = stringResource(id = R.string.bill_recognition_desc)
        val annotatedDescription = buildAnnotatedString {
            append(description)
            // 可以在这里添加更多的样式，例如加粗某些部分
            // pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            // append("重要部分")
            // pop()
        }

        Text(
            text = annotatedDescription,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Justify,
            modifier = Modifier.padding(16.dp)
        )

        Button(onClick = {
            permissionLauncher.launch(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            })
        }) {
            Text(stringResource(id = R.string.select_image_button))
        }

        displayedImage?.let { bitmap ->  // Display image if available
            Image(
                bitmap = bitmap.asImageBitmap(), // 直接使用 bitmap.asImageBitmap()
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Button(onClick = {
            selectedImage?.let { uri ->
                val bitmap = ImageUtils.uriToBitmap(uri, context, 1024) // Use ImageUtils for analysis Bitmap
                viewModel.analyzeImage(bitmap) // Pass Bitmap to ViewModel
            }
        }, enabled = selectedImage != null) {
            Text(stringResource(id = R.string.analyze_image_button))
        }

        if (viewModel.isLoading) {
            CircularProgressIndicator()
        }

        if (viewModel.analysisResult.isNotEmpty()) {
            Text(viewModel.analysisResult)
        }

        // 使用 Box 布局来包裹 Text 组件，并将其对齐到 Box 的底部中心
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "滑动屏幕以查看更多内容",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            )
        }
    }
}

// 预览功能
@Preview(showBackground = true)
@Composable
fun VisualScreenPreview() {
    VisualScreen()
}
