package com.example.celestis.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    PermanentNavigationDrawer(
        modifier = modifier,
        drawerContent = {
            PermanentDrawerSheet(
                modifier = Modifier
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = Color(0xFF111111).copy(alpha = 0.85f),
                            blurRadius = 30.dp,
                            noiseFactor = 0f,
                            tint = HazeTint.Unspecified,
                        )
                    )
                    .width(240.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Celestis",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        },
        content = content
    )
}
