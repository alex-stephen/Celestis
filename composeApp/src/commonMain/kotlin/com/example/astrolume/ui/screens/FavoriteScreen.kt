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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
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
import com.example.astrolume.ui.viewModels.FavoriteUiState
import com.example.astrolume.ui.viewModels.FavoriteViewModel

@Composable
fun FavoriteScreen(
    viewModel: FavoriteViewModel,
    windowSizeClass: WindowSizeClass,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is FavoriteUiState.Loading -> {
                FavoriteScreenLoading()
            }

            is FavoriteUiState.Error -> {
                FavoriteScreenError(state)
            }

            is FavoriteUiState.Success -> {
                FavoriteScreenSuccess(
                    state = state,
                    windowSizeClass = windowSizeClass,
                    onFavoriteClick = viewModel::toggleFavorite
                )
            }
        }
    }
}

@Composable
fun FavoriteScreenLoading() {

}

@Composable
fun FavoriteScreenSuccess(
    state: FavoriteUiState.Success,
    windowSizeClass: WindowSizeClass,
    onFavoriteClick: (ApodResponse) -> Unit
) {
    val displayList = state.favorites

    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    var gridCols by remember { mutableStateOf(2)}

    if (isLandscape) {
        gridCols = 3
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCols),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(
                items = displayList,
                key = { apod -> apod.date }
            ) { apod ->
                Card(
                    modifier = Modifier
                        .padding(4.dp)
                        .border(1.dp, Color.White, MaterialTheme.shapes.medium),
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
        }
    }
}

@Composable
fun FavoriteScreenError(state: FavoriteUiState.Error) {
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