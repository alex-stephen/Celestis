package com.alexstephen.celestis80085.ui.utils

import androidx.compose.runtime.Composable

@Composable
expect fun CommonBackHandler(enabled: Boolean = true, onBack: () -> Unit)