package com.example.astrolume

import androidx.compose.foundation.layout.Row
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
import com.example.astrolume.ui.navigation.ApodBottomNavBar
import com.example.astrolume.ui.navigation.ApodNavRail
import com.example.astrolume.ui.navigation.ApodPermanentDrawer
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
            when (widthClass) {
                // 1. PHONE (Portrait)
                WindowWidthSizeClass.Compact -> {
                    Scaffold(
                        bottomBar = { ApodBottomNavBar(navController) }
                    ) { padding ->
                        NavGraph(navController, windowSizeClass, Modifier.padding(padding))
                    }
                }

                // 2. FOLDABLE / SMALL TABLET (Landscape/Small Screen)
                WindowWidthSizeClass.Medium -> {
                    Row(Modifier.fillMaxSize()) {
                        ApodNavRail(navController)
                        NavGraph(navController, windowSizeClass, Modifier.weight(1f))
                    }
                }

                // 3. DESKTOP / IPAD PRO / TABLET (Expanded)
                WindowWidthSizeClass.Expanded -> {
                    // Using the Permanent Drawer for a "Pro" desktop feel
                    ApodPermanentDrawer(navController) {
                        NavGraph(navController, windowSizeClass, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}