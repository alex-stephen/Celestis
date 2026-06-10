package com.alexstephen.celestis80085.ui.utils

import com.alexstephen.celestis80085.model.ApodResponse
import coil3.PlatformContext

/**
 * Platform-specific sharing functionality.
 * Implementations handle sharing APOD content via native share sheets.
 */
expect fun shareApod(apod: ApodResponse, context: PlatformContext)
