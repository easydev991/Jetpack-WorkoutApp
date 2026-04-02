package com.swparks.util.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.swparks.util.AnalyticsReporter
import com.swparks.util.CrashReporter
import com.swparks.util.Logger

/**
 * Реализация AnalyticsReporter на базе Firebase Analytics.
 */
class FirebaseAnalyticsReporter(
    context: Context,
    private val logger: Logger,
    private val crashReporter: CrashReporter,
) : AnalyticsReporter {
    private val appContext = context.applicationContext

    @Suppress("TooGenericExceptionCaught")
    override fun logScreenView(screenName: String, screenClass: String?) {
        try {
            val firebaseAnalytics = FirebaseAnalytics.getInstance(appContext)
            val params =
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
                }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
            logger.d(TAG, "Отправлен screen_view: $screenName")
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка отправки screen_view", e)
            crashReporter.logException(e, "Ошибка Firebase Analytics screen_view")
        }
    }

    private companion object {
        private const val TAG = "FirebaseAnalytics"
    }
}
