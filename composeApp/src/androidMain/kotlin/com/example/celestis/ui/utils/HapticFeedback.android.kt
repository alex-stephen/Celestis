package com.example.celestis.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.koin.core.context.GlobalContext

class AndroidHapticFeedback(private val context: Context) : HapticFeedback {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun performHapticFeedback(type: HapticFeedbackType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (type) {
                HapticFeedbackType.LIGHT_IMPACT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticFeedbackType.MEDIUM_IMPACT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticFeedbackType.HEAVY_IMPACT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                HapticFeedbackType.SUCCESS -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                HapticFeedbackType.WARNING -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticFeedbackType.ERROR -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            }
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val duration = when (type) {
                HapticFeedbackType.LIGHT_IMPACT -> 10L
                HapticFeedbackType.MEDIUM_IMPACT -> 20L
                HapticFeedbackType.HEAVY_IMPACT -> 40L
                HapticFeedbackType.SUCCESS -> 30L
                HapticFeedbackType.WARNING -> 20L
                HapticFeedbackType.ERROR -> 50L
            }
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }
}

actual fun createHapticFeedback(): HapticFeedback {
    val context = GlobalContext.get().get<Context>()
    return AndroidHapticFeedback(context)
}
