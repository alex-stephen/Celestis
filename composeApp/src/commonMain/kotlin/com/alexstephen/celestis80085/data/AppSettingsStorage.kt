package com.alexstephen.celestis80085.data

expect class AppSettingsStorage {
    fun getLowDataMode(): Boolean?
    fun setLowDataMode(enabled: Boolean)
}
