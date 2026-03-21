package com.example.astrolume.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun ApodBottomNavBar(navController: NavHostController, isEnabled: Boolean) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        val items = listOf(
            Triple(Screen.Home, Icons.Default.Home, "Home"),
            Triple(Screen.Discover, Icons.Default.TravelExplore, "Discover"),
            Triple(Screen.Favorites, Icons.Default.Favorite, "Saved")
        )

        items.forEach { (screen, icon, label) ->
            val isInHierarchy = currentDestination?.hierarchy?.any { it.hasRoute(screen::class) } == true
            
            NavigationBarItem(
                selected = isInHierarchy,
                enabled = isEnabled,
                onClick = {
                    if (isEnabled) {
                        if (isInHierarchy) {
                            // Already in this section's hierarchy - pop back to root
                            navController.popBackStack(route = screen, inclusive = false)
                        } else {
                            // Navigate to new section
                            navController.navigate(screen) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
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