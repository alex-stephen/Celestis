package com.example.astrolume.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.navigation.NavHostController
import dev.chrisbanes.haze.HazeState

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
    windowSizeClass: WindowSizeClass, // Pass the full class, not just width
    hazeState: HazeState,
    drawerState: DrawerState,
    content: @Composable () -> Unit
) {
    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    // A true tablet usually has Expanded width AND at least Medium height.
    // A phone in landscape has Expanded width but Compact height.
    val isTablet = widthClass == WindowWidthSizeClass.Expanded &&
            heightClass != WindowHeightSizeClass.Compact

    if (isTablet) {
        // TABLET/DESKTOP: Fixed side-by-side layout
        Row(Modifier.fillMaxSize()) {
            ApodPermanentDrawer(navController = navController) {
                Box(Modifier.fillMaxSize()) { content() }
            }
        }
    } else {
        // ANY MOBILE (Portrait or Landscape): Overlay layout
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Enable swipe-to-open only if we aren't in portrait
            gesturesEnabled = widthClass != WindowWidthSizeClass.Compact,
            drawerContent = {
                ApodDrawerSheet(navController, drawerState, hazeState)
            }
        ) {
            // Stacked layout: content fills 100% of the screen
            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}