package com.alexstephen.celestis80085.observability

expect object CrashReporter {
    fun log(message: String)
    fun recordException(throwable: Throwable, message: String? = null)
    fun setCustomKey(key: String, value: String)
}
