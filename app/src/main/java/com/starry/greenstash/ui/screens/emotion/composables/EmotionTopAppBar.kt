package com.starry.greenstash.ui.screens.emotion.composables

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
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