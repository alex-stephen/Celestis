package com.example.astrolume.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.astrolume.ui.screens.DiscoverScreen
import com.example.astrolume.ui.screens.FavoriteScreen
import com.example.astrolume.ui.screens.HomeScreen
import com.example.astrolume.ui.screens.PhotoDetailScreen
import com.example.astrolume.ui.viewModels.DiscoverViewModel
import com.example.astrolume.ui.viewModels.FavoriteViewModel
import com.example.astrolume.ui.viewModels.HomeViewModel
import com.example.astrolume.ui.viewModels.PhotoDetailViewModel
import dev.chrisbanes.haze.HazeState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit,
    hazeState: HazeState
) {
    NavHost(
        navController = navController,

        startDestination = Screen.Home,
        modifier = modifier
    ) {
        composable<Screen.Home> {
            val homeViewModel: HomeViewModel = koinViewModel()

            HomeScreen(
                viewModel = homeViewModel,
                windowSizeClass = windowSizeClass,
                onOpenDrawer = onOpenDrawer,
                hazeState = hazeState
            )
        }

        composable<Screen.Discover> {
            val viewModel: DiscoverViewModel = koinViewModel()
            DiscoverScreen(
                viewModel,
                windowSizeClass = windowSizeClass,
                onOpenDrawer = onOpenDrawer,
                onPhotoDetailClick = {
                    navController.navigate(Screen.PhotoDetail(it.date))
                },
                hazeState = hazeState)
        }

        composable<Screen.Favorites> {
            val viewModel: FavoriteViewModel = koinViewModel()
            FavoriteScreen(
                viewModel,
                windowSizeClass = windowSizeClass,
                onOpenDrawer = onOpenDrawer,
                onPhotoDetailClick = {
                    navController.navigate(Screen.PhotoDetail(it.date))
                },
                hazeState = hazeState
            )
        }

        composable<Screen.PhotoDetail> { backStackEntry ->
            val photoDetail: Screen.PhotoDetail = backStackEntry.toRoute()
            val viewModel: PhotoDetailViewModel = koinViewModel()
            PhotoDetailScreen(
                date = photoDetail.date,
                viewModel = viewModel,
                windowSizeClass = windowSizeClass,
                onNavigateBack = { navController.navigateUp() },
                onShare = { /* TODO: Implement share */ },
                hazeState = hazeState
            )
        }
    }
}
