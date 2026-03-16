package com.example.astrolume.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun ApodNavRail(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationRail(modifier = modifier) {
        val items = listOf(
            Triple(Screen.Home, Icons.Default.Home, "Home"),
            Triple(Screen.Discover, Icons.Default.TravelExplore, "Discover"),
            Triple(Screen.Favorites, Icons.Default.Favorite, "Saved")
        )

        items.forEach { (screen, icon, label) ->
            val isInHierarchy = currentDestination?.hierarchy?.any { it.hasRoute(screen::class) } == true
            
            NavigationRailItem(
                selected = isInHierarchy,
                onClick = {
                    if (isInHierarchy) {
                        // Already in this section's hierarchy - pop back to root
                        navController.popBackStack(route = screen, inclusive = false)
                    } else {
                        // Navigate to new section
                        navController.navigate(screen) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}