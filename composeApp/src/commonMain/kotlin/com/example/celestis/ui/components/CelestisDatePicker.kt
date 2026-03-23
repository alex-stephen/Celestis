package com.example.celestis.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun CelestisRangePicker(
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
)