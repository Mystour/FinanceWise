package com.starry.greenstash.ui.screens.home.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.starry.greenstash.R
import com.starry.greenstash.ui.navigation.NormalScreens
import com.starry.greenstash.ui.theme.greenstashFont
import com.starry.greenstash.utils.isScrollingUp
import com.starry.greenstash.utils.weakHapticFeedback

@Composable
fun EmotionAnalysisFAB(
    modifier: Modifier,
    lazyListState: LazyListState,
    navController: NavController,
) {
    val isFabVisible = lazyListState.isScrollingUp()
    val density = LocalDensity.current
    val view = LocalView.current

    AnimatedVisibility(
        visible = isFabVisible,
        enter = slideInVertically {
            with(density) { 40.dp.roundToPx() }
        } + fadeIn(),
        exit = fadeOut(
            animationSpec = keyframes {
                this.durationMillis = 120
            }
        )
    ) {
        ExtendedFloatingActionButton(
            modifier = modifier.padding(end = 10.dp, bottom = 12.dp),
            onClick = {
                view.weakHapticFeedback()
                navController.navigate(NormalScreens.EmotionScreen)
            },
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SentimentVerySatisfied,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.emotion_analysis_button),
                modifier = Modifier.padding(top = 2.dp),
                fontFamily = greenstashFont,
                color = MaterialTheme.colorScheme.onSecondaryContainer // 显式指定颜色
            )
        }
    }
}