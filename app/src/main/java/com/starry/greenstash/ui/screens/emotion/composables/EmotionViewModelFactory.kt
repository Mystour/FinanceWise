package com.starry.greenstash.ui.screens.emotion.composables

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.starry.greenstash.ui.screens.emotion.EmotionViewModel

class EmotionViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmotionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmotionViewModel(context) as T  // 将 context 传递给 ViewModel 的构造函数
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}