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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun CelestisRangePicker(
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Define the APOD Epoch: June 16, 1995
    val apodEpoch = remember {
        LocalDate(1995, 6, 16).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }
    val today = remember {
        Clock.System.now().toEpochMilliseconds()
    }

    val dateRangePickerState = rememberDateRangePickerState(
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Elegantly restrict: Epoch <= Selected <= Today
                return utcTimeMillis in apodEpoch..today
            }

            override fun isSelectableYear(year: Int): Boolean {
                val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
                return year in 1995..currentYear
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = "Jump to Year",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
            val years = remember { (1995..currentYear).reversed().toList() }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(years) { year ->
                    SuggestionChip(
                        onClick = {
                            // KMP-friendly way to get the start of the year
                            val jumpedDate = LocalDate(year, 1, 1)
                                .atStartOfDayIn(TimeZone.UTC)
                                .toEpochMilliseconds()

                            scope.launch {
                                // Ensure we don't jump before the epoch
                                val safeJump = if (jumpedDate < apodEpoch) apodEpoch else jumpedDate
                                dateRangePickerState.displayedMonthMillis = safeJump
                            }
                        },
                        label = { Text(year.toString()) }
                    )
                }
            }

            DateRangePicker(
                state = dateRangePickerState,
                showModeToggle = false,
                title = null,
                headline = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    Text(
                        text = if (start != null && end != null) "Range Selected" else "Select Dates",
                        modifier = Modifier.padding(start = 24.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        }
    }
}