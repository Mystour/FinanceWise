package com.starry.greenstash.ui.screens.analysis.composables

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(title: String) {
    CenterAlignedTopAppBar(
        title = { Text(text = title, textAlign = TextAlign.Center) }
    )
}