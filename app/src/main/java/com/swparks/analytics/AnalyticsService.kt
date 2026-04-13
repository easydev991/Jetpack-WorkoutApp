package com.swparks.analytics

import com.swparks.util.Logger

class AnalyticsService(
    private val providers: List<AnalyticsProvider>,
    private val logger: Logger
) {
    @Suppress("TooGenericExceptionCaught")
    fun log(event: AnalyticsEvent) {
        for (provider in providers) {
            try {
                provider.log(event)
            } catch (e: Exception) {
                logger.e(
                    TAG,
                    "Ошибка в провайдере ${provider::class.simpleName}: ${e.message}",
                    e
                )
            }
        }
    }

    private companion object {
        private const val TAG = "AnalyticsService"
    }
}
