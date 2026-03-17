package com.example.astrolume.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.ui.components.CelestisRangePicker
import com.example.astrolume.ui.components.ShimmerApodGrid
import com.example.astrolume.ui.viewModels.DiscoverUiState
import com.example.astrolume.ui.viewModels.DiscoverViewModel
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    windowSizeClass: WindowSizeClass,
    onOpenDrawer: () -> Unit,
    onPhotoDetailClick: (ApodResponse) -> Unit,
    hazeState: HazeState,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        DiscoverSearchAppBar(
            query = query,
            onQueryChange = viewModel::updateQuery,
            onSearch = { viewModel.executeSearch() },
            isLandscape = isLandscape,
            onOpenDrawer = onOpenDrawer,
            onOpenDatePicker = { showDatePicker = true },
        )

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
                    viewModel = viewModel,
                    onPhotoDetailClick = onPhotoDetailClick
                )
            }
        }
    }
    if (showDatePicker) {
        CelestisRangePicker(
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                viewModel.onDateRangeSelected(start, end)
                showDatePicker = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreenGrid(
    state: DiscoverUiState.Success,
    windowSizeClass: WindowSizeClass,
    viewModel: DiscoverViewModel,
    onPhotoDetailClick: (ApodResponse) -> Unit
) {

    val gridCols = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 4
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.searchQuery.isEmpty()) {
            // MODE A: Discovery Feed (Standard List)
            LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                items(state.rangeApod, key = { it.date }) { apod ->
                    ApodCard(apod, onPhotoDetailClick)
                }
            }
        } else {
            // MODE B: Search Results with Pagination
            val searchState = state.searchResults
            
            LazyVerticalGrid(columns = GridCells.Fixed(gridCols)) {
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
                    ApodCard(apod, onPhotoDetailClick)
                    
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
                            viewModel.loadMoreSearchResults()
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

@Composable
fun ApodCard(
    apod: ApodResponse,
    photoDetialClick: (ApodResponse) -> Unit
) {
    Card(
        onClick = {photoDetialClick(apod)},
        modifier = Modifier
            .padding(4.dp)
            .border(1.dp, Color.White.copy(alpha = 0.2f), MaterialTheme.shapes.medium),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        val imageUrl = if (apod.mediaType.equals("video", ignoreCase = true)) {
            apod.thumbnailUrl ?: apod.url
        } else {
            apod.url
        }
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = apod.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f) // Fade to dark at the bottom
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Text(
                    text = apod.date, // Format this string nicely if needed
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
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
    modifier: Modifier = Modifier
) {
    // A unified header that uses haze for a glassmorphism effect (premium feel)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        color = Color.Transparent,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
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
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search APODs...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
