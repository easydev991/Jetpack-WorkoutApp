package com.swparks.analytics

import com.swparks.util.Logger
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
    fun log_whenMultipleLoggers_thenEventSentToAll() {
        val events1 = mutableListOf<AnalyticsEvent>()
        val events2 = mutableListOf<AnalyticsEvent>()
        val logger1: (AnalyticsEvent) -> Unit = { events1.add(it) }
        val logger2: (AnalyticsEvent) -> Unit = { events2.add(it) }

        val analyticsService = AnalyticsService(listOf(logger1, logger2), logger)
        analyticsService.log(testEvent)

        assert(events1 == listOf(testEvent))
        assert(events2 == listOf(testEvent))
    }

    @Test
    fun log_whenEmptyLoggersList_thenNoError() {
        service.log(testEvent)
    }

    @Test
    fun log_whenLoggerThrows_thenOtherLoggersStillReceiveEvent() {
        val failingLogger: (AnalyticsEvent) -> Unit = { throw IllegalStateException("Provider crashed") }
        val received = mutableListOf<AnalyticsEvent>()
        val workingLogger: (AnalyticsEvent) -> Unit = { received.add(it) }

        val analyticsService =
            AnalyticsService(
                listOf(failingLogger, workingLogger),
                logger
            )
        analyticsService.log(testEvent)

        assert(received == listOf(testEvent))
    }

    @Test
    fun log_whenLoggerThrows_thenErrorLogged() {
        val error = IllegalStateException("Provider crashed")
        val failingLogger: (AnalyticsEvent) -> Unit = { throw error }

        val analyticsService = AnalyticsService(listOf(failingLogger), logger)
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
    fun log_whenFirstLoggerFails_thenSecondLoggerStillReceivesEvent() {
        val error = IllegalStateException("fail")
        val failingLogger: (AnalyticsEvent) -> Unit = { throw error }
        val received = mutableListOf<AnalyticsEvent>()
        val workingLogger: (AnalyticsEvent) -> Unit = { received.add(it) }

        val analyticsService = AnalyticsService(listOf(failingLogger, workingLogger), logger)
        analyticsService.log(testEvent)

        assert(received == listOf(testEvent))
        verify { logger.e("AnalyticsService", any(), error) }
    }
}
