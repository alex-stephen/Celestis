package com.example.celestis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun CelestisRangePicker(
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val dateRangePickerState = rememberDateRangePickerState(
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= Clock.System.now().toEpochMilliseconds()
            }
            override fun isSelectableYear(year: Int): Boolean = year in 1995..2026
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis) },
                enabled = dateRangePickerState.selectedStartDateMillis != null
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        Column {
            // 1. THE FAST-TRAVEL HEADER
            Text(
                text = "Jump to Year",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Generate years from 1995 to current
                val years = (1995..2026).reversed().toList()
                items(years) { year ->
                    SuggestionChip(
                        onClick = {
                            // Programmatically jump the calendar to Jan 1st of that year
                            val calendar = java.util.Calendar.getInstance()
                            calendar.set(year, 0, 1)
                            scope.launch {
                                dateRangePickerState.displayedMonthMillis = calendar.timeInMillis
                            }
                        },
                        label = { Text(year.toString()) }
                    )
                }
            }

            // 2. THE CALENDAR
            DateRangePicker(
                state = dateRangePickerState,
                showModeToggle = false, // Pencil is gone forever
                title = null, // We used our custom jump header instead
                headline = {
                    // Keep the range selection feedback
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    Text(
                        text = if (start != null && end != null) "Range Selected" else "Select Dates",
                        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        }
    }
}