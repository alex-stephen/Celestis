package com.example.astrolume.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.astrolume.ui.screens.HomeScreen
import com.example.astrolume.ui.viewModels.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,

        startDestination = Screen.Home,
        modifier = modifier
    ) {
        composable<Screen.Home> {
            val viewModel: HomeViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            HomeScreen(
                state = state,
                windowSizeClass = windowSizeClass, // NEW: Pass it to the screen
                onRefresh = viewModel::refreshAll,
                onFavoriteToggle = viewModel::toggleFavorite
            )
        }

        composable<Screen.Search> {
            // Future: val viewModel: SearchViewModel = koinViewModel()
        }

        composable<Screen.Favorites> {
        }
    }
}