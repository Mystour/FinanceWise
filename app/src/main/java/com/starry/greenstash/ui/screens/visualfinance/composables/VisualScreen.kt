package com.starry.greenstash.ui.screens.visualfinance.composables

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.starry.greenstash.ui.screens.visualfinance.VisualViewModel
import com.starry.greenstash.utils.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualScreen() {
    val viewModel: VisualViewModel = viewModel()
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

        Button(onClick = {
            permissionLauncher.launch(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            })
        }) {
            Text("选择图片")
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
            Text("分析图片")
        }

        if (viewModel.isLoading) {
            CircularProgressIndicator()
        }

        if (viewModel.analysisResult.isNotEmpty()) {
            Text(viewModel.analysisResult)
        }
    }
}
