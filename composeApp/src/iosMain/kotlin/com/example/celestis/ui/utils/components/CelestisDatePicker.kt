package com.example.celestis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.hazeEffect
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSSelectorFromString
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle
import platform.darwin.NSObject

/**
 * Helper that bridges UIDatePicker value-changed events into a Kotlin callback.
 */
@OptIn(BetaInteropApi::class)
private class DatePickerDelegate : NSObject() {
    var onChange: ((NSDate) -> Unit)? = null

    @ObjCAction
    fun dateChanged(sender: UIDatePicker) {
        onChange?.invoke(sender.date)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CelestisRangePicker(
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    var startDate by remember { mutableStateOf<NSDate?>(null) }
    var endDate by remember { mutableStateOf<NSDate?>(null) }

    val maxDate = NSDate()

    val calendar = NSCalendar.currentCalendar
    val components = NSDateComponents().apply {
        year = 1995
        month = 6
        day = 16
    }
    val minDate = calendar.dateFromComponents(components)

    val startDelegate = remember {
        DatePickerDelegate().apply {
            onChange = { date -> startDate = date }
        }
    }
    val endDelegate = remember {
        DatePickerDelegate().apply {
            onChange = { date -> endDate = date }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Select Date Range",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start Date Picker
                Text(
                    "Start Date",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
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
                            target = startDelegate,
                            action = NSSelectorFromString("dateChanged:"),
                            forControlEvents = platform.UIKit.UIControlEventValueChanged
                        )
                        startDate = picker.date
                        picker
                    },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    update = { _ -> }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // End Date Picker
                Text(
                    "End Date",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
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
                            target = endDelegate,
                            action = NSSelectorFromString("dateChanged:"),
                            forControlEvents = platform.UIKit.UIControlEventValueChanged
                        )
                        endDate = picker.date
                        picker
                    },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    update = { _ -> }
                )

                // Validation message
                val validationMessage = remember(startDate, endDate) {
                    val start = startDate
                    val end = endDate
                    if (start != null && end != null) {
                        val daysDiff = ((end.timeIntervalSince1970 - start.timeIntervalSince1970) / (24 * 60 * 60)).toInt()
                        if (daysDiff < 0) {
                            "End date must be after start date"
                        } else {
                            "$daysDiff days selected"
                        }
                    } else {
                        ""
                    }
                }

                if (validationMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (validationMessage.contains("must be after"))
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val start = startDate
                            val end = endDate ?: start
                            if (start != null && end != null) {
                                val startMillis = (start.timeIntervalSince1970 * 1000).toLong()
                                val endMillis = (end.timeIntervalSince1970 * 1000).toLong()
                                onConfirm(startMillis, endMillis)
                            }
                        },
                        enabled = startDate != null && endDate != null && run {
                            val start = startDate
                            val end = endDate
                            if (start != null && end != null) {
                                val daysDiff = ((end.timeIntervalSince1970 - start.timeIntervalSince1970) / (24 * 60 * 60)).toInt()
                                daysDiff >= 0
                            } else {
                                false
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
