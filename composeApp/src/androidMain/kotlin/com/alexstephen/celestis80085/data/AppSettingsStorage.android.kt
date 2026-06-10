package com.alexstephen.celestis80085.data

import android.content.Context

actual class AppSettingsStorage(private val context: Context) {
    private val preferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun getLowDataMode(): Boolean? {
        return if (preferences.contains(KEY_LOW_DATA_MODE)) {
            preferences.getBoolean(KEY_LOW_DATA_MODE, false)
        } else {
            null
        }
    }

    actual fun setLowDataMode(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_LOW_DATA_MODE, enabled)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "celestis_settings"
        const val KEY_LOW_DATA_MODE = "low_data_mode"
    }
}
