package com.alexstephen.celestis80085.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

/**
 * Navigation Drawer Sheet with styling aligned to ApodBottomNavBar.
 * Uses pager-based navigation instead of NavController to prevent crashes.
 * 
 * @param currentPageIndex The current page index in the HorizontalPager
 * @param onPageSelected Callback when a page/tab is selected
 * @param drawerState The DrawerState to control open/close
 * @param hazeState The background haze state for glassmorphism effect
 */
@Composable
fun ApodDrawerSheet(
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    drawerState: DrawerState,
    hazeState: HazeState
) {
    val scope = rememberCoroutineScope()

    // Custom Glass Sheet - aligned with ApodBottomNavBar styling
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .clip(RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp))
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color(0xFF111111).copy(alpha = 0.85f),
                    blurRadius = 30.dp, // Match bottom bar blur radius
                    noiseFactor = 0f,
                    tint = HazeTint.Unspecified
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(0.15f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            )
            .padding(24.dp)
    ) {
        Column {
            // Header
            Text(
                text = "Celestis",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary, // Use theme primary color
                modifier = Modifier.padding(bottom = 32.dp, top = 16.dp)
            )

            // Navigation Items - Matches ApodBottomNavBar structure
            NavItem.entries.forEachIndexed { index, item ->
                DrawerNavItem(
                    selected = currentPageIndex == index,
                    icon = item.icon,
                    label = item.label,
                    onClick = {
                        // Close drawer first, then navigate
                        scope.launch {
                            drawerState.close()
                        }
                        // Navigate to the selected page
                        onPageSelected(index)
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Individual drawer navigation item with animated styling matching CustomNavItem.
 */
@Composable
private fun DrawerNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    // Animate colors and scales - matching CustomNavItem behavior
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
        animationSpec = tween(300),
        label = "drawer_item_color"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "drawer_item_scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color.White.copy(0.1f) else Color.Transparent,
        animationSpec = tween(300),
        label = "drawer_item_background"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Remove ripple effect like CustomNavItem
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .size(26.dp) // Match CustomNavItem icon size
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
