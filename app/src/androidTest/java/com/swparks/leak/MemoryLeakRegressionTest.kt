package com.swparks.leak

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.analytics.AnalyticsService
import com.swparks.analytics.fakeAnalyticsLogger
import com.swparks.navigation.Screen
import com.swparks.navigation.rememberAppState
import com.swparks.testing.TimeoutTest
import com.swparks.ui.screens.RootScreen
import com.swparks.util.Logger
import io.mockk.mockk
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Регрессия утечек памяти: циклы по критичным экранам через реальный [RootScreen].
 *
 * Валит прогон, если после теста LeakCanary нашел подтвержденные утечки
 * ([DetectLeaksAfterTestSuccess]). Правило объявлено внешним относительно compose-правила
 * (`order` меньше), поэтому проверка утечек идет ПОСЛЕ уничтожения композиции.
 *
 * Важно: нельзя хранить `NavHostController` в поле теста — он удерживает уже уничтоженную
 * Activity, и LeakCanary справедливо считает это утечкой приложения.
 *
 * Запуск: `make memory-check`.
 */
@RunWith(AndroidJUnit4::class)
class MemoryLeakRegressionTest : TimeoutTest(LEAK_CHECK_TIMEOUT_SECONDS) {
    @get:Rule(order = 1)
    val noLeaksRule = DetectLeaksAfterTestSuccess()

    @get:Rule(order = 2)
    val composeTestRule = createComposeRule()

    @Test
    fun rootScreen_whenCyclingBottomNavTabs_thenNoLeaks() {
        composeTestRule.setContent {
            RootScreen(appState = rememberAppState(analyticsService = analyticsService))
        }
        composeTestRule.waitForIdle()

        repeat(CYCLES) {
            tabTexts.forEach { tabText ->
                composeTestRule.onNodeWithText(tabText).performClick()
                composeTestRule.waitForIdle()
            }
        }
    }

    @Test
    fun rootScreen_whenOpeningParkDetailAndGoingBack_thenNoLeaks() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            RootScreen(appState = rememberAppState(navController = navController, analyticsService = analyticsService))
        }
        composeTestRule.waitForIdle()

        repeat(CYCLES) {
            composeTestRule.runOnIdle {
                navController.navigate(Screen.ParkDetail.createRoute(parkId = 1L))
            }
            composeTestRule.waitForIdle()

            composeTestRule.runOnIdle {
                navController.popBackStack()
            }
            composeTestRule.waitForIdle()
        }
    }

    private val analyticsService =
        AnalyticsService(listOf(fakeAnalyticsLogger()), mockk<Logger>(relaxed = true))

    private val tabTexts: List<String> =
        listOf(R.string.parks, R.string.events, R.string.messages, R.string.profile, R.string.more)
            .map { InstrumentationRegistry.getInstrumentation().targetContext.getString(it) }

    private companion object {
        /** Анализ heap занимает десятки секунд — обычных 60 с тестам не хватает. */
        const val LEAK_CHECK_TIMEOUT_SECONDS = 300L
        const val CYCLES = 15
    }
}
