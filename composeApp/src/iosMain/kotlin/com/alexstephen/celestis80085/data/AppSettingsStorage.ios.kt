package com.alexstephen.celestis80085.data

import platform.Foundation.NSUserDefaults

actual class AppSettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getLowDataMode(): Boolean? {
        return if (defaults.objectForKey(KEY_LOW_DATA_MODE) != null) {
            defaults.boolForKey(KEY_LOW_DATA_MODE)
        } else {
            null
        }
    }

    actual fun setLowDataMode(enabled: Boolean) {
        defaults.setBool(enabled, forKey = KEY_LOW_DATA_MODE)
    }

    private companion object {
        const val KEY_LOW_DATA_MODE = "low_data_mode"
    }
}
