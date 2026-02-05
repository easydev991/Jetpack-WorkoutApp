package com.swparks.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swparks.data.AppContainer
import com.swparks.model.AppError
import com.swparks.navigation.rememberAppState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Инструментальные тесты для RootScreen.
 *
 * Проверяют отображение Snackbar при возникновении ошибок.
 */
@RunWith(AndroidJUnit4::class)
class RootScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var appContainer: AppContainer

    @Before
    fun setup() {
        // Получаем Application для использования того же контейнера, что и в RootScreen
        val application =
            ApplicationProvider.getApplicationContext<com.swparks.JetpackWorkoutApplication>()
        appContainer = application.container
    }

    @Test
    fun snackbarShowsWhenNetworkErrorOccurs() {
        // Given - настраиваем UI
        composeTestRule.setContent {
            RootScreen(
                appState = rememberAppState()
            )
        }

        // Ждем, когда UI будет готов
        composeTestRule.waitForIdle()

        // When - отправляем ошибку через ErrorReporter
        val errorMessage = "Нет подключения к интернету"
        appContainer.errorReporter.handleError(
            AppError.Network(message = errorMessage)
        )

        // Ждем, когда Snackbar появится (даем время на анимацию и отрисовку)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(errorMessage).fetchSemanticsNodes().isNotEmpty()
        }

        // Then - проверяем, что Snackbar отображается с правильным сообщением
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun snackbarShowsWhenValidationErrorOccurs() {
        // Given - настраиваем UI
        composeTestRule.setContent {
            RootScreen(
                appState = rememberAppState()
            )
        }

        // Ждем, когда UI будет готов
        composeTestRule.waitForIdle()

        // When - отправляем ошибку валидации через ErrorReporter
        val errorMessage = "Пароль слишком короткий"
        appContainer.errorReporter.handleError(
            AppError.Validation(message = errorMessage, field = "password")
        )

        // Ждем, когда Snackbar появится (даем время на анимацию и отрисовку)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(errorMessage).fetchSemanticsNodes().isNotEmpty()
        }

        // Then - проверяем, что Snackbar отображается с правильным сообщением
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun snackbarShowsWhenServerErrorOccurs() {
        // Given - настраиваем UI
        composeTestRule.setContent {
            RootScreen(
                appState = rememberAppState()
            )
        }

        // Ждем, когда UI будет готов
        composeTestRule.waitForIdle()

        // When - отправляем ошибку сервера через ErrorReporter
        val errorMessage = "Ошибка сервера 500"
        appContainer.errorReporter.handleError(
            AppError.Server(message = errorMessage, code = 500)
        )

        // Ждем, когда Snackbar появится (даем время на анимацию и отрисовку)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(errorMessage).fetchSemanticsNodes().isNotEmpty()
        }

        // Then - проверяем, что Snackbar отображается с правильным сообщением
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun snackbarShowsWhenGenericErrorOccurs() {
        // Given - настраиваем UI
        composeTestRule.setContent {
            RootScreen(
                appState = rememberAppState()
            )
        }

        // Ждем, когда UI будет готов
        composeTestRule.waitForIdle()

        // When - отправляем общую ошибку через ErrorReporter
        val errorMessage = "Произошла непредвиденная ошибка"
        appContainer.errorReporter.handleError(
            AppError.Generic(message = errorMessage)
        )

        // Ждем, когда Snackbar появится (даем время на анимацию и отрисовку)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(errorMessage).fetchSemanticsNodes().isNotEmpty()
        }

        // Then - проверяем, что Snackbar отображается с правильным сообщением
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }
}
