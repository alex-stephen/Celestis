package com.alexstephen.celestis80085.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun CelestisRangePicker(
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
)