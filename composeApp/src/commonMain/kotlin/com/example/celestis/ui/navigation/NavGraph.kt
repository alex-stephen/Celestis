package com.example.celestis.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.celestis.ui.screens.DiscoverScreen
import com.example.celestis.ui.screens.FavoriteScreen
import com.example.celestis.ui.screens.HomeScreen
import com.example.celestis.ui.screens.PhotoDetailScreen
import com.example.celestis.ui.viewModels.DiscoverViewModel
import com.example.celestis.ui.viewModels.FavoriteViewModel
import com.example.celestis.ui.viewModels.HomeViewModel
import com.example.celestis.ui.viewModels.PhotoDetailViewModel
import dev.chrisbanes.haze.HazeState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit,
    hazeState: HazeState,
    bottomPadding: Dp
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = modifier
        ) {
            composable<Screen.Home> {
                val homeViewModel: HomeViewModel = koinViewModel()
                val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                val isShowingRandom by homeViewModel.isShowingRandom.collectAsStateWithLifecycle()
                val isFetchingRandom by homeViewModel.isRefilling.collectAsStateWithLifecycle()
                val isImageLoading by homeViewModel.isImageLoading.collectAsStateWithLifecycle()

                HomeScreen(
                    uiState = uiState,
                    isShowingRandom = isShowingRandom,
                    isFetchingRandom = isFetchingRandom,
                    isImageLoading = isImageLoading,
                    onShare = homeViewModel::shareApod,
                    onRefresh = homeViewModel::showNextRandom,
                    onBackToToday = homeViewModel::showToday,
                    onFavoriteToggle = homeViewModel::toggleFavorite,
                    onShowHdImage = homeViewModel::showHdImage,
                    onHideHdImage = homeViewModel::hideHdImage,
                    windowSizeClass = windowSizeClass,
                    onOpenDrawer = onOpenDrawer,
                    hazeState = hazeState,
                    bottomPadding = bottomPadding
                )
            }

            composable<Screen.Discover> {
                val viewModel: DiscoverViewModel = koinViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                
                DiscoverScreen(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    onQueryChange = viewModel::updateQuery,
                    onSearch = viewModel::executeSearch,
                    onLoadMoreSearchResults = viewModel::loadMoreSearchResults,
                    onDateRangeSelected = viewModel::onDateRangeSelected,
                    windowSizeClass = windowSizeClass,
                    onOpenDrawer = onOpenDrawer,
                    onPhotoDetailClick = {
                        navController.navigate(Screen.PhotoDetail(it.date))
                    },
                    hazeState = hazeState,
                    animatedVisibilityScope = this
                )
            }

            composable<Screen.Favorites> {
                val viewModel: FavoriteViewModel = koinViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                
                FavoriteScreen(
                    uiState = uiState,
                    windowSizeClass = windowSizeClass,
                    onOpenDrawer = onOpenDrawer,
                    onPhotoDetailClick = {
                        navController.navigate(Screen.PhotoDetail(it.date))
                    },
                    hazeState = hazeState,
                    animatedVisibilityScope = this
                )
            }

            composable<Screen.PhotoDetail>(
                enterTransition = {
                    fadeIn(animationSpec = tween(400))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(400))
                }
            ) { backStackEntry ->
                val photoDetail: Screen.PhotoDetail = backStackEntry.toRoute()
                val viewModel: PhotoDetailViewModel = koinViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                PhotoDetailScreen(
                    date = photoDetail.date,
                    state = uiState,
                    onFavoriteClick = viewModel::toggleFavorite,
                    onHideHdImage = viewModel::hideHdImage,
                    onShowHdImage = viewModel::showHdImage,
                    onShare = viewModel::shareApod,
                    onLoadApodByDate = viewModel::loadApodByDate,
                    windowSizeClass = windowSizeClass,
                    onNavigateBack = { navController.navigateUp() },
                    hazeState = hazeState,
                    animatedVisibilityScope = this
                )
            }
        }
    }
}
