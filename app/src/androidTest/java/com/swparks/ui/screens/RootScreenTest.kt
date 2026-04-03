package com.swparks.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swparks.R
import com.swparks.data.AppContainer
import com.swparks.navigation.rememberAppState
import com.swparks.util.AppError
import com.swparks.util.FakeAnalyticsReporter
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

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
    private lateinit var context: Context

    @Before
    fun setup() {
        // Получаем Application для использования того же контейнера, что и в RootScreen
        val application =
            ApplicationProvider.getApplicationContext<com.swparks.JetpackWorkoutApplication>()
        appContainer = application.container
        context = application
    }

    @Test
    fun snackbarShowsWhenNetworkErrorOccurs() {
        // Given - настраиваем UI
        composeTestRule.setContent {
            RootScreen(
                appState = rememberAppState(analyticsReporter = FakeAnalyticsReporter())
            )
        }

        // Ждем, когда UI будет готов
        composeTestRule.waitForIdle()

        // When - отправляем ошибку через ErrorReporter (с IOException для получения конкретного сообщения)
        appContainer.userNotifier.handleError(
            AppError.Network(message = "", throwable = IOException())
        )

        // Ожидаемое сообщение из строковых ресурсов
        val expectedMessage = context.getString(R.string.error_network_io)

        // Ждем, когда Snackbar появится (даем время на анимацию и отрисовку)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(expectedMessage).fetchSemanticsNodes().isNotEmpty()
        }

        // Then - проверяем, что Snackbar отображается с правильным сообщением
        composeTestRule
            .onNodeWithText(expectedMessage)
            .assertIsDisplayed()
    }

    @Test
    fun snackbarShowsWhenValidationErrorOccurs() {
        // Given - настраиваем UI
        composeTestRule.setContent {
            RootScreen(
                appState = rememberAppState(analyticsReporter = FakeAnalyticsReporter())
            )
        }

        // Ждем, когда UI будет готов
        composeTestRule.waitForIdle()

        // When - отправляем ошибку валидации через ErrorReporter
        appContainer.userNotifier.handleError(
            AppError.Validation(message = "", field = "password")
        )

        // Ожидаемое сообщение из строковых ресурсов
        val expectedMessage = context.getString(R.string.error_validation_password)

        // Ждем, когда Snackbar появится (даем время на анимацию и отрисовку)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(expectedMessage).fetchSemanticsNodes().isNotEmpty()
        }

        // Then - проверяем, что Snackbar отображается с правильным сообщением
        composeTestRule
            .onNodeWithText(expectedMessage)
            .assertIsDisplayed()
    }

    @Test
    fun snackbarShowsWhenServerErrorOccurs() {
        // Given - настраиваем UI
        composeTestRule.setContent {
            RootScreen(
                appState = rememberAppState(analyticsReporter = FakeAnalyticsReporter())
            )
        }
        // Ждем, когда UI будет готов
        composeTestRule.waitForIdle()

        // When - отправляем ошибку сервера через ErrorReporter
        appContainer.userNotifier.handleError(
            AppError.Server(message = "", code = 500)
        )

        // Ожидаемое сообщение из строковых ресурсов
        val expectedMessage = context.getString(R.string.error_server_internal)

        // Ждем, когда Snackbar появится (даем время на анимацию и отрисовку)
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(expectedMessage).fetchSemanticsNodes().isNotEmpty()
        }

        // Then - проверяем, что Snackbar отображается с правильным сообщением
        composeTestRule
            .onNodeWithText(expectedMessage)
            .assertIsDisplayed()
    }

    @Test
    fun snackbarShowsWhenGenericErrorOccurs() {
        // Given - настраиваем UI
        composeTestRule.setContent {
            RootScreen(
                appState = rememberAppState(analyticsReporter = FakeAnalyticsReporter())
            )
        }

        // Ждем, когда UI будет готов
        composeTestRule.waitForIdle()

        // When - отправляем общую ошибку через ErrorReporter
        val errorMessage = "An unexpected error occurred"
        appContainer.userNotifier.handleError(
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
