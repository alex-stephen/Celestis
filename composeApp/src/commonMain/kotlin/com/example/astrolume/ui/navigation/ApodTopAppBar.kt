package com.example.astrolume.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

@Composable
fun ApodTopAppBar(
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    titleContent: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    hazeState: HazeState
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Scrim to ensure text readability over bright images
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color(0xFF111111),
                    blurRadius = 40.dp,
                    tint = null
                )
            )
            .drawBehind {
                val strokeWidthPx = 1.dp.toPx()
                val verticalOffset = size.height - strokeWidthPx / 2
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, verticalOffset),
                    end = Offset(size.width, verticalOffset),
                    strokeWidth = strokeWidthPx
                )
            }
            .height(72.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Left Slot (Menu/Back)
        if (navigationIcon != null) {
            Box(Modifier.align(Alignment.CenterStart)) {
                navigationIcon()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f) // Constrain width so it doesn't hit the icons
                .fillMaxHeight()
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            titleContent()
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}