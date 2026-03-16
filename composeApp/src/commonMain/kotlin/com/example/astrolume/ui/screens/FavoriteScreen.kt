package com.example.astrolume.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.ui.navigation.ApodTopAppBar
import com.example.astrolume.ui.viewModels.FavoriteUiState
import com.example.astrolume.ui.viewModels.FavoriteViewModel
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    viewModel: FavoriteViewModel,
    windowSizeClass: WindowSizeClass,
    onOpenDrawer: () -> Unit,
    onPhotoDetailClick: (ApodResponse) -> Unit,
    hazeState: HazeState
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        ApodTopAppBar(
            titleContent = {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FAVORITES",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
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
                IconButton(onClick = { /* Show DatePicker Dialog */ }) {
                    Icon(Icons.Default.CalendarToday, "Select Date", tint = Color.White)
                }
            }
        )
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
                    onFavoriteClick = viewModel::toggleFavorite,
                    onPhotoDetailClick = onPhotoDetailClick
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
    onFavoriteClick: (ApodResponse) -> Unit,
    onPhotoDetailClick: (ApodResponse) -> Unit
) {

    val gridCols = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 4
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
                items = state.favorites,
                // Date is a good key, but ensure it's unique!
                key = { it.date }
            ) { apod ->
                FavoriteCard(
                    apod = apod,
                    onFavoriteClick = { onFavoriteClick(apod) },
                    onPhotoDetailClick = { onPhotoDetailClick(apod) }
                )
            }
        }
    }
}

@Composable
fun FavoriteCard(
    apod: ApodResponse,
    onFavoriteClick: () -> Unit,
    onPhotoDetailClick: () -> Unit
) {
    Card(
        onClick = onPhotoDetailClick,
        modifier = Modifier
            .padding(4.dp)
            .border(1.dp, Color.White.copy(alpha = 0.2f), MaterialTheme.shapes.medium),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = if (apod.mediaType == "video") apod.thumbnailUrl else apod.url,
                contentDescription = apod.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
            ) {
                // Ensure this icon redraws when 'apod' changes
                Icon(
                    imageVector = if (apod.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (apod.isFavorite) Color.Red else Color.White,
                )
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