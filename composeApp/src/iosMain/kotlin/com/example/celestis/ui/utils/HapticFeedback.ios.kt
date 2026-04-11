package com.example.celestis.ui.utils

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_SEC
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

class IOSHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(type: HapticFeedbackType) {
        when (type) {
            HapticFeedbackType.LIGHT_IMPACT -> {
                val generator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
                generator.prepare()
                generator.impactOccurred()
            }
            HapticFeedbackType.MEDIUM_IMPACT -> {
                val generator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
                generator.prepare()
                generator.impactOccurred()
            }
            HapticFeedbackType.HEAVY_IMPACT -> {
                val generator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
                generator.prepare()
                generator.impactOccurred()
            }
            HapticFeedbackType.SUCCESS -> {
                val generator = UINotificationFeedbackGenerator()
                generator.prepare()
                generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            }
            HapticFeedbackType.WARNING -> {
                val generator = UINotificationFeedbackGenerator()
                generator.prepare()
                generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
            }
            HapticFeedbackType.ERROR -> {
                val generator = UINotificationFeedbackGenerator()
                generator.prepare()
                generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
            }
            HapticFeedbackType.DICE_ROLL -> {
                // 3 quick light taps (dice rattling) then a heavy thump (landing)
                val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
                val heavy = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
                light.prepare()
                heavy.prepare()
                light.impactOccurred()
                val queue = dispatch_get_main_queue()
                dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (0.085 * NSEC_PER_SEC).toLong()), queue) {
                    light.impactOccurred()
                }
                dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (0.17 * NSEC_PER_SEC).toLong()), queue) {
                    light.impactOccurred()
                }
                dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (0.26 * NSEC_PER_SEC).toLong()), queue) {
                    heavy.impactOccurred()
                }
            }
        }
    }
}

actual fun createHapticFeedback(): HapticFeedback = IOSHapticFeedback()
