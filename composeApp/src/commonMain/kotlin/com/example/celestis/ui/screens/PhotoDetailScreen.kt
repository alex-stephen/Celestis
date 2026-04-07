package com.example.celestis.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.celestis.model.isVideo
import com.example.celestis.ui.components.CelestisVideoPlayer
import com.example.celestis.ui.components.HdImagePopup
import com.example.celestis.ui.components.LoadingOverlay
import com.example.celestis.ui.navigation.ApodTopAppBar
import com.example.celestis.ui.navigation.TopBarState
import com.example.celestis.ui.utils.extractDominantColor
import com.example.celestis.ui.viewModels.PhotoDetailUiState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PhotoDetailScreen(
    date: String,
    state: PhotoDetailUiState,
    onFavoriteClick: () -> Unit,
    onHideHdImage: () -> Unit,
    onShowHdImage: (String?, String?) -> Unit,
    onShare: () -> Unit,
    onLoadApodByDate: (String) -> Unit,
    windowSizeClass: WindowSizeClass,
    onNavigateBack: () -> Unit,
    hazeState: HazeState,
    animatedVisibilityScope: AnimatedVisibilityScope,
    topBarState: TopBarState
) {

    LaunchedEffect(date) {
        onLoadApodByDate(date)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = state) {
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
                    val isLandscape = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
                    val shouldHideTopBar = state.selectedHdUrl != null
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = hazeState)
                    ) {
                        PhotoDetailContent(
                            state = state,
                            onImageClick = {
                                state.apod.urlHD?.let { hdUrl ->
                                    state.apod.url?.let { url ->
                                        onShowHdImage(hdUrl, url)
                                    }
                                }
                            },
                            onFavoriteClick = onFavoriteClick,
                            onHideHdImage = onHideHdImage,
                            hazeState = hazeState,
                            onNavigateBack = onNavigateBack,
                            onShare = onShare,
                            windowSizeClass = windowSizeClass,
                            animatedVisibilityScope = animatedVisibilityScope,
                            topBarState = topBarState
                        )
                    }
                    
                    // Top App Bar overlay - hide in landscape when HD popup is open, or when scrolling
                    if (!shouldHideTopBar) {
                        AnimatedVisibility(
                            visible = topBarState.isVisible,
                            enter = slideInVertically { -it },
                            exit = slideOutVertically { -it }
                        ) {
                            ApodTopAppBar(
                                titleContent = {
                                    Text(
                                        text = "PHOTO DETAIL",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        letterSpacing = 2.sp
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
                                hazeState = hazeState,
                                windowSizeClass = windowSizeClass
                            )
                        }
                    }
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PhotoDetailContent(
    state: PhotoDetailUiState.Success,
    onImageClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    hazeState: HazeState,
    onHideHdImage: () -> Unit,
    onNavigateBack: () -> Unit,
    onShare: () -> Unit,
    windowSizeClass: WindowSizeClass,
    animatedVisibilityScope: AnimatedVisibilityScope,
    topBarState: TopBarState
) {
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val appBarContentHeight = if (isLandscape) 42.dp else 65.dp

    val apod = state.apod
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var dominantColor by remember(apod.date) { mutableStateOf(Color.Transparent) }
    
    // Animate the color for smooth fade-in effect
    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        label = "dominantColorAnimation"
    )

    val density = LocalDensity.current
    // Calculate pixel offsets for the "Glow Zone"
    val imageBottomPx = with(density) { 400.dp.toPx() }
    val glowDepthPx = with(density) { 600.dp.toPx() } // How far the light travels

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topBarState.nestedScrollConnection) // Top bar visibility logic
                .verticalScroll(scrollState)
        ) {
            // Add top padding for the AppBar ContentPadding
            Spacer(modifier = Modifier.height(statusBarTop + appBarContentHeight))

            // Check if media is video
            if (apod.isVideo()) {
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
                        },
                        onSuccess = { successState ->
                            // Extract dominant color when image loads successfully
                            coroutineScope.launch {
                                try {
                                    val image = successState.result.image
                                    val extractedColor = extractDominantColor(image)
                                    extractedColor?.let { color ->
                                        dominantColor = color
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
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
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(color = Color.Black)

                            // The "Eased" Gradient - avoid the "Blob" by adding mid-points
                            val easedGradient = Brush.verticalGradient(
                                0.0f to animatedDominantColor.copy(alpha = 0.5f), // Start at bottom of image
                                0.3f to animatedDominantColor.copy(alpha = 0.2f),
                                0.6f to animatedDominantColor.copy(alpha = 0.05f),
                                1.0f to Color.Transparent,
                                startY = imageBottomPx,
                                endY = imageBottomPx + glowDepthPx
                            )

                            drawRect(brush = easedGradient)
                        }
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
                            onFavoriteClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onFavoriteClick()
                            },
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
        }

        // HD Image Popup
        if (state.selectedHdUrl != null) {
            HdImagePopup(
                imageUrl = state.selectedHdUrl,
                onDismiss = onHideHdImage
            )
        }
}
