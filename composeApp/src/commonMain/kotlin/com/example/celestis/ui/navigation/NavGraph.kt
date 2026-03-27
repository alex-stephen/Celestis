package com.example.celestis.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import com.example.celestis.ui.screens.MainPagerScreen
import com.example.celestis.ui.screens.PhotoDetailScreen
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
            startDestination = "main_pager",
            modifier = modifier,
            enterTransition = { Transitions.tabEnter },
            exitTransition = { Transitions.tabExit },
            popEnterTransition = { Transitions.tabEnter },
            popExitTransition = { Transitions.tabExit }
        ) {
            // Main pager screen with horizontal swipe navigation
            composable("main_pager") {
                MainPagerScreen(
                    windowSizeClass = windowSizeClass,
                    hazeState = hazeState,
                    onNavigateToDetail = { date ->
                        navController.navigate(Screen.PhotoDetail(date))
                    },
                    onOpenDrawer = onOpenDrawer,
                    bottomPadding = bottomPadding
                )
            }

            // Photo detail screen
            composable<Screen.PhotoDetail>(
                enterTransition = { Transitions.detailEnter },
                exitTransition = { Transitions.detailExit },
                popEnterTransition = { Transitions.tabEnter },
                popExitTransition = { Transitions.detailExit }
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
