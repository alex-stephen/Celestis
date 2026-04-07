package com.example.celestis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import platform.Foundation.NSDate
import platform.Foundation.NSSelectorFromString
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIColor
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

    val apodEpochMillis = remember {
        LocalDate(1995, 6, 16).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }
    val minDate = remember {
        NSDate.dateWithTimeIntervalSince1970(apodEpochMillis.toDouble() / 1000.0)
    }

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

    val hazeState = remember { HazeState() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background layer — this is what hazeEffect will blur
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeSource(state = hazeState)
            )

            // Use real window dimensions — BoxWithConstraints reports Infinity height
            // inside a Dialog, making landscape detection unreliable.
            val windowInfo = LocalWindowInfo.current
            val density = LocalDensity.current
            val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
            val windowHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
            val isLandscape = windowWidthDp > windowHeightDp
            val pickerHeight = if (isLandscape) 120.dp else 150.dp
            // Cap the dialog height so verticalScroll has a bounded container
            val maxDialogHeight = windowHeightDp - 32.dp

                // Validation message computed once, used in both layout branches
                val validationMessage = remember(startDate, endDate) {
                    val start = startDate
                    val end = endDate
                    if (start != null && end != null) {
                        val daysDiff = ((end.timeIntervalSince1970 - start.timeIntervalSince1970) / (24 * 60 * 60)).toInt()
                        if (daysDiff < 0) "End date must be after start date"
                        else "$daysDiff days selected"
                    } else ""
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxDialogHeight),
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 6.dp,
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .hazeEffect(
                                state = hazeState,
                                style = HazeStyle(
                                    backgroundColor = Color(0xFF111111).copy(alpha = 0.85f),
                                    blurRadius = 50.dp,
                                    noiseFactor = 0.15f,
                                    tint = HazeTint.Unspecified,
                                )
                            )
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Select Date Range",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLandscape) {
                            // Landscape: pickers side by side to fit within screen height
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
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
                                            picker.backgroundColor = UIColor(red = 0x0B / 255.0, green = 0x0E / 255.0, blue = 0x14 / 255.0, alpha = 1.0)
                                            picker.addTarget(target = startDelegate, action = NSSelectorFromString("dateChanged:"), forControlEvents = platform.UIKit.UIControlEventValueChanged)
                                            startDate = picker.date
                                            picker
                                        },
                                        modifier = Modifier.fillMaxWidth().height(pickerHeight),
                                        update = { _ -> }
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
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
                                            picker.backgroundColor = UIColor(red = 0x0B / 255.0, green = 0x0E / 255.0, blue = 0x14 / 255.0, alpha = 1.0)
                                            picker.addTarget(target = endDelegate, action = NSSelectorFromString("dateChanged:"), forControlEvents = platform.UIKit.UIControlEventValueChanged)
                                            endDate = picker.date
                                            picker
                                        },
                                        modifier = Modifier.fillMaxWidth().height(pickerHeight),
                                        update = { _ -> }
                                    )
                                }
                            }
                        } else {
                            // Portrait: pickers stacked vertically
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
                                    picker.backgroundColor = UIColor(red = 0x0B / 255.0, green = 0x0E / 255.0, blue = 0x14 / 255.0, alpha = 1.0)
                                    picker.addTarget(target = startDelegate, action = NSSelectorFromString("dateChanged:"), forControlEvents = platform.UIKit.UIControlEventValueChanged)
                                    startDate = picker.date
                                    picker
                                },
                                modifier = Modifier.fillMaxWidth().height(pickerHeight),
                                update = { _ -> }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

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
                                    picker.backgroundColor = UIColor(red = 0x0B / 255.0, green = 0x0E / 255.0, blue = 0x14 / 255.0, alpha = 1.0)
                                    picker.addTarget(target = endDelegate, action = NSSelectorFromString("dateChanged:"), forControlEvents = platform.UIKit.UIControlEventValueChanged)
                                    endDate = picker.date
                                    picker
                                },
                                modifier = Modifier.fillMaxWidth().height(pickerHeight),
                                update = { _ -> }
                            )
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

                        // Buttons — always at the bottom of the scroll area
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) { Text("Cancel") }
                            TextButton(
                                onClick = {
                                    val start = startDate
                                    val end = endDate ?: start
                                    if (start != null && end != null) {
                                        // Clamp to APOD epoch — mirrors Android's safeJump guard
                                        val startMillis = (start.timeIntervalSince1970 * 1000).toLong()
                                            .coerceAtLeast(apodEpochMillis)
                                        val endMillis = (end.timeIntervalSince1970 * 1000).toLong()
                                            .coerceAtLeast(apodEpochMillis)
                                        onConfirm(startMillis, endMillis)
                                    }
                                },
                                enabled = startDate != null && endDate != null && run {
                                    val start = startDate
                                    val end = endDate
                                    if (start != null && end != null) {
                                        val daysDiff = ((end.timeIntervalSince1970 - start.timeIntervalSince1970) / (24 * 60 * 60)).toInt()
                                        daysDiff >= 0
                                    } else false
                                }
                            ) { Text("Confirm") }
                        }
                    }
                }
        }
    }
}
