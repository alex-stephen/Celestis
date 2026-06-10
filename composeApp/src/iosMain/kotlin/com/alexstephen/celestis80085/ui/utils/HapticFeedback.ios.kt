package com.alexstephen.celestis80085.ui.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

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
                CoroutineScope(Dispatchers.Main).launch {
                    delay(85L)
                    light.impactOccurred()
                    delay(85L) // 170ms total from start
                    light.impactOccurred()
                    delay(90L) // 260ms total from start
                    heavy.impactOccurred()
                }
            }
        }
    }
}

actual fun createHapticFeedback(): HapticFeedback = IOSHapticFeedback()
