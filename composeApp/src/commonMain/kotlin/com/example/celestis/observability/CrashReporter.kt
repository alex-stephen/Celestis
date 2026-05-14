package com.example.celestis.observability

expect object CrashReporter {
    fun log(message: String)
    fun recordException(throwable: Throwable, message: String? = null)
    fun setCustomKey(key: String, value: String)
}
