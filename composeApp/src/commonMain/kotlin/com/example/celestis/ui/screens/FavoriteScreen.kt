package com.example.celestis.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.celestis.model.ApodResponse
import com.example.celestis.ui.navigation.ApodTopAppBar
import com.example.celestis.ui.navigation.TopBarState
import com.example.celestis.ui.viewModels.FavoriteUiState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.FavoriteScreen(
    uiState: FavoriteUiState,
    windowSizeClass: WindowSizeClass,
    onPhotoDetailClick: (ApodResponse) -> Unit,
    hazeState: HazeState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    topBarState: TopBarState
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is FavoriteUiState.Loading -> {
                    FavoriteScreenLoading()
                }

                is FavoriteUiState.Error -> {
                    FavoriteScreenError(state)
                }

                is FavoriteUiState.Success -> {
                    FavoriteScreenSuccess(
                        state = state,
                        windowSizeClass = windowSizeClass,
                        onPhotoDetailClick = onPhotoDetailClick,
                        animatedVisibilityScope = animatedVisibilityScope,
                        topBarState = topBarState
                    )
                }
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = topBarState.isVisible,
            enter = androidx.compose.animation.slideInVertically { -it },
            exit = androidx.compose.animation.slideOutVertically { -it }
        ) {
            ApodTopAppBar(
                titleContent = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FAVORITES",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                },
                hazeState = hazeState,
            )
        }
    }
}

@Composable
fun FavoriteScreenLoading() {

}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.FavoriteScreenSuccess(
    state: FavoriteUiState.Success,
    windowSizeClass: WindowSizeClass,
    onPhotoDetailClick: (ApodResponse) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    topBarState: TopBarState
) {
    val isLandscape = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val gridCols = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 4
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCols),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topBarState.nestedScrollConnection),
            contentPadding = PaddingValues(top = if (isLandscape) 92.dp else 114.dp, bottom = 80.dp)
        ) {
            items(
                items = state.favorites,
                // Date is a good key, but ensure it's unique!
                key = { it.date }
            ) { apod ->
                ApodCard(
                    apod = apod,
                    onPhotoDetailClick = { onPhotoDetailClick(apod) },
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }
}

@Composable
fun FavoriteScreenError(state: FavoriteUiState.Error) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        //implement retry button
//        Button() {
//            Text("Retry")
//        }
    }
}