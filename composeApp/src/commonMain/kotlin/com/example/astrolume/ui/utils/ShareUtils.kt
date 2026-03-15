package com.example.astrolume.ui.utils

import com.example.astrolume.model.ApodResponse
import coil3.PlatformContext

/**
 * Platform-specific sharing functionality.
 * Implementations handle sharing APOD content via native share sheets.
 */
expect fun shareApod(apod: ApodResponse, context: PlatformContext)
