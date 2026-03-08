package com.example.astrolume.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.astrolume.model.ApodResponse
import com.example.astrolume.ui.utils.CommonBackHandler
import com.example.astrolume.ui.viewModels.HomeUiState
import com.example.astrolume.ui.viewModels.HomeViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    windowSizeClass: WindowSizeClass,
    onOpenDrawer: () -> Unit,
    hazeState: HazeState
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Collect the toggle for Today vs Random
    val isShowingRandom by viewModel.isShowingRandom.collectAsStateWithLifecycle()

    val isFetchingRandom by viewModel.isFetchingRandom.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                HomeScreenLoading()
            }

            is HomeUiState.Error -> {
                HomeScreenError(state, onRefresh = viewModel::showRandomNext)
            }

            is HomeUiState.Success -> {
                HomeScreenSuccess(
                    state = state,
                    windowSizeClass = windowSizeClass,
                    isShowingRandom = isShowingRandom,
                    isFetchingRandom = isFetchingRandom,
                    onRefresh = viewModel::showRandomNext, // Trigger prefetch & swap
                    onBackToToday = viewModel::showToday,
                    onFavoriteToggle = viewModel::toggleFavorite,
                    onOpenDrawer = onOpenDrawer,
                    hazeState = hazeState,
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
    isFetchingRandom: Boolean,
    onRefresh: () -> Unit,
    onBackToToday: () -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit,
    onOpenDrawer: () -> Unit,
    hazeState: HazeState,
) {
    CommonBackHandler(enabled = isShowingRandom) { onBackToToday() }

    val displayApod = if (isShowingRandom) {
        state.randomApod ?: state.todayApod
    } else {
        state.todayApod
    }

    var isExpanded by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    val cardWidth by animateDpAsState(
        targetValue = when {
            !isLandscape -> 1000.dp // Will be constrained by fillMaxWidth()
            isExpanded -> 500.dp    // Expanded Landscape Width
            else -> 320.dp          // Collapsed Landscape Width
        }, label = "Width"
    )

    val cardHeight by animateDpAsState(
        targetValue = when {
            isExpanded -> 320.dp    // Capped height: Scrollable text handles the rest
            else -> 180.dp          // Snug collapsed height
        }, label = "Height"
    )

    if (isLandscape) {
        // --- LANDSCAPE LAYOUT ---
        Box(modifier = Modifier.fillMaxSize()) {

            // Full-screen immersive media
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isVisible = !isVisible
                        if (!isVisible) isExpanded = false // Auto-collapse when hiding
                    }
            ) {
                MediaDisplayLayer(url = displayApod.url, hazeState = hazeState)
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInHorizontally { -20 },
                exit = fadeOut() + slideOutHorizontally { -20 }
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(24.dp)
                        .graphicsLayer {
                            // Fade in/out with the isVisible toggle
                            alpha = if (isVisible) 1f else 0f
                            translationY = if (isVisible) 0f else -50f
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    GlassIconButton(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        onClick = onOpenDrawer,
                        hazeState = hazeState,
                        enabled = isVisible
                    )
                    Row {
                        AnimatedVisibility(
                            visible = isShowingRandom,
                            enter = slideInHorizontally() + fadeIn(),
                            exit = slideOutHorizontally() + fadeOut()
                        ) {
                            GlassIconButton(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                onClick = onBackToToday,
                                modifier = Modifier.padding(end = 8.dp),
                                hazeState = hazeState,
                                enabled = isVisible
                            )
                        }

                        if (!isShowingRandom) Spacer(Modifier.weight(1f))

                        RandomApodActionButton(
                            onClick = onRefresh,
                            hazeState = hazeState,
                            isLoading = isFetchingRandom,
                            enabled = isVisible,
                        )
                    }
                }
            }

            displayApod.copyright?.let {
                Text(
                    text = "© $it",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                        .graphicsLayer { alpha = if (isVisible) 0.7f else 0f },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Anchored to bottom right like portrait
                    .padding(end = 24.dp, bottom = 24.dp, top = 80.dp)
                    .graphicsLayer {
                        alpha = if (isVisible) 1f else 0f
                        transformOrigin = TransformOrigin(1f, 1f) // Scale from the bottom right corner
                        scaleX = if (isVisible) 1f else 0.8f
                        scaleY = if (isVisible) 1f else 0.8f
                    }
                    .animateContentSize(
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        alignment = Alignment.BottomEnd
                    )
                    .then(
                        if (isExpanded) {
                            Modifier
                                .width(if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) 500.dp else 420.dp)
                                .fillMaxHeight(0.85f)
                        } else {
                            Modifier
                                .width(340.dp)
                                .height(160.dp)
                        }
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = Color(0xFF111111),
                            blurRadius = 40.dp,
                            noiseFactor = 0.15f,
                            tint = null
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(0.25f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable { isExpanded = !isExpanded }
            ) {
                CardContent(
                    displayApod = displayApod,
                    title = displayApod.title ?: "Unknown",
                    explanation = displayApod.explanation ?: "",
                    isExpanded = isExpanded,
                    isFavorite = displayApod.isFavorite,
                    isVisible = isVisible,
                    onFavoriteClick = { onFavoriteToggle(displayApod.date, !displayApod.isFavorite) },
                    hazeState = hazeState
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState) // Apply .haze() ONLY to this layer
            ) {
                Crossfade(
                    targetState =
                        if (displayApod.mediaType.equals("video", ignoreCase = true)) {
                            displayApod.thumbnailUrl ?: displayApod.url
                        } else {
                            displayApod.url
                        }, animationSpec = tween(700)
                ) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { isVisible = !isVisible }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .graphicsLayer { alpha = if (isVisible) 1f else 0f }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                        )
                    )
            )

            // Top Navigation
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer { alpha = if (isVisible) 1f else 0f },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = isShowingRandom,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    GlassIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBackToToday,
                        hazeState = hazeState,
                        enabled = isVisible
                    )
                }

                if (!isShowingRandom) Spacer(Modifier.weight(1f))

                RandomApodActionButton(
                    onClick = onRefresh,
                    hazeState = hazeState, // haze
                    isLoading = isFetchingRandom,
                    enabled = isVisible,

                )
            }

            // Copyright Info (centered at bottom of image area)
            displayApod.copyright?.let {
                Text(
                    text = "© $it",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .graphicsLayer { alpha = if (isVisible) 0.7f else 0f },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }

            // The Glass Card
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .graphicsLayer {
                        alpha = if (isVisible) 1f else 0f
                        transformOrigin = TransformOrigin(1f, 1f) // Anchor point: Bottom-Right
                        scaleX = if (isVisible) 1f else 0.8f
                        scaleY = if (isVisible) 1f else 0.8f
                    }
                    .then(if (isLandscape) Modifier.width(cardWidth) else Modifier.fillMaxWidth())
                    .height(cardHeight)
                    .clip(RoundedCornerShape(28.dp))
                    .hazeChild(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = Color(0xFF111111), // Darker for better contrast
                            blurRadius = 30.dp,
                            noiseFactor = 0.15f,
                            tint = null
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                    .clickable(enabled = isVisible) { isExpanded = !isExpanded }
                    .animateContentSize()
            ) {
                CardContent(
                    displayApod = displayApod,
                    title = displayApod.title ?: "Unknown",
                    explanation = displayApod.explanation ?: "",
                    isExpanded = isExpanded,
                    isFavorite = displayApod.isFavorite,
                    isVisible = isVisible,
                    onFavoriteClick = {
                        onFavoriteToggle(
                            displayApod.date,
                            !displayApod.isFavorite
                        )
                    },
                    hazeState = hazeState
                )
            }
        }
    }
}
@Composable
fun CardContent(
    displayApod: ApodResponse,
    title: String,
    explanation: String,
    isExpanded: Boolean,
    isFavorite: Boolean,
    isVisible: Boolean,
    onFavoriteClick: () -> Unit,
    hazeState: HazeState
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp), // Space between the pill and the title
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .hazeChild(state = hazeState,
                        style = HazeStyle(
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                            blurRadius = 30.dp,
                            noiseFactor = 0.15f,
                            tint = null
                        )
                    ),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = displayApod.date,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Title Row with crossfade to prevent text jumping during container resize
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                modifier = Modifier.weight(1f),
                label = "TitleAnimation"
            ) { targetTitle ->
                Text(
                    text = targetTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontSize = 24.sp,
                    maxLines = if (isExpanded) 3 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onFavoriteClick, enabled = isVisible) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color.Red else Color.White.copy(alpha = if(isVisible) 1f else 0f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // We wrap the description in a LazyColumn so it's scrollable when expanded
        val scrollState = rememberLazyListState()

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            // Only allow scrolling if the card is actually expanded
            userScrollEnabled = isExpanded
        ) {
            item {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 22.sp
                )
            }
        }
    }
}


