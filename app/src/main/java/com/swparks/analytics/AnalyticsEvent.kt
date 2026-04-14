package com.swparks.analytics

sealed interface AnalyticsEvent {
    data class ScreenView(
        val screen: AppScreen,
        val source: AppScreen? = null
    ) : AnalyticsEvent

    data class UserAction(
        val action: UserActionType,
        val params: Map<String, String> = emptyMap()
    ) : AnalyticsEvent

    data class AppError(
        val operation: AppErrorOperation,
        val throwable: Throwable
    ) : AnalyticsEvent
}
