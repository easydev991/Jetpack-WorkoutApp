package com.swparks.ui.screens.more

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Компонентные тесты для [MoreScreen].
 *
 * Проверяет наличие всех кнопок на экране, корректность отображения
 * версии приложения и функциональность кнопок.
 */
@RunWith(AndroidJUnit4::class)
class MoreScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent() {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                MoreScreen()
            }
        }
    }

    @Test
    fun moreScreen_whenDisplayed_thenShowsThemeAndIconButton() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.appearance), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun moreScreen_whenDisplayed_thenShowsSendFeedbackButton() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.send_feedback), ignoreCase = true)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun moreScreen_whenDisplayed_thenShowsRateAppButton() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.rate_app), ignoreCase = true)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun moreScreen_whenDisplayed_thenShowsShareAppButton() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.share_the_app), ignoreCase = true)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun moreScreen_whenDisplayed_thenShowsOfficialSiteButton() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.official_site), ignoreCase = true)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun moreScreen_whenDisplayed_thenShowsAppDeveloperButton() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_developer), ignoreCase = true)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun moreScreen_whenDisplayed_thenShowsDaysCounterAppButton() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.days_counter_app), ignoreCase = true)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun moreScreen_whenDisplayed_thenShowsGitHubPageButton() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.github_page), ignoreCase = true)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun moreScreen_whenDisplayed_thenShowsAppVersion() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_version), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun moreScreen_whenDisplayed_thenShowsMoreContent() {
        // When
        setContent()

        // Then - Проверяем отображение содержимого экрана (без AppBar, который находится в RootScreen)
        // Секция "Settings"
        composeTestRule
            .onNodeWithText(context.getString(R.string.settings), ignoreCase = true)
            .assertIsDisplayed()

        // Кнопка "App Theme and Icon"
        composeTestRule
            .onNodeWithText(context.getString(R.string.appearance), ignoreCase = true)
            .assertIsDisplayed()

        // Секция "About App"
        composeTestRule
            .onNodeWithText(context.getString(R.string.about_app), ignoreCase = true)
            .assertIsDisplayed()

        // Секция "Other Apps"
        composeTestRule
            .onNodeWithText(context.getString(R.string.other_apps), ignoreCase = true)
            .assertIsDisplayed()

        // Секция "Support Project"
        composeTestRule
            .onNodeWithText(context.getString(R.string.support_project), ignoreCase = true)
            .assertIsDisplayed()
    }
}
