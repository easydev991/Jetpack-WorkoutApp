package com.swparks.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.swparks.util.CrashReporter
import com.swparks.util.Logger

class FirebaseAnalyticsProvider(
    context: Context,
    private val logger: Logger,
    private val crashReporter: CrashReporter
) : AnalyticsProvider {
    private val appContext = context.applicationContext

    @Suppress("TooGenericExceptionCaught")
    override fun log(event: AnalyticsEvent) {
        try {
            when (event) {
                is AnalyticsEvent.ScreenView -> logScreenView(event)
                is AnalyticsEvent.UserAction -> logUserAction(event)
                is AnalyticsEvent.AppError -> logAppError(event)
            }
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка отправки события ${event::class.simpleName}", e)
            crashReporter.logException(e, "Ошибка Firebase Analytics")
        }
    }

    private fun logScreenView(event: AnalyticsEvent.ScreenView) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(appContext)
        val params =
            Bundle().apply {
                putString(PARAM_SCREEN_NAME, event.screen.screenName)
                event.source?.let { putString(PARAM_SOURCE, it.screenName) }
            }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
        logger.d(TAG, "Отправлен screen_view: ${event.screen.screenName}")
    }

    private fun logUserAction(event: AnalyticsEvent.UserAction) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(appContext)
        val params =
            Bundle().apply {
                putString(PARAM_ACTION, event.action.value)
                event.params.forEach { (key, value) ->
                    putString(key, value)
                }
            }
        firebaseAnalytics.logEvent(EVENT_USER_ACTION, params)
        logger.d(TAG, "Отправлен user_action: ${event.action.value}")
    }

    private fun logAppError(event: AnalyticsEvent.AppError) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(appContext)
        val params =
            Bundle().apply {
                putString(PARAM_OPERATION, event.operation.value)
                putString(PARAM_ERROR_DOMAIN, event.throwable::class.java.name)
                putLong(PARAM_ERROR_CODE, event.throwable.hashCode().toLong())
            }
        firebaseAnalytics.logEvent(EVENT_APP_ERROR, params)
        crashReporter.logException(event.throwable, event.operation.value)
        logger.d(TAG, "Отправлен app_error: ${event.operation.value}")
    }

    private companion object {
        private const val TAG = "FirebaseAnalytics"
        private const val PARAM_SCREEN_NAME = "screen_name"
        private const val PARAM_SOURCE = "source"
        private const val PARAM_ACTION = "action"
        private const val PARAM_OPERATION = "operation"
        private const val PARAM_ERROR_DOMAIN = "error_domain"
        private const val PARAM_ERROR_CODE = "error_code"
        private const val EVENT_USER_ACTION = "user_action"
        private const val EVENT_APP_ERROR = "app_error"
    }
}
