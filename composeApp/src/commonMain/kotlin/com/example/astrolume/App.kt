package com.example.astrolume

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.astrolume.ui.navigation.AdaptiveNavigationWrapper
import com.example.astrolume.ui.navigation.ApodBottomNavBar
import com.example.astrolume.ui.navigation.NavGraph
import com.example.astrolume.ui.navigation.rememberWindowSizeClass

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val navController = rememberNavController()
    val windowSizeClass = rememberWindowSizeClass()
    val widthClass = windowSizeClass.widthSizeClass

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveNavigationWrapper(
                navController = navController,
                widthClass = widthClass
            ) {
                // Force the Scaffold to take up all available space from the wrapper
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (widthClass == WindowWidthSizeClass.Compact) {
                            ApodBottomNavBar(navController)
                        }
                    }
                ) { padding ->
                    // Use the padding only on the NavGraph
                    NavGraph(
                        navController = navController,
                        windowSizeClass = windowSizeClass,
                        modifier = Modifier.padding(padding).fillMaxSize()
                    )
                }
            }
        }
    }
}