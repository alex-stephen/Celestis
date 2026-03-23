package com.example.celestis.ui.utils

import com.example.celestis.model.ApodResponse
import coil3.PlatformContext

/**
 * Platform-specific sharing functionality.
 * Implementations handle sharing APOD content via native share sheets.
 */
expect fun shareApod(apod: ApodResponse, context: PlatformContext)
