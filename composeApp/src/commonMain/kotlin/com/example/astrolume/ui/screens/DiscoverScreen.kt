package com.example.astrolume.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.ui.viewModels.DiscoverUiState
import com.example.astrolume.ui.viewModels.DiscoverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    windowSizeClass: WindowSizeClass
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = viewModel::updateQuery,
                    onSearch = { viewModel.executeSearch() },
                    expanded = false, // Keep false so we can see the grid below
                    onExpandedChange = { },
                    placeholder = { Text("Search APODs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
        ) {
            // Leave empty since we are using the grid below instead
        }
        when (val state = uiState) {
            is DiscoverUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is DiscoverUiState.Error -> DiscoverScreenError(state)
            is DiscoverUiState.Success -> {
                DiscoverScreenGrid(
                    state = state,
                    windowSizeClass = windowSizeClass,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
fun DiscoverScreenLoading() {

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreenGrid(
    state: DiscoverUiState.Success,
    windowSizeClass: WindowSizeClass,
    viewModel: DiscoverViewModel,
) {
    // Determine which list to display
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val gridCols = if (isLandscape) 3 else 2

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.searchQuery.isEmpty()) {
            // MODE A: Discovery Feed (Standard List)
            LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                items(state.rangeApod, key = { it.date }) { apod ->
                    ApodCard(apod, onFavoriteClick = { viewModel.toggleFavorite(it) })
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
                    ApodCard(apod, onFavoriteClick = { viewModel.toggleFavorite(it) })
                    
                    // Trigger load more when near end
                    val shouldLoadMore by remember(index, searchState.items.size, searchState.hasMore) {
                        derivedStateOf {
                            index >= searchState.items.size - 5 && 
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
    onFavoriteClick: (ApodResponse) -> Unit) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .border(1.dp, Color.White, MaterialTheme.shapes.medium ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        val imageUrl = if (apod.mediaType.equals("video", ignoreCase = true)) {
            apod.thumbnailUrl ?: apod.url
        } else {
            apod.url
        }
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = apod.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
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

                // The Favorite Button
                IconButton(
                    onClick = { onFavoriteClick(apod) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                ) {
                    Icon(
                        imageVector = if (apod.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (apod.isFavorite) Color.Red else Color.White,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApodDateRangePicker(onRangeSelected: (Long?, Long?) -> Unit) {
    // 31 days in milliseconds
    val maxRangeMillis = 31L * 24 * 60 * 60 * 1000

    val dateRangePickerState = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return true // Allow starting on any date (or restrict to past dates here)
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year <= 2026 // APOD started in 1995, max is current year
            }
        }
    )

    // Enforce the 31-day limit reactively
    LaunchedEffect(dateRangePickerState.selectedEndDateMillis) {
        val start = dateRangePickerState.selectedStartDateMillis
        val end = dateRangePickerState.selectedEndDateMillis

        if (start != null && end != null) {
            if (end - start > maxRangeMillis) {
                // If they select a range larger than 31 days, reset the end date
                dateRangePickerState.setSelection(start, null)
            } else {
                onRangeSelected(start, end)
            }
        }
    }

    DateRangePicker(
        state = dateRangePickerState,
        modifier = Modifier.fillMaxWidth().height(400.dp)
    )
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
