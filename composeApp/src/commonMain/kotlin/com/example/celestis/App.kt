package com.example.celestis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.example.celestis.ui.navigation.AdaptiveNavigationWrapper
import com.example.celestis.ui.navigation.NavGraph
import com.example.celestis.ui.navigation.rememberWindowSizeClass
import com.example.celestis.ui.theme.CelestisTheme
import com.example.celestis.ui.viewModels.HomeViewModel
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App(
    initialDeepLinkDate: String? = null
) {
    val navController = rememberNavController()
    val windowSizeClass = rememberWindowSizeClass()
    val widthClass = windowSizeClass.widthSizeClass
    val hazeState = remember { HazeState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Get HomeViewModel to call onAppResume when app comes to foreground
    val homeViewModel: HomeViewModel = koinViewModel()
    
    // Lifecycle observer to refresh APOD when app resumes
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.onAppResume()
            }
        }
        
        lifecycle.addObserver(lifecycleObserver)
        
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    // Handle deep link navigation
    LaunchedEffect(initialDeepLinkDate) {
        initialDeepLinkDate?.let { date ->
            navController.navigate(com.example.celestis.ui.navigation.Screen.PhotoDetail(date))
        }
    }

    // Auto-close drawer when switching to Compact (Portrait)
    LaunchedEffect(widthClass) {
        if (widthClass == WindowWidthSizeClass.Compact && drawerState.isOpen) {
            drawerState.close()
        }
    }

    CelestisTheme {
        AdaptiveNavigationWrapper(
            navController = navController,
            windowSizeClass = windowSizeClass,
            hazeState = hazeState,
            drawerState = drawerState
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    NavGraph(
                        navController = navController,
                        windowSizeClass = windowSizeClass,
                        modifier = Modifier.fillMaxSize(),
                        onOpenDrawer = {
                            // Only allow opening if NOT in portrait
                            if (widthClass != WindowWidthSizeClass.Compact) {
                                scope.launch { drawerState.open() }
                            }
                        },
                        hazeState = hazeState,
                        bottomPadding = padding.calculateBottomPadding()
                    )
                }
            }
        }
    }
}
