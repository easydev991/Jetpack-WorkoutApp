package com.swparks.analytics

import com.swparks.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AnalyticsServiceTest {
    private val logger: Logger = mockk(relaxed = true)
    private lateinit var service: AnalyticsService

    private val testEvent = AnalyticsEvent.ScreenView(AppScreen.LOGIN)

    @Before
    fun setUp() {
        service = AnalyticsService(emptyList(), logger)
    }

    @Test
    fun log_whenMultipleProviders_thenEventSentToAll() {
        val provider1: AnalyticsProvider = mockk(relaxed = true)
        val provider2: AnalyticsProvider = mockk(relaxed = true)

        val analyticsService = AnalyticsService(listOf(provider1, provider2), logger)
        analyticsService.log(testEvent)

        verify { provider1.log(testEvent) }
        verify { provider2.log(testEvent) }
    }

    @Test
    fun log_whenEmptyProvidersList_thenNoError() {
        service.log(testEvent)
    }

    @Test
    fun log_whenProviderThrows_thenOtherProvidersStillReceiveEvent() {
        val failingProvider: AnalyticsProvider =
            mockk {
                every { log(any()) } throws RuntimeException("Provider crashed")
            }
        val workingProvider: AnalyticsProvider = mockk(relaxed = true)

        val analyticsService =
            AnalyticsService(
                listOf(failingProvider, workingProvider),
                logger
            )
        analyticsService.log(testEvent)

        verify { workingProvider.log(testEvent) }
    }

    @Test
    fun log_whenProviderThrows_thenErrorLogged() {
        val error = RuntimeException("Provider crashed")
        val failingProvider: AnalyticsProvider =
            mockk {
                every { log(any()) } throws error
            }

        val analyticsService = AnalyticsService(listOf(failingProvider), logger)
        analyticsService.log(testEvent)

        verify {
            logger.e(
                "AnalyticsService",
                match { it.contains("Provider crashed") },
                error
            )
        }
    }

    @Test
    fun log_whenFirstProviderFails_thenSecondProviderStillReceivesEvent() {
        val error = IllegalStateException("fail")
        val provider1: AnalyticsProvider =
            mockk {
                every { log(any()) } throws error
            }
        val provider2: AnalyticsProvider = mockk(relaxed = true)

        val analyticsService = AnalyticsService(listOf(provider1, provider2), logger)
        analyticsService.log(testEvent)

        verify { provider1.log(testEvent) }
        verify { provider2.log(testEvent) }
        verify { logger.e("AnalyticsService", any(), error) }
    }
}
