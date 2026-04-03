package com.swparks.util

/**
 * Реализация Logger для Android-окружения.
 * Использует android.util.Log для реального логирования в приложении.
 */
class AndroidLogger : Logger {
    override fun d(
        tag: String,
        message: String
    ) {
        android.util.Log.d(tag, message)
    }

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        if (throwable != null) {
            android.util.Log.w(tag, message, throwable)
        } else {
            android.util.Log.w(tag, message)
        }
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        if (throwable != null) {
            android.util.Log.e(tag, message, throwable)
        } else {
            android.util.Log.e(tag, message)
        }
    }

    override fun i(
        tag: String,
        message: String
    ) {
        android.util.Log.i(tag, message)
    }
}
