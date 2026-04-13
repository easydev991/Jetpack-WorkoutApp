package com.swparks.analytics

import org.junit.Before
import org.junit.Test

class NoopAnalyticsProviderTest {
    private lateinit var provider: NoopAnalyticsProvider

    @Before
    fun setUp() {
        provider = NoopAnalyticsProvider()
    }

    @Test
    fun log_whenScreenView_thenNoException() {
        provider.log(AnalyticsEvent.ScreenView(AppScreen.LOGIN))
    }

    @Test
    fun log_whenScreenViewWithSource_thenNoException() {
        provider.log(
            AnalyticsEvent.ScreenView(
                screen = AppScreen.PARK_DETAIL,
                source = AppScreen.PARKS_MAP
            )
        )
    }

    @Test
    fun log_whenUserAction_thenNoException() {
        provider.log(
            AnalyticsEvent.UserAction(
                action = UserActionType.LOGIN,
                params = mapOf("method" to "email")
            )
        )
    }

    @Test
    fun log_whenUserActionWithoutParams_thenNoException() {
        provider.log(AnalyticsEvent.UserAction(UserActionType.LOGOUT))
    }

    @Test
    fun log_whenAppError_thenNoException() {
        provider.log(
            AnalyticsEvent.AppError(
                operation = AppErrorOperation.LOGIN_FAILED,
                throwable = RuntimeException("test")
            )
        )
    }
}
