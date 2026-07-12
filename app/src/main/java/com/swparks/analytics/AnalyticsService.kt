package com.swparks.analytics

import com.swparks.util.Logger

class AnalyticsService(
    private val loggers: List<(AnalyticsEvent) -> Unit>,
    private val logger: Logger
) {
    @Suppress("TooGenericExceptionCaught")
    fun log(event: AnalyticsEvent) {
        for ((index, logAction) in loggers.withIndex()) {
            try {
                logAction(event)
            } catch (e: Exception) {
                logger.e(
                    TAG,
                    "Ошибка в логгере #${index + 1}: ${e.message}",
                    e
                )
            }
        }
    }

    private companion object {
        private const val TAG = "AnalyticsService"
    }
}
