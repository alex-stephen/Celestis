package com.alexstephen.celestis80085.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.alexstephen.celestis80085.model.isVideo
import com.alexstephen.celestis80085.ui.components.CelestisVideoPlayer
import com.alexstephen.celestis80085.ui.components.HdImagePopup
import com.alexstephen.celestis80085.ui.navigation.ApodTopAppBarHorizontalPadding
import com.alexstephen.celestis80085.ui.navigation.ApodTopAppBar
import com.alexstephen.celestis80085.ui.navigation.apodNavigationOverlayWidth
import com.alexstephen.celestis80085.ui.navigation.apodTopAppBarContentHeight
import com.alexstephen.celestis80085.ui.viewModels.HomeUiState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    isShowingRandom: Boolean,
    isImageLoading: Boolean,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
    onBackToToday: () -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit,
    onShowHdImage: (String?, String?) -> Unit,
    onHideHdImage: () -> Unit,
    onImageLoaded: () -> Unit = {},
    windowSizeClass: WindowSizeClass,
    hazeState: HazeState,
    bottomPadding: Dp = 0.dp,
    isVideoSource: Boolean = false
) {
    // No Scaffold TopBar slot = No unwanted gaps
    Scaffold(
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is HomeUiState.Success -> {
                    HomeScreenSuccess(
                        state = state,
                        windowSizeClass = windowSizeClass,
                        isShowingRandom = isShowingRandom,
                        isImageLoading = isImageLoading,
                        onShare = onShare,
                        onRefresh = onRefresh,
                        onBackToToday = onBackToToday,
                        onFavoriteToggle = onFavoriteToggle,
                        hazeState = hazeState,
                        onShowHdImage = onShowHdImage,
                        onHideHdImage = onHideHdImage,
                        onImageLoaded = onImageLoaded,
                        bottomPadding = bottomPadding,
                        isVideoSource = isVideoSource
                    )
                }

                is HomeUiState.Loading -> HomeScreenLoading()
                is HomeUiState.Error -> HomeScreenError(
                    state,
                    onRefresh = onRefresh
                )
            }
        }
    }
}

