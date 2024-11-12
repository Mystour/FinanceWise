package com.starry.greenstash.ui.screens.emotion.composables

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.starry.greenstash.database.goal.GoalDao
import com.starry.greenstash.ui.screens.emotion.EmotionViewModel
import com.starry.greenstash.utils.PreferenceUtil

class EmotionViewModelFactory(
    private val goalDao: GoalDao,
    private val context: Context,
    private val preferenceUtil: PreferenceUtil
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmotionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmotionViewModel(context, goalDao, preferenceUtil) as T  // 将 goalDao, context 和 preferenceUtil 传递给 ViewModel 的构造函数
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
