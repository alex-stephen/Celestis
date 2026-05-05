package com.example.celestis.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.chrisbanes.haze.HazeState

val ApodNavRailWidth: Dp = 120.dp
val ApodPermanentDrawerWidth: Dp = 192.dp

fun apodNavigationOverlayWidth(windowSizeClass: WindowSizeClass): Dp {
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 0.dp
        WindowWidthSizeClass.Medium -> ApodNavRailWidth
        WindowWidthSizeClass.Expanded -> {
            if (windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact) {
                ApodNavRailWidth
            } else {
                ApodPermanentDrawerWidth
            }
        }
        else -> 0.dp
    }
}

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

/**
 * Adaptive navigation wrapper implementing Material 3 canonical layouts:
 * - Compact width (< 600dp): Bottom Navigation Bar (portrait only)
 * - Medium width (600dp - 840dp): Navigation Rail (landscape/tablets)
 * - Expanded width (> 840dp): Permanent Navigation Drawer (large tablets/desktop)
 * 
 * Reference: https://m3.material.io/foundations/layout/applying-layout/window-size-classes
 */
@Composable
fun AdaptiveNavigationWrapper(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    hazeState: HazeState,
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    when (widthClass) {
        WindowWidthSizeClass.Compact -> {
            // COMPACT (< 600dp): Bottom Bar only
            // Bottom bar is handled inside MainPagerScreen
            Box(Modifier.fillMaxSize()) {
                content()
            }
        }

        WindowWidthSizeClass.Medium -> {
            // MEDIUM (600dp - 840dp): Navigation Rail (landscape phones/small tablets)
            Box(Modifier.fillMaxSize()) {
                content()
                ApodNavRail(
                    currentPageIndex = currentPageIndex,
                    onPageSelected = onPageSelected,
                    hazeState = hazeState,
                )
            }
        }

        WindowWidthSizeClass.Expanded -> {
            // EXPANDED (> 840dp): Permanent Drawer or NavRail
            // Only use permanent drawer if we have sufficient height (not landscape phone)
            if (heightClass != WindowHeightSizeClass.Compact) {
                ApodPermanentDrawer(
                    navController = navController,
                    currentPageIndex = currentPageIndex,
                    onPageSelected = onPageSelected,
                    hazeState = hazeState
                ) {
                    content()
                }
            } else {
                // Landscape phone with expanded width but compact height - use NavRail
                Box(Modifier.fillMaxSize()) {
                    content()
                    ApodNavRail(
                        currentPageIndex = currentPageIndex,
                        onPageSelected = onPageSelected,
                        hazeState = hazeState,
                    )
                }
            }
        }

        else -> {
            // Fallback - shouldn't happen but handle gracefully
            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