@Composable
fun HomeScreenSuccess(
    state: HomeUiState.Success,
    windowSizeClass: WindowSizeClass,
    isShowingRandom: Boolean,
    isImageLoading: Boolean,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
    onBackToToday: () -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit,
    hazeState: HazeState,
    onShowHdImage: (String?, String?) -> Unit,
    onHideHdImage: () -> Unit,
    onImageLoaded: () -> Unit,
    bottomPadding: Dp,
    isVideoSource: Boolean = false
) {
    val displayApod = if (isShowingRandom) state.randomApod ?: state.todayApod else state.todayApod
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val isPhoneLandscape = isLandscape &&
            windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    val isVerticalPhone = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact &&
            windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact
    val shouldShowGlassSheet = !isPhoneLandscape
    val shouldShowInlineArrow = shouldShowGlassSheet && !isVerticalPhone
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val appBarContentHeight = apodTopAppBarContentHeight(windowSizeClass)
    val homeControlsTopPadding = appBarContentHeight + ApodTopAppBarHorizontalPadding
    val navigationOverlayWidth = apodNavigationOverlayWidth(windowSizeClass)

    var currentApod by remember { mutableStateOf(displayApod) }
    var isVideoPlaying by remember(currentApod.date) { mutableStateOf(false) }
    // Smooth transition coordination
    LaunchedEffect(displayApod.date) {
        if (currentApod.date != displayApod.date) {
            currentApod = displayApod
        }
    }

    // Core Scrolling Mechanism
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val isTabletLayout = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
        val isFoldableLandscape = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                maxWidth >= maxHeight &&
                maxHeight < 700.dp
        val isTabletLandscape = isTabletLayout && !isFoldableLandscape && maxWidth >= maxHeight &&
                windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact
        val isFoldablePortrait = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium &&
                maxHeight > maxWidth
        val isTabletPortrait = isTabletLayout && !isFoldablePortrait && maxHeight > maxWidth
        val peekHeight = when {
            isPhoneLandscape -> (screenHeight * 0.12f).coerceIn(72.dp, 128.dp)
            isFoldableLandscape -> (screenHeight * 0.28f).coerceIn(172.dp, 240.dp)
            isTabletLandscape -> (screenHeight * 0.24f).coerceIn(150.dp, 240.dp)
            isFoldablePortrait -> (screenHeight * 0.16f).coerceIn(144.dp, 190.dp)
            isTabletPortrait -> (screenHeight * 0.09f).coerceIn(96.dp, 136.dp)
            isLandscape -> (screenHeight * 0.12f).coerceIn(72.dp, 128.dp)
            else -> (screenHeight * 0.16f).coerceIn(bottomPadding + 112.dp, bottomPadding + 172.dp)
        }
        val imageFadeHeight = (screenHeight * 0.18f).coerceIn(120.dp, 240.dp)

        val density = LocalDensity.current
        val screenHeightPx = with(density) { screenHeight.toPx() }

        Box(modifier = Modifier.fillMaxSize()) {

            val baseImageModifier = Modifier
                .fillMaxWidth()
                .height(screenHeight)
                .hazeSource(state = hazeState)
                .graphicsLayer {
                    translationY = -scrollState.value * 0.4f
                    val scrollFraction = (scrollState.value.toFloat() / screenHeightPx).coerceIn(0f, 1f)
                    alpha = 1f - (scrollFraction * 0.6f)
                }
                .drawWithContent {
                    drawContent()
                    val gradientHeight = imageFadeHeight.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = size.height - gradientHeight,
                            endY = size.height
                        )
                    )
                }

        Box(modifier = baseImageModifier) {
            if (isLandscape && !currentApod.isVideo() && !currentApod.url.isNullOrEmpty()) {
                AsyncImage(
                    model = currentApod.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.1f
                            scaleY = 1.1f
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(Color.Black.copy(alpha = 0.5f))
                        }
                )
            }
            
            // Foreground layer with professional crossfade - only visible during transitions
            Crossfade(
                targetState = currentApod.date,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "main_content_transition"
            ) { _ ->
                val hasImage = !currentApod.isVideo() && !currentApod.url.isNullOrEmpty()
                val hasVideo = currentApod.isVideo() && !currentApod.url.isNullOrEmpty()

                when {
                    hasVideo -> {
                        // No image load callback — dismiss shimmer immediately via effect
                        LaunchedEffect(currentApod.date) { onImageLoaded() }
                        CelestisVideoPlayer(
                            videoUrl = currentApod.url ?: "",
                            modifier = Modifier.fillMaxSize(),
                            onError = { println("Video playback error: $it") },
                            isPlaying = isVideoPlaying,
                            isLandscape = isLandscape,
                            onPlayingChange = { isVideoPlaying = it }
                        )
                    }
                    hasImage -> {
                        if (!isLandscape) {
                            Box(modifier = Modifier.fillMaxSize().blur(24.dp)) {
                                AsyncImage(
                                    model = currentApod.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.55f))
                                )
                            }
                        }
                        AsyncImage(
                            model = currentApod.url,
                            contentDescription = currentApod.title,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center,
                            onSuccess = { onImageLoaded() },
                            onError = { onImageLoaded() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = navigationOverlayWidth)
                                .graphicsLayer {
                                    scaleX = 1.25f
                                    scaleY = 1.25f
                                }
                        )
                    }
                    else -> {
                        LaunchedEffect(currentApod.date) { onImageLoaded() }
                        MediaUnavailablePlaceholder(title = currentApod.title)
                    }
                }
            }
            AnimatedVisibility(
                visible = isImageLoading,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                ImageLoadingShimmer()
            }
        }

        // SCROLLABLE FOREGROUND LAYER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Spacer for both images and videos - images are clickable, videos are not
            if (!displayApod.isVideo()) {
                Spacer(
                    modifier = Modifier
                        .height(screenHeight - peekHeight)
                        .fillMaxWidth()
                        .padding(start = navigationOverlayWidth)
                        .clickable { onShowHdImage(displayApod.urlHD, displayApod.url) }
                )
            } else {
                // For videos, use pointerInput to allow touches to pass through to video below
                Spacer(
                    modifier = Modifier
                        .height(screenHeight - peekHeight)
                        .fillMaxWidth()
                        .clickable { isVideoPlaying = !isVideoPlaying }// Empty pointerInput allows touches to pass through
                )
            }

            if (shouldShowGlassSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = navigationOverlayWidth)
                ) {

                    // GLASS SHEET CONTENT
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp) // Leave 28dp of space for the FAB to overlap the top border
                            .defaultMinSize(minHeight = screenHeight * 0.7f)
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    backgroundColor = Color.White.copy(alpha = 0.15f),
                                    blurRadius = 40.dp,
                                    noiseFactor = 0.05f,
                                    tint = HazeTint.Unspecified
                                )
                            )
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                            .padding(bottom = bottomPadding + 40.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(50)),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = displayApod.date,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            if (shouldShowInlineArrow) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SwipeUpIndicatorIcon()
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            // Share and Favorite buttons side by side
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onShare) {
                                    Icon(
                                        Icons.Default.Share,
                                        "Share",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                AnimatedFavoriteButton(
                                    isFavorite = displayApod.isFavorite,
                                    onFavoriteClick = {
                                        // Using the correct Compose HapticFeedbackType
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onFavoriteToggle(displayApod.date, !displayApod.isFavorite)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = displayApod.title ?: "Unknown",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            lineHeight = 36.sp
                        )

                        Text(
                            text = displayApod.explanation ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Justify,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 26.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        displayApod.copyright?.let {
                            Text(
                                text = "© $it",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(0.6f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                }
            }
        }

        val topBarAlpha by remember {
            derivedStateOf {
                1f - (scrollState.value.toFloat() / 300f).coerceIn(0f, 1f)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = navigationOverlayWidth)
                .graphicsLayer { alpha = topBarAlpha }
        ) {
            ApodTopAppBar(
                titleContent = {
                    Text(
                        text = "CELESTIS",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                },
                hazeState = hazeState,
                actions = {
                    // Share button moved to sheet header
                },
                windowSizeClass = windowSizeClass,
                isVideoSource = isVideoSource
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = navigationOverlayWidth)
                .padding(top = statusBarTop + homeControlsTopPadding)
                .padding(horizontal = ApodTopAppBarHorizontalPadding)
                .graphicsLayer { alpha = topBarAlpha }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isShowingRandom) {
                    GlassIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Today",
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onBackToToday()
                        },
                        hazeState = hazeState,
                        enabled = topBarAlpha > 0.1f
                    )
                } else {
                    Spacer(modifier = Modifier.width(56.dp))
                }
                
                RandomApodActionButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onRefresh()
                    },
                    hazeState = hazeState,
                    enabled = topBarAlpha > 0.1f
                )
            }
        }

        val isAtTop by remember { derivedStateOf { scrollState.value < 50 } }
        AnimatedVisibility(
            visible = isAtTop && !shouldShowInlineArrow,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = navigationOverlayWidth)
                .padding(bottom = bottomPadding + 18.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SwipeUpIndicatorIcon()
            }
        }

            // HD Image Popup Layer
            if (state.selectedHdUrl != null) {
                HdImagePopup(imageUrl = state.selectedHdUrl, onDismiss = onHideHdImage)
            }
        }
    }
}

