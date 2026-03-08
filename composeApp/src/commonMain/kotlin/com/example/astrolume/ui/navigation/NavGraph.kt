package com.example.astrolume.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.astrolume.ui.screens.HomeScreen
import com.example.astrolume.ui.viewModels.HomeViewModel
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

        composable<Screen.Search> {
            // Future: val viewModel: SearchViewModel = koinViewModel()
        }

        composable<Screen.Favorites> {
        }
    }
}