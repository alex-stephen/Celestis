package com.example.astrolume.ui.navigation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val windowDpSize = with(density) {
        val size = windowInfo.containerSize
        DpSize(size.width.toDp(), size.height.toDp())
    }

    return WindowSizeClass.calculateFromSize(windowDpSize)
}