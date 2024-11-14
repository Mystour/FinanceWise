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
    showTitle: Boolean,
    searchText: String,
    onSearchInputChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    filterType: EmotionViewModel.FilterType,
    onFilterTypeChange: (EmotionViewModel.FilterType) -> Unit,
    onRefreshClick: () -> Unit
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    TopAppBar(
        title = { if (showTitle) Text(text = title) }, // Conditional title display
        actions = {
            // Filter menu
            IconButton(onClick = { filterMenuExpanded = true }) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filter")
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
                            if (type != EmotionViewModel.FilterType.Title) {
                                onSearchInputChange("") // Clear search text if not Title filter
                                keyboardController?.hide() // Hide keyboard
                            }
                        }
                    )
                }
            }


            // Search field (only for Title filter)
            if (filterType == EmotionViewModel.FilterType.Title) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchInputChange,
                    placeholder = { Text("Search...") }, // More descriptive placeholder
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        onSearchAction()
                        keyboardController?.hide()
                    }),
                    modifier = Modifier.widthIn(max = 240.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,  // Use appropriate text style
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, // Customize colors
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Refresh button
            IconButton(onClick = onRefreshClick) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    )
}