@Composable
fun SwipeUpIndicatorIcon(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_y"
    )

    Icon(
        imageVector = Icons.Rounded.KeyboardArrowUp,
        contentDescription = "Scroll up for more",
        tint = Color.White.copy(alpha = 0.7f),
        modifier = modifier
            .size(36.dp)
            .graphicsLayer { translationY = offsetY }
    )
}

@Composable
fun AnimatedFavoriteButton(
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean
) {
    var isAnimatingRainbow by remember { mutableStateOf(false) }
    val wasFavorite = remember { mutableStateOf(isFavorite) }

    LaunchedEffect(isFavorite) {
        if (isFavorite && !wasFavorite.value) {
            isAnimatingRainbow = true
            delay(500)
            isAnimatingRainbow = false
        }
        wasFavorite.value = isFavorite
    }

    val infiniteTransition = rememberInfiniteTransition(label = "RainbowTransition")
    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HueAnimation"
    )

    val iconColor = when {
        isAnimatingRainbow -> Color.hsv(hue = hue, saturation = 0.8f, value = 1f)
        isFavorite -> Color.Red
        else -> Color.White.copy(alpha = 0.8f)
    }

    IconButton(onClick = onFavoriteClick) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = "Favorite",
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}



@Composable
fun RandomApodActionButton(
    onClick: () -> Unit,
    hazeState: HazeState,
    enabled: Boolean = true
) {
    var isRainbowing by remember { mutableStateOf(false) }
    var rainbowPulse by remember { mutableStateOf(0) }

    LaunchedEffect(rainbowPulse) {
        if (rainbowPulse > 0) {
            delay(1500)
            isRainbowing = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "RainbowTransition")

    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HueAnimation"
    )

    val iconColor = if (isRainbowing) {
        Color.hsv(hue = hue, saturation = 0.6f, value = 1f)
    } else {
        Color.White
    }
    
    Surface(
        onClick = {
            isRainbowing = true
            rainbowPulse += 1
            onClick()
        },
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .height(56.dp)
            .clip(CircleShape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color.White.copy(alpha = 0.15f),
                    blurRadius = 30.dp,
                    noiseFactor = 0.05f,
                    tint = HazeTint.Unspecified
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Discover Random Photo",
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Discover",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = iconColor
            )
        }
    }
}

@Composable
fun GlassIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color.White.copy(alpha = 0.15f),
                    blurRadius = 30.dp,
                    noiseFactor = 0.05f,
                    tint = HazeTint.Unspecified
                )
            )
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.2f else 0f), CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun HomeScreenError(state: HomeUiState.Error, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRefresh) {
            Text("Retry")
        }
    }
}

@Composable
fun HomeScreenLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ImageLoadingShimmer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()

                // Center starts at -20% of screen height (fully off-top, alpha=0 at screen edge).
                // At progress=1 the center is at 125% (fully off-bottom, alpha=0).
                // Both endpoints are invisible so the Restart jump is seamless.
                // The band becomes visible around the 1/3 mark as it descends into view.
                val bandHalfHeight = size.height * 0.18f
                val centerY = size.height * (-0.20f + progress * 1.45f)

                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f   to Color.Transparent,
                            0.2f to Color.White.copy(alpha = 0.03f),
                            0.5f to Color.White.copy(alpha = 0.09f),
                            0.8f to Color.White.copy(alpha = 0.03f),
                            1f   to Color.Transparent,
                        ),
                        start = Offset(0f, centerY - bandHalfHeight),
                        end   = Offset(size.width, centerY + bandHalfHeight)
                    ),
                    size = size
                )
            }
    )
}

@Composable
fun MediaUnavailablePlaceholder(title: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A)),  // deep space dark
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,  // reuse existing import
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Media unavailable",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