@Composable
fun RandomApodActionButton(
    onClick: () -> Unit,
    hazeState: HazeState,
    isLoading: Boolean,
    enabled: Boolean = true) {
    val infiniteTransition = rememberInfiniteTransition(label = "RainbowTransition")

    // Animates the hue from 0 to 360 degrees infinitely
    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HueAnimation"
    )

    // Interpolate between White and the Rainbow Hue based on loading state
    val iconColor = if (isLoading) {
        Color.hsv(hue = hue, saturation = 0.6f, value = 1f)
    } else {
        Color.White
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .hazeChild(state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color.White.copy(alpha = 0.15f),
                    blurRadius = 30.dp,
                    noiseFactor = 0.15f,
                    tint = null
                )
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.AutoAwesome, "Surprise Me", tint = iconColor.copy(alpha = if (enabled) 1f else 0f))
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
            .hazeChild(state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color.White.copy(alpha = 0.15f),
                    blurRadius = 30.dp,
                    noiseFactor = 0.15f,
                    tint = null
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
fun MediaDisplayLayer(url: String?, hazeState: HazeState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .haze(state = hazeState)
    ) {
        // Layer 1: Blurred Background to prevent letterboxing empty space
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(50.dp)
                .alpha(0.4f)
        )

        // Layer 2: The actual image, fitted properly
        AsyncImage(
            model = url,
            contentDescription = "APOD Image",
            contentScale = ContentScale.Fit, // Keeps subject perfectly in frame
            modifier = Modifier.fillMaxSize()
        )
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
