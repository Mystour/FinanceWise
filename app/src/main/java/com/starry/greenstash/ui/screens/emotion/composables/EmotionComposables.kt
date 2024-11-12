package com.starry.greenstash.ui.screens.emotion.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import com.starry.greenstash.R
import com.starry.greenstash.database.core.GoalWithTransactions
import com.starry.greenstash.ui.theme.greenstashFont
import com.starry.greenstash.ui.screens.emotion.EmotionViewModel
import androidx.compose.runtime.State
import androidx.navigation.NavController
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope

@Composable
fun GoalSearchResults(
    allGoalState: State<List<GoalWithTransactions>>,
    searchTextState: String,
    viewModel: EmotionViewModel,
    coroutineScope: CoroutineScope,
    navController: NavController,
    snackBarHostState: SnackbarHostState
) {
    val allGoals = allGoalState.value
    val filteredList = allGoals.filter { goalItem ->
        goalItem.goal.title.lowercase().contains(searchTextState.lowercase())
    }

    if (allGoals.isNotEmpty() && filteredList.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val compositionResult: LottieCompositionResult =
                rememberLottieComposition(
                    spec = LottieCompositionSpec.RawRes(R.raw.no_goal_found_lottie)
                )
            val progressAnimation by animateLottieCompositionAsState(
                compositionResult.value,
                isPlaying = true,
                iterations = LottieConstants.IterateForever,
                speed = 1f
            )

            Spacer(modifier = Modifier.weight(1f))

            LottieAnimation(
                composition = compositionResult.value,
                progress = { progressAnimation },
                modifier = Modifier.size(320.dp),
                enableMergePaths = true
            )

            Text(
                text = stringResource(id = R.string.search_goal_not_found),
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                fontFamily = greenstashFont,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp)
            )

            Spacer(modifier = Modifier.weight(2f))
        }

    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            items(
                count = filteredList.size,
                key = { k -> filteredList[k].goal.goalId },
                contentType = { 0 }
            ) { idx ->
                val item = filteredList[idx]
                Box(modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)) {
                    GoalLazyColumnItem(
                        viewModel = viewModel,
                        item = item,
                        coroutineScope = coroutineScope,
                        navController = navController,
                        snackBarHostState = snackBarHostState
                    )
                }
            }
        }
    }
}
