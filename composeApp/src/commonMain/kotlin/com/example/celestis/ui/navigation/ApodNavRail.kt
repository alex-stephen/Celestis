package com.example.celestis.ui.navigation

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Professional Navigation Rail with edge-to-edge design matching ApodBottomNavBar.
 */
@Composable
fun ApodNavRail(
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(ApodNavRailWidth)
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
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = NavigationRailDefaults.windowInsets
    ) {
        NavItem.entries.forEachIndexed { index, item ->
            CustomNavItem(
                selected = currentPageIndex == index,
                onClick = { onPageSelected(index) },
                icon = item.icon,
                label = item.label
            )
        }
    }
}
