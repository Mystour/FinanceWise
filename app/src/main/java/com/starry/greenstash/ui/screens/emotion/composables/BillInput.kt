package com.starry.greenstash.ui.screens.emotion.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.starry.greenstash.R

@Composable
fun BillInput(billText: String, onBillTextChange: (String) -> Unit) {
    OutlinedTextField(
        value = billText,
        onValueChange = onBillTextChange,
        label = { Text(text = stringResource(id = R.string.content_hint)) },
        modifier = Modifier.padding(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )
}

