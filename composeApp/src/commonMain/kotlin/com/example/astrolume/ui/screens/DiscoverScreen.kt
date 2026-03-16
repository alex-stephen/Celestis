package com.example.astrolume.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.ui.components.ShimmerApodGrid
import com.example.astrolume.ui.navigation.ApodTopAppBar
import com.example.astrolume.ui.viewModels.DiscoverUiState
import com.example.astrolume.ui.viewModels.DiscoverViewModel
import dev.chrisbanes.haze.HazeState
import kotlin.time.Clock

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
        ApodTopAppBar(
            titleContent = {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center // This forces vertical centering
                ) {
                    SearchBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = query,
                                onQueryChange = viewModel::updateQuery,
                                onSearch = { viewModel.executeSearch() },
                                expanded = false, // Keep false so we can see the grid below
                                onExpandedChange = { },
                                placeholder = { Text("Search APODs...") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateQuery("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                        },
                        expanded = false,
                        onExpandedChange = { },
                        shape = RoundedCornerShape(24.dp),
                        colors = SearchBarDefaults.colors(containerColor = Color.White.copy(0.1f))
                    ) {
                        // Leave empty
                    }
                }
            },
            hazeState = hazeState,
            navigationIcon = {
                // Only show Menu in TopBar if Landscape
                if (isLandscape) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                    }
                }
            },
            actions = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, "Select Date", tint = Color.White)
                }
            }
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
        ApodDateRangePickerDialog(
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
        elevation = CardDefaults.cardElevation(4.dp)
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
fun ApodDateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        // Start in year/month picker mode for easier navigation
        initialDisplayMode = DisplayMode.Input,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Prevent selecting future dates beyond current time
                val now = Clock.System.now().toEpochMilliseconds()
                return utcTimeMillis <= now
            }

            override fun isSelectableYear(year: Int): Boolean {
                // NASA APOD started on June 16, 1995
                return year in 1995..2026
            }
        }
    )

    val confirmEnabled by remember {
        derivedStateOf { dateRangePickerState.selectedStartDateMillis != null }
    }

    // Validate 31-day constraint
    val isRangeValid by remember {
        derivedStateOf {
            val start = dateRangePickerState.selectedStartDateMillis
            val end = dateRangePickerState.selectedEndDateMillis
            
            // If only start is selected or end is null, range is valid
            if (start == null || end == null) {
                true
            } else if (start == end) {
                // Same date clicked twice - treat as single day (inclusive)
                true
            } else {
                // Check 31-day limit
                val diffMillis = end - start
                val daysDiff = diffMillis / (24 * 60 * 60 * 1000)
                daysDiff <= 31
            }
        }
    }

    // Build helpful headline message
    val headlineMessage by remember {
        derivedStateOf {
            val start = dateRangePickerState.selectedStartDateMillis
            val end = dateRangePickerState.selectedEndDateMillis
            
            when {
                start != null && end != null -> {
                    if (start == end) {
                        "Single day selected"
                    } else {
                        val daysDiff = (end - start) / (24 * 60 * 60 * 1000)
                        if (daysDiff > 31) {
                            "Range too long: $daysDiff days (max 31)"
                        } else {
                            "$daysDiff days selected"
                        }
                    }
                }
                start != null -> "Select end date (max 31 days from start)"
                else -> "Select start date"
            }
        }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    
                    // Handle same-date selection: treat as inclusive single day
                    if (startMillis != null && endMillis == null) {
                        // User only selected start date - use same date as end
                        onConfirm(startMillis, startMillis)
                    } else {
                        onConfirm(startMillis, endMillis)
                    }
                },
                enabled = confirmEnabled && isRangeValid
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = { 
                Text(
                    "Select Date Range (1995-2026)",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                ) 
            },
            headline = {
                Text(
                    text = headlineMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRangeValid) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.error
                )
            },
            showModeToggle = true // Enable year/month quick selection
        )
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
