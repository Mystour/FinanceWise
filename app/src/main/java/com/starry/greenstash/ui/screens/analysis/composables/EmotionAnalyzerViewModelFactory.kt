package com.starry.greenstash.ui.screens.analysis.composables

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.starry.greenstash.ui.screens.analysis.EmotionAnalyzerViewModel

class EmotionAnalyzerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmotionAnalyzerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmotionAnalyzerViewModel(context) as T  // 将 context 传递给 ViewModel 的构造函数
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}