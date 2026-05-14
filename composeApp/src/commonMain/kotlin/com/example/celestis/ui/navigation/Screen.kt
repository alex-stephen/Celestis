package com.example.celestis.ui.navigation

import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable data object Home : Screen
    @Serializable data object Discover : Screen
    @Serializable data object Favorites : Screen
    @Serializable data object Settings : Screen

    // Detail screen requires a parameter
    @Serializable data class PhotoDetail(val date: String) : Screen
}

// Helper to map UI properties to your routes
enum class NavItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Home(Screen.Home, "Home", androidx.compose.material.icons.Icons.Default.Home),
    Discover(Screen.Discover, "Explore", androidx.compose.material.icons.Icons.Default.TravelExplore),
    Favorites(Screen.Favorites, "Saved", androidx.compose.material.icons.Icons.Default.Favorite),
    Settings(Screen.Settings, "Settings", androidx.compose.material.icons.Icons.Default.Settings)
}
