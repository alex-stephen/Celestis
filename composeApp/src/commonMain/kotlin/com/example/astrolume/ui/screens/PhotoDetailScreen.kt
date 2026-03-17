package com.example.astrolume.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.ui.components.HdImagePopup
import com.example.astrolume.ui.components.LoadingOverlay
import com.example.astrolume.ui.navigation.ApodTopAppBar
import com.example.astrolume.ui.viewModels.PhotoDetailUiState
import com.example.astrolume.ui.viewModels.PhotoDetailViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
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
                        text = "PHOTO DETAIL",
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
        },
        floatingActionButton = {
            // Move the FAB out of the Scrollable Column and into the Scaffold
            if (uiState is PhotoDetailUiState.Success) {
                val state = uiState as PhotoDetailUiState.Success
                FavoriteActionButton(
                    apod = state.apod,
                    onClick = viewModel::toggleFavorite,
                    hazeState = hazeState
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (val state = uiState) {
                    is PhotoDetailUiState.Loading -> {
                        // Only show loading overlay after a delay to avoid flash for cached content
                        var showLoading by remember { mutableStateOf(false) }
                        
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(300) // Only show if loading takes > 300ms
                            showLoading = true
                        }
                        
                        if (showLoading) {
                            LoadingOverlay(message = "Loading Photo Details...")
                        }
                    }

                    is PhotoDetailUiState.Success -> {
                        PhotoDetailContent(
                            state = state,
                            onImageClick = {
                                viewModel.showHdImage(
                                    state.apod.urlHD ?: state.apod.url
                                )
                            },
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
}

@Composable
fun PhotoDetailContent(
    state: PhotoDetailUiState.Success,
    onImageClick: () -> Unit,
    hazeState: HazeState,
    onHideHdImage: () -> Unit,
) {
    val apod = state.apod
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
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
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading image...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineBreak = LineBreak.Paragraph,
                        hyphens = Hyphens.Auto,
                        // Note: fontStyle is already inherited from bodyLarge,
                        // but you can override it here if needed.
                        //  fontStyle = FontStyle.Normal
                    ),
                    textAlign = TextAlign.Justify,
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

        // HD Image Popup
        if (state.selectedHdUrl != null) {
            HdImagePopup(
                imageUrl = state.selectedHdUrl,
                onDismiss = onHideHdImage
            )
        }
    }
}

@Composable
fun FavoriteActionButton(
    apod: ApodResponse,
    onClick: () -> Unit,
    hazeState: HazeState,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color.White.copy(alpha = 0.15f),
                    blurRadius = 40.dp,
                    noiseFactor = 0.15f,
                    tint = null
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
        color = Color.Transparent
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedFavoriteButton(
                onFavoriteClick = onClick,
                isFavorite = apod.isFavorite,
            )
        }
    }
}
