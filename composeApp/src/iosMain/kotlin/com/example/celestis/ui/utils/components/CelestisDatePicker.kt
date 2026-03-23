package com.example.celestis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CelestisRangePicker(
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    var startDate by remember { mutableStateOf<NSDate?>(null) }
    var endDate by remember { mutableStateOf<NSDate?>(null) }
    
    // Calculate max date (today)
    val maxDate = NSDate()
    
    // Calculate min date (June 16, 1995 - first APOD)
    val calendar = NSCalendar.currentCalendar
    val components = NSDateComponents().apply {
        year = 1995
        month = 6
        day = 16
    }
    val minDate = calendar.dateFromComponents(components)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Date Range", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Start Date Picker
                Text(
                    "Start Date",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                UIKitView(
                    factory = {
                        val picker = UIDatePicker()
                        picker.datePickerMode = UIDatePickerMode.UIDatePickerModeDate
                        picker.preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
                        picker.minimumDate = minDate
                        picker.maximumDate = maxDate
                        picker.addTarget(
                            target = null,
                            action = null,
                            forControlEvents = platform.UIKit.UIControlEventValueChanged
                        )
                        picker
                    },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    update = { picker ->
                        startDate = picker.date
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // End Date Picker
                Text(
                    "End Date",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                UIKitView(
                    factory = {
                        val picker = UIDatePicker()
                        picker.datePickerMode = UIDatePickerMode.UIDatePickerModeDate
                        picker.preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
                        picker.minimumDate = minDate
                        picker.maximumDate = maxDate
                        picker
                    },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    update = { picker ->
                        endDate = picker.date
                    }
                )
                
                // Validation message
                val validationMessage = remember(startDate, endDate) {
                    val start = startDate
                    val end = endDate
                    if (start != null && end != null) {
                        val daysDiff = ((end.timeIntervalSince1970 - start.timeIntervalSince1970) / (24 * 60 * 60)).toInt()
                        if (daysDiff < 0) {
                            "End date must be after start date"
                        } else if (daysDiff > 31) {
                            "Range too long: $daysDiff days (max 31)"
                        } else {
                            "$daysDiff days selected"
                        }
                    } else {
                        ""
                    }
                }
                
                if (validationMessage.isNotEmpty()) {
                    Text(
                        text = validationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (validationMessage.contains("Range too long") || validationMessage.contains("must be after"))
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = startDate
                    val end = endDate ?: start
                    if (start != null && end != null) {
                        val startMillis = (start.timeIntervalSince1970 * 1000).toLong()
                        val endMillis = (end.timeIntervalSince1970 * 1000).toLong()
                        
                        // Validate range
                        val daysDiff = ((endMillis - startMillis) / (24 * 60 * 60 * 1000))
                        if (daysDiff >= 0 && daysDiff <= 31) {
                            onConfirm(startMillis, endMillis)
                        }
                    }
                },
                enabled = startDate != null && endDate != null && run {
                    val start = startDate
                    val end = endDate
                    if (start != null && end != null) {
                        val daysDiff = ((end.timeIntervalSince1970 - start.timeIntervalSince1970) / (24 * 60 * 60)).toInt()
                        daysDiff >= 0 && daysDiff <= 31
                    } else {
                        false
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
