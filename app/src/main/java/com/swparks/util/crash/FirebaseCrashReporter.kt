package com.swparks.util.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.swparks.util.AndroidLogger
import com.swparks.util.CrashReporter
import com.swparks.util.Logger

/**
 * CrashReporter на основе Firebase Crashlytics.
 * Реализует паттерн singleton через object.
 * Все вызовы Firebase оборачиваются в try/catch для fail-safe поведения.
 */
object FirebaseCrashReporter : CrashReporter {
    private val logger: Logger = AndroidLogger()

    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    override fun logException(
        exception: Throwable,
        message: String?
    ) {
        try {
            message?.let {
                crashlytics.setCustomKey("error_message", it)
            }
            crashlytics.recordException(exception)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception
        ) {
            logger.e("FirebaseCrashReporter", "Не удалось записать исключение в Crashlytics", e)
        }
    }

    override fun setUserId(userId: String?) {
        try {
            if (userId != null) {
                crashlytics.setUserId(userId)
            } else {
                crashlytics.setUserId("")
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception
        ) {
            logger.e("FirebaseCrashReporter", "Не удалось установить userId в Crashlytics", e)
        }
    }

    override fun setCustomKey(
        key: String,
        value: Any
    ) {
        try {
            when (value) {
                is String -> crashlytics.setCustomKey(key, value)
                is Int -> crashlytics.setCustomKey(key, value)
                is Boolean -> crashlytics.setCustomKey(key, value)
                is Long -> crashlytics.setCustomKey(key, value)
                is Double -> crashlytics.setCustomKey(key, value)
                is Float -> crashlytics.setCustomKey(key, value)
                else -> crashlytics.setCustomKey(key, value.toString())
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception
        ) {
            logger.e("FirebaseCrashReporter", "Не удалось установить ключ $key в Crashlytics", e)
        }
    }

    override fun log(message: String) {
        try {
            crashlytics.log(message)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception
        ) {
            logger.e("FirebaseCrashReporter", "Не удалось записать лог в Crashlytics", e)
        }
    }
}
