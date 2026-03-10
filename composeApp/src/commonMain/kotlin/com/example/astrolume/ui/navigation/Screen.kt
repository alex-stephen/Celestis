package com.example.astrolume.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable object Home : Screen
    @Serializable object Discover : Screen
    @Serializable object Favorites : Screen
}