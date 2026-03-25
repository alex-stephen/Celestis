package com.example.celestis

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.example.celestis.ui.navigation.AdaptiveNavigationWrapper
import com.example.celestis.ui.navigation.ApodBottomNavBar
import com.example.celestis.ui.navigation.BottomBarState
import com.example.celestis.ui.navigation.NavGraph
import com.example.celestis.ui.navigation.rememberWindowSizeClass
import com.example.celestis.ui.theme.CelestisTheme
import com.example.celestis.ui.viewModels.HomeViewModel
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalComposeUiApi::class)
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

    val bottomBarState = remember { BottomBarState() }
    val isCompact = widthClass == WindowWidthSizeClass.Compact

    val animatedAlpha by animateFloatAsState(
        targetValue = if (bottomBarState.isVisible || !isCompact) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "BottomBarAlpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (bottomBarState.isVisible || !isCompact) 0f else 100f,
        animationSpec = tween(durationMillis = 200),
        label = "BottomBarOffset"
    )

    val bottomNavHeight = 70.dp // Standard NavigationBar height
    val animatedBottomPadding by animateDpAsState(
        targetValue = if (bottomBarState.isVisible && isCompact) bottomNavHeight else 0.dp,
        animationSpec = tween(300)
    )

    // Handle deep link navigation
    LaunchedEffect(initialDeepLinkDate) {
        initialDeepLinkDate?.let { date ->
            navController.navigate(com.example.celestis.ui.navigation.Screen.PhotoDetail(date))
        }
    }

    // NEW: Auto-close drawer when switching to Compact (Portrait)
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
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isCompact) Modifier.nestedScroll(bottomBarState.nestedScrollConnection)
                            else Modifier
                        )
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
                            bottomPadding = animatedBottomPadding
                        )
                    }
                }
                if (isCompact) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter) // Align to the very bottom
                            .offset { IntOffset(0, animatedOffset.roundToInt()) }
                            .alpha(animatedAlpha),
                        color = Color.Transparent, // CHANGE: Make this transparent
                        tonalElevation = 0.dp
                    ) {
                        ApodBottomNavBar(
                            navController = navController,
                            isEnabled = bottomBarState.isVisible,
                            hazeState = hazeState,
                        )
                    }
                }
            }
        }
    }
}
