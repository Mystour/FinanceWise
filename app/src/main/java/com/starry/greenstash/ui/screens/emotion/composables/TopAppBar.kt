package com.starry.greenstash.ui.screens.emotion.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: String,
    onFilterClicked: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = { Text(text = title, textAlign = TextAlign.Center) },
        actions = {
            IconButton(onClick = {
                expanded = true
                onFilterClicked()
            }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Option 1") },
                    onClick = {
                        expanded = false
                        // 处理 Option 1 的点击事件
                    }
                )
                DropdownMenuItem(
                    text = { Text("Option 2") },
                    onClick = {
                        expanded = false
                        // 处理 Option 2 的点击事件
                    }
                )
                DropdownMenuItem(
                    text = { Text("Option 3") },
                    onClick = {
                        expanded = false
                        // 处理 Option 3 的点击事件
                    }
                )
            }
        }
    )
}
