package com.alexstephen.celestis80085.observability

import platform.Foundation.NSLog

actual object CrashReporter {
    actual fun log(message: String) {
        NSLog("Celestis: $message")
    }

    actual fun recordException(throwable: Throwable, message: String?) {
        val prefix = message?.let { "$it - " } ?: ""
        NSLog("Celestis: ${prefix}${throwable.message ?: throwable.toString()}")
    }

    actual fun setCustomKey(key: String, value: String) {
        NSLog("Celestis: crash key $key=$value")
    }
}
