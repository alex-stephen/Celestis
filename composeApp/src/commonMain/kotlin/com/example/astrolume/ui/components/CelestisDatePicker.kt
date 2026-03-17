package com.example.astrolume.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun CelestisRangePicker(
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
)