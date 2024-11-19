package com.starry.greenstash.ui.screens.recognition.composables

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.starry.greenstash.database.transaction.TransactionType
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.starry.greenstash.R

@Composable
fun ShowTransactionTypeSelectionDialog(
    onSelection: (TransactionType) -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = stringResource(id = R.string.select_transaction_type_title)) },
            text = {
                Column {
                    // 过滤掉 INVALID 值
                    TransactionType.entries.filter { it != TransactionType.Invalid }.forEach { type ->
                        TextButton(onClick = {
                            onSelection(type)
                            showDialog = false
                        }) {
                            Text(text = type.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
}
