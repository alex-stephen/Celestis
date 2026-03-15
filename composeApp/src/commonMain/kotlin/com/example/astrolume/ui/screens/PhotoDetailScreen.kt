package com.example.astrolume.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.example.astrolume.ui.components.HdImagePopup
import com.example.astrolume.ui.navigation.ApodTopAppBar
import com.example.astrolume.ui.viewModels.PhotoDetailUiState
import com.example.astrolume.ui.viewModels.PhotoDetailViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

@Composable
fun PhotoDetailScreen(
    date: String,
    viewModel: PhotoDetailViewModel,
    windowSizeClass: WindowSizeClass,
    onNavigateBack: () -> Unit,
    onShare: () -> Unit = {},
    hazeState: HazeState
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(date) {
        viewModel.loadApodByDate(date)
    }

    Scaffold(
        topBar = {
            ApodTopAppBar(
                titleContent = {
                    Text(
                        text = "Photo Detail",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PhotoDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PhotoDetailUiState.Success -> {
                    PhotoDetailContent(
                        state = state,
                        onImageClick = { viewModel.showHdImage(state.apod.urlHD ?: state.apod.url) },
                        onFavoriteClick = viewModel::toggleFavorite,
                        onHideHdImage = viewModel::hideHdImage,
                        hazeState = hazeState
                    )
                }
                is PhotoDetailUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoDetailContent(
    state: PhotoDetailUiState.Success,
    onImageClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onHideHdImage: () -> Unit,
    hazeState: HazeState
) {
    val apod = state.apod
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header Image - 50% of viewport height, clickable for HD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // Approximately 50% of typical viewport
                    .clickable(onClick = onImageClick)
            ) {
                SubcomposeAsyncImage(
                    model = apod.url,
                    contentDescription = apod.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                )
            }

            // Metadata Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Date pill
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = apod.date,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = apod.title ?: "Unknown Title",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Explanation
                Text(
                    text = apod.explanation ?: "No description available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Copyright
                apod.copyright?.let { copyright ->
                    Text(
                        text = "© $copyright",
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                // Add some bottom padding so FAB doesn't overlap content
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Favorite Button - Prominent on the page
        FloatingActionButton(
            onClick = onFavoriteClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .hazeChild(state = hazeState, style = HazeStyle(backgroundColor = Color(0xFF111111), blurRadius = 30.dp, tint = null)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (apod.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (apod.isFavorite) "Remove from favorites" else "Add to favorites",
                    modifier = Modifier.size(24.dp),
                    tint = if (apod.isFavorite) Color.Red else Color.White
                )
            }
        }

        // HD Image Popup
        if (state.selectedHdUrl != null) {
            HdImagePopup(
                imageUrl = state.selectedHdUrl,
                onDismiss = onHideHdImage
            )
        }
    }
}
