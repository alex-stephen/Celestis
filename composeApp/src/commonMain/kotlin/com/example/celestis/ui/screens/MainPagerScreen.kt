package com.example.celestis.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.celestis.model.ApodResponse
import com.example.celestis.ui.navigation.ApodBottomNavBar
import com.example.celestis.ui.navigation.BottomBarState
import com.example.celestis.ui.navigation.NavItem
import com.example.celestis.ui.viewModels.DiscoverUiState
import com.example.celestis.ui.viewModels.DiscoverViewModel
import com.example.celestis.ui.viewModels.FavoriteUiState
import com.example.celestis.ui.viewModels.FavoriteViewModel
import com.example.celestis.ui.viewModels.HomeViewModel
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainPagerScreen(
    windowSizeClass: WindowSizeClass,
    hazeState: HazeState,
    onNavigateToDetail: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val pagerState = rememberPagerState(pageCount = { NavItem.entries.size })
    val scope = rememberCoroutineScope()

    // Track the bottom bar visibility state
    val bottomBarState = remember { BottomBarState() }

    val customBottomBarHeight = 70.dp

    // ViewModels for each screen
    val homeViewModel: HomeViewModel = koinViewModel()
    val discoverViewModel: DiscoverViewModel = koinViewModel()
    val favoriteViewModel: FavoriteViewModel = koinViewModel()

    // Collect UI states
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val isShowingRandom by homeViewModel.isShowingRandom.collectAsStateWithLifecycle()
    val isFetchingRandom by homeViewModel.isRefilling.collectAsStateWithLifecycle()
    val isImageLoading by homeViewModel.isImageLoading.collectAsStateWithLifecycle()

    val discoverUiState by discoverViewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by discoverViewModel.searchQuery.collectAsStateWithLifecycle()

    val favoriteUiState by favoriteViewModel.uiState.collectAsStateWithLifecycle()

    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    // Use a simple Box layout for full control over sizing and positioning
    Box(modifier = Modifier.fillMaxSize()) {
        // Content area with HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(bottomBarState.nestedScrollConnection),
            // Pre-load adjacent pages for instant switching
            beyondViewportPageCount = 1,
            // Enable user scrolling for swipe gestures
            userScrollEnabled = true
        ) { pageIndex ->
            when (NavItem.entries[pageIndex]) {
                NavItem.Home -> HomeScreen(
                    uiState = homeUiState,
                    isShowingRandom = isShowingRandom,
                    isFetchingRandom = isFetchingRandom,
                    isImageLoading = isImageLoading,
                    onShare = homeViewModel::shareApod,
                    onRefresh = homeViewModel::showNextRandom,
                    onBackToToday = homeViewModel::showToday,
                    onFavoriteToggle = homeViewModel::toggleFavorite,
                    onShowHdImage = homeViewModel::showHdImage,
                    onHideHdImage = homeViewModel::hideHdImage,
                    windowSizeClass = windowSizeClass,
                    onOpenDrawer = onOpenDrawer,
                    hazeState = hazeState,
                    bottomPadding = if (isCompact) customBottomBarHeight else 0.dp
                )

                NavItem.Discover -> DiscoverScreenWrapper(
                    uiState = discoverUiState,
                    searchQuery = searchQuery,
                    onQueryChange = discoverViewModel::updateQuery,
                    onSearch = discoverViewModel::executeSearch,
                    onLoadMoreSearchResults = discoverViewModel::loadMoreSearchResults,
                    onDateRangeSelected = discoverViewModel::onDateRangeSelected,
                    windowSizeClass = windowSizeClass,
                    onOpenDrawer = onOpenDrawer,
                    onPhotoDetailClick = {
                        onNavigateToDetail(it.date)
                    },
                    hazeState = hazeState
                )

                NavItem.Favorites -> FavoriteScreenWrapper(
                    uiState = favoriteUiState,
                    windowSizeClass = windowSizeClass,
                    onOpenDrawer = onOpenDrawer,
                    onPhotoDetailClick = {
                        onNavigateToDetail(it.date)
                    },
                    hazeState = hazeState
                )
            }
        }

        // Bottom bar positioned at the bottom with custom sizing
        if (isCompact) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                AnimatedVisibility(
                    visible = bottomBarState.isVisible,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    // Let the bottom bar control its own height
                    ApodBottomNavBar(
                        selectedIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        hazeState = hazeState
                    )
                }
            }
        }
    }
}

/**
 * Wrapper for DiscoverScreen that provides SharedTransitionLayout and AnimatedVisibility scope
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DiscoverScreenWrapper(
    uiState: DiscoverUiState,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMoreSearchResults: () -> Unit,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    windowSizeClass: WindowSizeClass,
    onOpenDrawer: () -> Unit,
    onPhotoDetailClick: (ApodResponse) -> Unit,
    hazeState: HazeState
) {
    SharedTransitionLayout {
        AnimatedVisibility(visible = true) {
            DiscoverScreen(
                uiState = uiState,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onLoadMoreSearchResults = onLoadMoreSearchResults,
                onDateRangeSelected = onDateRangeSelected,
                windowSizeClass = windowSizeClass,
                onOpenDrawer = onOpenDrawer,
                onPhotoDetailClick = onPhotoDetailClick,
                hazeState = hazeState,
                animatedVisibilityScope = this
            )
        }
    }
}

/**
 * Wrapper for FavoriteScreen that provides SharedTransitionLayout and AnimatedVisibility scope
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FavoriteScreenWrapper(
    uiState: FavoriteUiState,
    windowSizeClass: WindowSizeClass,
    onOpenDrawer: () -> Unit,
    onPhotoDetailClick: (ApodResponse) -> Unit,
    hazeState: HazeState
) {
    SharedTransitionLayout {
        AnimatedVisibility(visible = true) {
            FavoriteScreen(
                uiState = uiState,
                windowSizeClass = windowSizeClass,
                onOpenDrawer = onOpenDrawer,
                onPhotoDetailClick = onPhotoDetailClick,
                hazeState = hazeState,
                animatedVisibilityScope = this
            )
        }
    }
}
