package com.example.astrolume

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.astrolume.ui.navigation.AdaptiveNavigationWrapper
import com.example.astrolume.ui.navigation.ApodBottomNavBar
import com.example.astrolume.ui.navigation.NavGraph
import com.example.astrolume.ui.navigation.rememberWindowSizeClass
import com.example.astrolume.ui.theme.CelestisTheme
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalComposeUiApi::class)
@Composable
fun App(
    initialDeepLinkDate: String? = null
) {
    val navController = rememberNavController()
    val windowSizeClass = rememberWindowSizeClass()
    val widthClass = windowSizeClass.widthSizeClass
    val hazeState = remember { HazeState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Handle deep link navigation
    LaunchedEffect(initialDeepLinkDate) {
        initialDeepLinkDate?.let { date ->
            navController.navigate(com.example.astrolume.ui.navigation.Screen.PhotoDetail(date))
        }
    }

    // NEW: Auto-close drawer when switching to Compact (Portrait)
    LaunchedEffect(widthClass) {
        if (widthClass == WindowWidthSizeClass.Compact && drawerState.isOpen) {
            drawerState.close()
        }
    }

    CelestisTheme {
        AdaptiveNavigationWrapper(
            navController = navController,
            windowSizeClass = windowSizeClass,
            hazeState = hazeState,
            drawerState = drawerState
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    // Portrait only
                    if (widthClass == WindowWidthSizeClass.Compact) {
                        ApodBottomNavBar(navController)
                    }
                }
            ) { padding ->
                NavGraph(
                    navController = navController,
                    windowSizeClass = windowSizeClass,
                    modifier = Modifier.padding(padding),
                    onOpenDrawer = {
                        // Only allow opening if NOT in portrait
                        if (widthClass != WindowWidthSizeClass.Compact) {
                            scope.launch { drawerState.open() }
                        }
                    },
                    hazeState = hazeState
                )
            }
        }
    }
}