package com.example.celestis.observability

import com.google.firebase.crashlytics.FirebaseCrashlytics

actual object CrashReporter {
    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    actual fun log(message: String) {
        crashlytics.log(message)
    }

    actual fun recordException(throwable: Throwable, message: String?) {
        message?.let(crashlytics::log)
        crashlytics.recordException(throwable)
    }

    actual fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
}
