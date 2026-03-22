package com.example.astrolume.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.model.isVideo
import com.example.astrolume.ui.components.CelestisRangePicker
import com.example.astrolume.ui.components.ShimmerApodGrid
import com.example.astrolume.ui.components.VideoPlaceholder
import com.example.astrolume.ui.utils.VideoUrlUtils
import com.example.astrolume.ui.viewModels.DiscoverUiState
import com.example.astrolume.ui.viewModels.DiscoverViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DiscoverScreen(
    uiState: DiscoverUiState,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMoreSearchResults: () -> Unit,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    windowSizeClass: WindowSizeClass,
    onOpenDrawer: () -> Unit,
    onPhotoDetailClick: (ApodResponse) -> Unit,
    hazeState: HazeState,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    var showDatePicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background layer with hazeSource (like HomeScreen pattern)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is DiscoverUiState.Loading -> {
                    // Professional loading with shimmer effect
                    val gridCols = when (windowSizeClass.widthSizeClass) {
                        WindowWidthSizeClass.Compact -> 2
                        WindowWidthSizeClass.Medium -> 3
                        else -> 4
                    }
                    ShimmerApodGrid(columns = gridCols, itemCount = 8)
                }

                is DiscoverUiState.Error -> DiscoverScreenError(state)
                is DiscoverUiState.Success -> {
                    DiscoverScreenGrid(
                        state = state,
                        windowSizeClass = windowSizeClass,
                        onLoadMoreSearchResults = onLoadMoreSearchResults,
                        onPhotoDetailClick = onPhotoDetailClick,
                        animatedVisibilityScope = animatedVisibilityScope,
                        contentPadding = PaddingValues(top = 80.dp, bottom = 80.dp)
                    )
                }
            }
        }
        
        DiscoverSearchAppBar(
            query = searchQuery,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            isLandscape = isLandscape,
            onOpenDrawer = onOpenDrawer,
            onOpenDatePicker = { showDatePicker = true },
            hazeState = hazeState
        )
    }
    if (showDatePicker) {
        CelestisRangePicker(
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                onDateRangeSelected(start, end)
                showDatePicker = false
            }
        )
    }
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DiscoverScreenGrid(
    state: DiscoverUiState.Success,
    windowSizeClass: WindowSizeClass,
    onLoadMoreSearchResults: () -> Unit,
    onPhotoDetailClick: (ApodResponse) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    contentPadding: PaddingValues
) {

    val gridCols = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 4
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.searchQuery.isEmpty()) {
            // MODE A: Discovery Feed (Standard List)
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCols),
                contentPadding = contentPadding
            )
            {
                items(state.rangeApod, key = { it.date }) { apod ->
                    ApodCard(apod, onPhotoDetailClick, animatedVisibilityScope)
                }
            }
        } else {
            // MODE B: Search Results with Pagination
            val searchState = state.searchResults
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCols),
                contentPadding = contentPadding
            ) {
                // Show initial loading
                if (searchState.isLoading && searchState.items.isEmpty()) {
                    item(span = { GridItemSpan(gridCols) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                // Show search results
                itemsIndexed(
                    items = searchState.items,
                    key = { _, apod -> apod.date }
                ) { index, apod ->
                    ApodCard(apod, onPhotoDetailClick, animatedVisibilityScope)
                    
                    // Trigger load more when near end
                    val shouldLoadMore by remember(index, searchState.items.size, searchState.hasMore) {
                        derivedStateOf {
                            index >= searchState.items.size - 15 &&
                            searchState.hasMore && 
                            !searchState.isLoadingMore
                        }
                    }
                    
                    if (shouldLoadMore) {
                        LaunchedEffect(Unit) {
                            onLoadMoreSearchResults()
                        }
                    }
                }

                // Show loading more indicator at bottom
                if (searchState.isLoadingMore) {
                    item(span = { GridItemSpan(gridCols) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                // Show error if present
                if (searchState.error != null) {
                    item(span = { GridItemSpan(gridCols) }) {
                        Text(
                            text = "Error: ${searchState.error}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                // Show "no results" message
                if (!searchState.isLoading && searchState.items.isEmpty() && searchState.error == null) {
                    item(span = { GridItemSpan(gridCols) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No results found for \"${state.searchQuery}\"")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ApodCard(
    apod: ApodResponse,
    onPhotoDetailClick: (ApodResponse) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Card(
        onClick = { onPhotoDetailClick(apod) },
        modifier = Modifier
            .padding(1.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Check if it's a video without thumbnail
            val isVideo = apod.isVideo()
            val hasNoThumbnail = isVideo && apod.thumbnailUrl == null
            
            if (hasNoThumbnail) {
                // Use VideoPlaceholder for videos without thumbnails
                VideoPlaceholder(
                    title = apod.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .aspectRatio(1f)
                )
            } else {
                // For images or videos with thumbnails, use AsyncImage
                val imageUrl = if (isVideo) {
                    // Try to get YouTube thumbnail if it's a YouTube video
                    apod.url?.let { url ->
                        VideoUrlUtils.extractYouTubeId(url)?.let { videoId ->
                            VideoUrlUtils.getYouTubeThumbnail(videoId)
                        }
                    } ?: apod.thumbnailUrl ?: apod.url
                } else {
                    apod.url
                }

                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = apod.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .aspectRatio(1f)
                        .sharedElement(
                            rememberSharedContentState(key = "image-${apod.date}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        }
                    },
                    error = {
                        // Fallback to VideoPlaceholder if image fails to load for videos
                        if (isVideo) {
                            VideoPlaceholder(
                                title = apod.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Image Load Failed", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                )
                
                // Add play icon overlay for videos
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Dynamic Text Overlay with Scrim
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 0f // Start gradient at the very top of the box for smooth fade
                        )
                    )
                    .padding(8.dp)
            ) {
                Column {
                    apod.title?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = apod.date,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverSearchAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLandscape: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenDatePicker: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    // A unified header that uses haze for a glassmorphism effect (premium feel)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color(0xFF111111).copy(alpha = 0.85f),
                    blurRadius = 20.dp,
                    tint = HazeTint(Color.White.copy(alpha = 0.05f)),
                )
            )
            .drawBehind {
                val strokeWidthPx = 1.dp.toPx()
                val verticalOffset = size.height - strokeWidthPx / 2
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, verticalOffset),
                    end = Offset(size.width, verticalOffset),
                    strokeWidth = strokeWidthPx
                )
            },
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu Icon (Landscape)
            if (isLandscape) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Sleek, embedded Search Field
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.50f)),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { 
                    focusManager.clearFocus()
                    onSearch()
                }),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search APODs...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            }
                            innerTextField()
                        }
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onQueryChange("")
                                    onSearch() // Trigger reset
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )

            // Calendar Action
            IconButton(onClick = onOpenDatePicker) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun DiscoverScreenError(state: DiscoverUiState.Error) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        //implement retry button
//        Button() {
//            Text("Retry")
//        }
    }
}
