package com.swparks.screenshots

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.swparks.JetpackWorkoutApplication

@RunWith(AndroidJUnit4::class)
class ScreenshotAppBootstrapTest {
    @Test
    fun screenshotRunner_usesScreenshotTestApplication() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()

        assertTrue(
            "Application must inherit JetpackWorkoutApplication for RootScreen cast",
            app is JetpackWorkoutApplication
        )

        assertTrue(
            "Instrumentation must bootstrap ScreenshotTestApplication",
            app is ScreenshotTestApplication
        )
    }

    @Test
    fun screenshotContainer_providesDeterministicDemoData() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
            as JetpackWorkoutApplication
        val container = app.container

        val parks = container.swRepository.getParksFlow().first()
        assertFalse("Demo parks must not be empty", parks.isEmpty())
        assertTrue(
            "Scenario park id must exist in demo data",
            parks.any { it.id == ScreenshotScenarioState.PARK_DETAIL_ID }
        )

        val pastEvents = container.getPastEventsFlowUseCase().first()
        assertFalse("Demo past events must not be empty", pastEvents.isEmpty())
        assertTrue(
            "Scenario event id must exist in demo data",
            pastEvents.any { it.id == ScreenshotScenarioState.EVENT_DETAIL_ID }
        )

        val city = container.countriesRepository.getCityById("1")
        assertEquals("Москва", city?.name)
    }
}
