package com.example.astrolume.ui.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.ui.viewModels.DiscoverUiState
import com.example.astrolume.ui.viewModels.DiscoverViewModel

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    windowSizeClass: WindowSizeClass
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is DiscoverUiState.Loading -> {
                DiscoverScreenLoading()
            }

            is DiscoverUiState.Error -> {
                DiscoverScreenError(state)
            }

            is DiscoverUiState.Success -> {
                DiscoverScreenSuccess(
                    state = state,
                    windowSizeClass = windowSizeClass,
                    onQueryChange = viewModel::updateQuery,
                    onLoadMore = viewModel::loadNextSearchPage,
                    onSearch = viewModel::executeSearch,
                    onFavoriteClick = { apod -> viewModel.toggleFavorite(apod) }
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
fun DiscoverScreenSuccess(
    state: DiscoverUiState.Success,
    windowSizeClass: WindowSizeClass,
    onQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onSearch: () -> Unit,
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
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            inputField = {
                SearchBarDefaults.InputField(
                    query = state.searchQuery,
                    onQueryChange = onQueryChange,
                    onSearch = { onSearch() },
                    expanded = false, // Keep false so we can see the grid below
                    onExpandedChange = { },
                    placeholder = { Text("Search APODs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCols),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            itemsIndexed(
                items = displayList,
                key = { _, apod -> apod.date }
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