package com.example.celestis.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun ApodBottomNavBar(
    navController: NavHostController,
    isEnabled: Boolean,
    hazeState: HazeState,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = Modifier
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color(0xFF111111).copy(alpha = 0.85f), // Slightly transparent for blur visibility
                    blurRadius = 30.dp,
                    noiseFactor = 0f,
                    tint = HazeTint.Unspecified, // Subtle tint for depth
                )
            )
            .drawBehind {
                val strokeWidthPx = 1.dp.toPx()
                val verticalOffset = strokeWidthPx / 2

                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, verticalOffset),
                    end = Offset(size.width, verticalOffset),
                    strokeWidth = strokeWidthPx
                )
            }
            .height(90.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {

        NavItem.entries.forEach { item ->
            val isInHierarchy = currentDestination?.hierarchy?.any { it.hasRoute(item.screen::class) } == true
            val isSelected = currentDestination?.hasRoute(item.screen::class) == true
            CustomNavItem(
                selected = isSelected,
                icon = item.icon,
                label = item.label,
                onClick = {
                    if (isEnabled) {
                        if (isInHierarchy) {
                            // Already in this section's hierarchy - pop back to root
                            navController.popBackStack(route = item.screen, inclusive = false)
                        } else {
                            // Navigate to new section
                            navController.navigate(item.screen) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * State holder for the Bottom Bar visibility logic.
 */
@Stable
class BottomBarState {
    var isVisible by mutableStateOf(true)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // available.y < 0 means scrolling down
            if (available.y < -10f) {
                isVisible = false
            } else if (available.y > 10f) {
                isVisible = true
            }
            return Offset.Zero
        }
    }
}