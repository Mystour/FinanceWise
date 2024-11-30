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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
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

    var inputText by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var displayedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImage = uri
        uri?.let {
            displayedImage = ImageUtils.uriToBitmap(it, context, 200)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val analysisStream by viewModel.analysisStream.collectAsState()
    var selectedTransactionType by remember { mutableStateOf<TransactionType?>(null) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isAnalysisSuccessful by viewModel.isAnalysisSuccessful.collectAsState()


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.bill_recognition_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        item {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text(stringResource(id = R.string.input_hint)) },
                trailingIcon = { //  将两个图标都放在 trailingIcon 中
                    Row(
                        verticalAlignment = Alignment.CenterVertically //  垂直居中对齐图标
                    ) {
                        IconButton(onClick = { /* 语音识别逻辑 */ }) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = stringResource(id = R.string.speech_input)
                            )
                        }
                        IconButton(onClick = {
                            permissionLauncher.launch(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Manifest.permission.READ_MEDIA_IMAGES
                                } else {
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                }
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = stringResource(id = R.string.add_image)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        item {
            if (displayedImage != null) {
                Image(
                    bitmap = displayedImage!!.asImageBitmap(),
                    contentDescription = stringResource(id = R.string.selected_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        item {
            Button(onClick = {
                selectedImage?.let { uri ->
                    val bitmap = ImageUtils.uriToBitmap(uri, context, 1024)
                    viewModel.analyzeImage(bitmap, inputText)
                } ?: run {
                    viewModel.analyzeImage(null, inputText)
                }
            }, enabled = (inputText.isNotBlank() || selectedImage != null) && !isAnalyzing) {
                Text(stringResource(id = R.string.analyze_button))
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
            if (isAnalysisSuccessful) {
                Button(onClick = {
                    val transactionType = viewModel.transactionType
                    amount = viewModel.amount
                    note = viewModel.note

                    if (transactionType == TransactionType.Invalid) {
                        showTransactionDialog = true
                    } else {
                        navController.navigate(
                            NormalScreens.DWScreen(
                                goalId.toString(),
                                transactionType.name,
                                amount,
                                note
                            )
                        )
                    }
                }, enabled = !isAnalyzing) {
                    Text(stringResource(id = R.string.add_to_transaction_button))
                }
            }
        }

        item {
            if (showTransactionDialog) {
                ShowTransactionTypeSelectionDialog { selectedType ->
                    selectedTransactionType = selectedType
                    navController.navigate(
                        NormalScreens.DWScreen(
                            goalId.toString(),
                            selectedType.name,
                            amount,
                            note
                        )
                    )
                    showTransactionDialog = false
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = stringResource(id = R.string.bill_recognition_desc),
                    style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                )
            }
        }
    }
}