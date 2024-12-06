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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.iflytek.cloud.RecognizerResult
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.ui.RecognizerDialog
import com.iflytek.cloud.ui.RecognizerDialogListener
import com.starry.greenstash.R
import com.starry.greenstash.database.transaction.TransactionType
import com.starry.greenstash.iflytek.speech.util.JsonParser.parseIatResult
import com.starry.greenstash.ui.navigation.NormalScreens
import com.starry.greenstash.ui.screens.recognition.RecognitionViewModel
import com.starry.greenstash.utils.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(
    viewModel: RecognitionViewModel = hiltViewModel(),
    navController: NavController,
    goalId: Long,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var displayedImage by remember { mutableStateOf<Bitmap?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImage = uri
        uri?.let {
            displayedImage = ImageUtils.uriToBitmap(it, context, 200)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.READ_MEDIA_IMAGES] == true || permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
                imagePickerLauncher.launch("image/*")
            }

            if (permissions[Manifest.permission.RECORD_AUDIO] != true) {
                Toast.makeText(context, context.getString(R.string.record_audio_permission_denied), Toast.LENGTH_SHORT).show()
            }

            if (permissions.values.any { !it }) {
                Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    )

    val recognizerDialog = RecognizerDialog(context, null)

// 设置识别参数 (在onCreate中设置一次即可)
    recognizerDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn")
    recognizerDialog.setParameter(SpeechConstant.ACCENT, "mandarin")
    recognizerDialog.setParameter(SpeechConstant.ASR_PTT, "0") // 关闭标点符号


    val recognizerDialogListener = object : RecognizerDialogListener {
        override fun onResult(results: RecognizerResult, isLast: Boolean) {
            val text = parseIatResult(results.resultString) // 现在可以正确调用了
            inputText = text
        }

        override fun onError(error: SpeechError) {
            println("Speech recognition error: ${error.errorDescription}")
            Toast.makeText(context, "Error: ${error.errorDescription}", Toast.LENGTH_SHORT).show()
        }
    }


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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text(stringResource(id = R.string.input_hint)) },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                try {
                                    recognizerDialog.setListener(recognizerDialogListener)
                                    recognizerDialog.show()
                                } catch (e: Exception) {
                                    viewModel.showSnackbar(context.getString(R.string.speech_recognition_not_supported), snackbarHostState)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = stringResource(id = R.string.speech_input)
                                )
                            }

                            IconButton(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_MEDIA_IMAGES,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.AddAPhoto,
                                    contentDescription = stringResource(id = R.string.add_image)
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
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
            if (isAnalysisSuccessful) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.transaction_type_label),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = viewModel.transactionType.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.amount_label),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = viewModel.amount,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.note_label),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = viewModel.note,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
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
