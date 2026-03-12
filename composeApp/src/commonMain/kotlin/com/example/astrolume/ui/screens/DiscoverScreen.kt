package com.example.astrolume.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
                    onSearch = { viewModel::executeSearch.invoke() },
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
                DiscoverScreenGrid(state = state,
                    windowSizeClass = windowSizeClass,
                    onLoadMore = viewModel::loadNextSearchPage,
                    onFavoriteClick = viewModel::toggleFavorite
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
    onLoadMore: () -> Unit,
    onFavoriteClick: (ApodResponse) -> Unit
) {
    var active by remember { mutableStateOf(false) }

    // Determine which list to display
    val displayList = if (state.searchQuery.isEmpty()) state.rangeApod else state.searchResults
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    var gridCols by remember { mutableStateOf(2)}

    if (isLandscape) {
        gridCols = 3
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCols),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            itemsIndexed(
                items = displayList,
                key = { _, apod -> apod.date },
                contentType = { _, _ -> "ApodCard" }
            ) { index, apod ->
                // Pagination Trigger: When the user is 4 items from the end
                if (index >= displayList.size - 4 && state.searchQuery.isNotEmpty()) {
                    LaunchedEffect(Unit) { onLoadMore() }
                }
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
                        }

                        // The Favorite Button
                        IconButton(
                            onClick = { onFavoriteClick(apod) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
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

            // Show a small loader at the bottom during paging
            if (!state.searchQuery.isNullOrEmpty() && displayList.isNotEmpty() && state.isPaging) {
                item(span = { GridItemSpan(2) }) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
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