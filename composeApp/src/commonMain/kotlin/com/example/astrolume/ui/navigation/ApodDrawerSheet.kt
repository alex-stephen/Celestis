package com.example.astrolume.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch

@Composable
fun ApodDrawerSheet(
    navController: NavHostController,
    drawerState: DrawerState,
    hazeState: HazeState // Pass the background haze state
) {
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Custom Glass Sheet
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .clip(RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color(0xFF111111).copy(alpha = 0.6f),
                    blurRadius = 40.dp,
                    noiseFactor = 0.1f,
                    tint = null
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(Color.White.copy(0.15f), Color.Transparent)),
                shape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Astrolume",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp, top = 16.dp)
            )

            val items = listOf(
                Triple(Screen.Home, Icons.Default.Home, "Home"),
                Triple(Screen.Search, Icons.Default.Search, "Search"),
                Triple(Screen.Favorites, Icons.Default.Favorite, "Saved")
            )

            items.forEach { (screen, icon, label) ->
                val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(screen::class) } == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color.White.copy(0.1f) else Color.Transparent)
                        .clickable {
                            scope.launch { drawerState.close() }
                            navController.navigate(screen) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.Cyan else Color.White
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = label,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}