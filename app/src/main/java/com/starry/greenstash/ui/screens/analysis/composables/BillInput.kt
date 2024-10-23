package com.starry.greenstash.ui.screens.analysis.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillInput(billText: String, onBillTextChange: (String) -> Unit) {
    OutlinedTextField(
        value = billText,
        onValueChange = onBillTextChange,
        label = { Text("输入账单内容") },
        modifier = Modifier
            .padding(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )
}
