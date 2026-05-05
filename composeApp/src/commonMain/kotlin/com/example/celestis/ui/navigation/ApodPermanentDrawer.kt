package com.example.celestis.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Permanent drawer for tablet/desktop layouts.
 * Uses pager-based navigation instead of NavController for consistency.
 * 
 * @param navController NavController for deep navigation (PhotoDetail, etc.)
 * @param currentPageIndex The current page index in the HorizontalPager
 * @param onPageSelected Callback when a page/tab is selected
 * @param hazeState The background haze state for glassmorphism effect
 * @param modifier Modifier for the drawer
 * @param content Main content area
 */
@Composable
fun ApodPermanentDrawer(
    navController: NavHostController,
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()

        PermanentDrawerSheet(
            modifier = Modifier
                .fillMaxHeight()
                .width(ApodPermanentDrawerWidth)
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = Color(0xFF111111).copy(alpha = 0.92f),
                        blurRadius = 30.dp,
                        noiseFactor = 0f,
                        tint = HazeTint.Unspecified,
                    )
                )
                .drawBehind {
                    val strokeWidthPx = 1.dp.toPx()
                    val horizontalOffset = size.width - strokeWidthPx / 2
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(horizontalOffset, 0f),
                        end = Offset(horizontalOffset, size.height),
                        strokeWidth = strokeWidthPx
                    )
                },
            drawerContainerColor = Color.Transparent,
            drawerContentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Celestis",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            NavItem.entries.forEachIndexed { index, item ->
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = currentPageIndex == index,
                    onClick = {
                        // Navigate to the selected page
                        onPageSelected(index)
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color.White.copy(alpha = 0.10f),
                        unselectedContainerColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.White.copy(alpha = 0.85f),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = Color.White.copy(alpha = 0.85f)
                    )
                )
            }
        }
    }
}
