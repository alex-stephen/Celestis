package com.example.astrolume.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val windowDpSize = with(density) {
        val size = windowInfo.containerSize
        DpSize(size.width.toDp(), size.height.toDp())
    }

    return WindowSizeClass.calculateFromSize(windowDpSize)
}

@Composable
fun AdaptiveNavigationWrapper(
    navController: NavHostController,
    widthClass: WindowWidthSizeClass,
    content: @Composable () -> Unit
) {
    when (widthClass) {
        WindowWidthSizeClass.Expanded -> {
            ApodPermanentDrawer(
                navController = navController,
                content = content // The drawer handles the fill
            )
        }
        WindowWidthSizeClass.Medium -> {
            Row(Modifier.fillMaxSize()) {
                ApodNavRail(navController)
                // Use weight(1f) to ensure the content takes up the REST of the screen
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    content()
                }
            }
        }
        else -> content() // Compact
    }
}