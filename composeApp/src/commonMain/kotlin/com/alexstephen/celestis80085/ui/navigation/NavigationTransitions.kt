package com.alexstephen.celestis80085.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

object Transitions {
    private const val TabDuration = 200
    private const val DetailDuration = 300

    // Tab-to-Tab: Fade and very slight scale-in
    val tabEnter = fadeIn(
        animationSpec = tween(TabDuration, easing = LinearOutSlowInEasing)
    ) + scaleIn(
        initialScale = 0.98f, // Very subtle scale
        animationSpec = tween(TabDuration, easing = LinearOutSlowInEasing)
    )

    val tabExit = fadeOut(animationSpec = tween(TabDuration))

    // Hierarchical: Slide up from the bottom (Vertical)
    val detailEnter = slideInVertically(
        initialOffsetY = { it / 6 }, // Starts slightly lower
        animationSpec = tween(DetailDuration, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(DetailDuration))

    val detailExit = slideOutVertically(
        targetOffsetY = { it / 6 },
        animationSpec = tween(DetailDuration, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(DetailDuration))
}