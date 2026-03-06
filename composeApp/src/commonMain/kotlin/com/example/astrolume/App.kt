package com.example.astrolume

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.astrolume.ui.navigation.ApodBottomBar
import com.example.astrolume.ui.navigation.NavGraph

@Composable
fun App() {
    val navController = rememberNavController()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            bottomBar = { ApodBottomBar(navController) }
        ) { innerPadding ->
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}