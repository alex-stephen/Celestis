package com.example.astrolume.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.example.astrolume.model.isVideo
import com.example.astrolume.ui.components.CelestisVideoPlayer
import com.example.astrolume.ui.components.HdImagePopup
import com.example.astrolume.ui.components.LoadingOverlay
import com.example.astrolume.ui.navigation.ApodTopAppBar
import com.example.astrolume.ui.viewModels.PhotoDetailUiState
import com.example.astrolume.ui.viewModels.PhotoDetailViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PhotoDetailScreen(
    date: String,
    viewModel: PhotoDetailViewModel,
    windowSizeClass: WindowSizeClass,
    onNavigateBack: () -> Unit,
    onShare: () -> Unit = {},
    hazeState: HazeState,
    animatedVisibilityScope: AnimatedVisibilityScope
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
                    IconButton(onClick = { viewModel.shareApod() }) {
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
                                    state.apod.urlHD,
                                    state.apod.url
                                )
                            },
                            onFavoriteClick = viewModel::toggleFavorite,
                            onHideHdImage = viewModel::hideHdImage,
                            hazeState = hazeState,
                            onNavigateBack = onNavigateBack,
                            animatedVisibilityScope = animatedVisibilityScope
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PhotoDetailContent(
    state: PhotoDetailUiState.Success,
    onImageClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    hazeState: HazeState,
    onHideHdImage: () -> Unit,
    onNavigateBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val apod = state.apod
    val scrollState = rememberScrollState()
    
    // Pull-to-dismiss state
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val dismissThreshold = 200f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .graphicsLayer {
                alpha = 1f - (offsetY / 1000f).coerceIn(0f, 0.5f)
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        if (offsetY > dismissThreshold) {
                            onNavigateBack()
                        } else {
                            offsetY = 0f
                        }
                        isDragging = false
                    },
                    onDragCancel = {
                        offsetY = 0f
                        isDragging = false
                    },
                    onVerticalDrag = { _, dragAmount ->
                        // Only allow downward drag
                        if (scrollState.value == 0 && dragAmount > 0) {
                            offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
                .verticalScroll(scrollState, enabled = !isDragging)
        ) {
            // Check if media is video
            if (apod.isVideo()) {
                // Video Player - embedded with full-screen support
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    CelestisVideoPlayer(
                        videoUrl = apod.url ?: "",
                        modifier = Modifier.fillMaxSize(),
                        onError = { error ->
                            // Handle video error - could show error state in UI
                            println("Video playback error: $error")
                        }
                    )
                }
            } else {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedElement(
                                rememberSharedContentState(key = "image-${apod.date}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ),
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
            }

            // Metadata Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    AnimatedFavoriteButton(
                        onFavoriteClick = onFavoriteClick,
                        isFavorite = apod.isFavorite
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp), // Fine-tune internal balance
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineBreak = LineBreak.Paragraph,
                        hyphens = Hyphens.Auto,
                        lineHeight = 24.sp,
                        letterSpacing = 0.5.sp
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
