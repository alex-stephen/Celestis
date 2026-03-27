package com.example.celestis.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Pager-based Bottom Navigation Bar for use with HorizontalPager
 */
@Composable
fun ApodBottomNavBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState,
) {
    NavigationBar(
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
            .drawBehind {
                val strokeWidthPx = 1.dp.toPx()
                val verticalOffset = strokeWidthPx / 2

                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, verticalOffset),
                    end = Offset(size.width, verticalOffset),
                    strokeWidth = strokeWidthPx
                )
            }
            .height(70.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0) // Disable automatic window insets
    ) {
        NavItem.entries.forEachIndexed { index, item ->
            CustomNavItem(
                selected = selectedIndex == index,
                icon = item.icon,
                label = item.label,
                onClick = { onTabSelected(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * State holder for the Bottom Bar visibility logic.
 */
@Stable
class BottomBarState {
    var isVisible by mutableStateOf(true)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // available.y < 0 means scrolling down
            if (available.y < -10f) {
                isVisible = false
            } else if (available.y > 10f) {
                isVisible = true
            }
            return Offset.Zero
        }
    }
}