package com.example.astrolume.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.astrolume.ui.viewModels.DiscoverUiState
import com.example.astrolume.ui.viewModels.DiscoverViewModel

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel
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
                )
            }
        }
    }
}

@Composable
fun DiscoverScreenLoading() {

}

@Composable
fun DiscoverScreenSuccess(
    state: DiscoverUiState.Success
) {
    // Local state for UI interactions (e.g., toggling an overlay)
    var isVisible by remember { mutableStateOf(true) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = state.rangeApod,
            key = { it.date } // Crucial for LazyGrid performance/reordering
        ) { apod ->
            val imageUrl = if (apod.mediaType.equals("video", ignoreCase = true)) {
                apod.thumbnailUrl ?: apod.url
            } else {
                apod.url
            }

            Box(modifier = Modifier.aspectRatio(1f)) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = apod.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun DiscoverScreenError(state: DiscoverUiState.Error) {
    Text(state.message)

}