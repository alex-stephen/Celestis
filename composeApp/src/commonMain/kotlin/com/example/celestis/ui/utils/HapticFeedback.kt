package com.example.celestis.ui.utils

interface HapticFeedback {
    fun performHapticFeedback(type: HapticFeedbackType = HapticFeedbackType.LIGHT_IMPACT)
}

enum class HapticFeedbackType {
    LIGHT_IMPACT,
    MEDIUM_IMPACT,
    HEAVY_IMPACT,
    SUCCESS,
    WARNING,
    ERROR
}

expect fun createHapticFeedback(): HapticFeedback
