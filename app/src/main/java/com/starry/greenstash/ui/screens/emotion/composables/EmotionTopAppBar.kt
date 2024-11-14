package com.starry.greenstash.ui.screens.emotion.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.starry.greenstash.ui.screens.emotion.EmotionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionTopAppBar(
    title: String,
    searchText: String,
    onSearchInputChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    filterType: EmotionViewModel.FilterType,
    onFilterTypeChange: (EmotionViewModel.FilterType) -> Unit,
    onRefreshClick: () -> Unit // 添加刷新回调
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    TopAppBar(
        title = { Text(text = title) },
        actions = {
            // Filter Button
            IconButton(onClick = { filterMenuExpanded = true }) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filter")
            }

            // Search Field (only shown when filterType is Title)
            if (filterType == EmotionViewModel.FilterType.Title) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchInputChange,
                    placeholder = { Text("Search") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        onSearchAction()
                        keyboardController?.hide()
                    }),
                    modifier = Modifier.widthIn(max = 240.dp) // Limit width
                )
            }

            // Refresh Button
            IconButton(onClick = onRefreshClick) {  // 使用提供的回调
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }


            DropdownMenu(
                expanded = filterMenuExpanded,
                onDismissRequest = { filterMenuExpanded = false }
            ) {
                EmotionViewModel.FilterType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            onFilterTypeChange(type)
                            filterMenuExpanded = false
                        }
                    )
                }
            }
        }
    )
}