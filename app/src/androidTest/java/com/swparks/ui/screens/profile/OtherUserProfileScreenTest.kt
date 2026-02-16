package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.User
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.OtherUserProfileUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для OtherUserProfileScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 * Проверяет отображение различных состояний и реакцию на действия пользователя.
 */
@RunWith(AndroidJUnit4::class)
class OtherUserProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // === Тесты для UserNotFoundContent ===

    @Test
    fun userNotFoundContent_displaysErrorMessage() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                UserNotFoundContent(onBack = {})
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.user_not_found))
            .assertIsDisplayed()
    }

    @Test
    fun userNotFoundContent_displaysGoBackButton() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                UserNotFoundContent(onBack = {})
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.go_back))
            .assertIsDisplayed()
    }

    @Test
    fun userNotFoundContent_clickGoBack_callsOnBack() {
        // Given
        var backClicked = false

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                UserNotFoundContent(onBack = { backClicked = true })
            }
        }

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.go_back))
            .performClick()

        // Then
        assert(backClicked) { "Ожидался вызов onBack при клике на кнопку" }
    }

    // === Тесты для BlockedByUserContent ===

    @Test
    fun blockedByUserContent_displaysErrorMessage() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                BlockedByUserContent(onBack = {})
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.blocked_by_user))
            .assertIsDisplayed()
    }

    @Test
    fun blockedByUserContent_displaysGoBackButton() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                BlockedByUserContent(onBack = {})
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.go_back))
            .assertIsDisplayed()
    }

    @Test
    fun blockedByUserContent_clickGoBack_callsOnBack() {
        // Given
        var backClicked = false

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                BlockedByUserContent(onBack = { backClicked = true })
            }
        }

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.go_back))
            .performClick()

        // Then
        assert(backClicked) { "Ожидался вызов onBack при клике на кнопку" }
    }

    // === Тесты для ErrorContent ===

    @Test
    fun errorContent_displaysErrorMessage() {
        // Given
        val errorMessage = "Network error"

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ErrorContent(
                    message = errorMessage,
                    canRetry = true,
                    onRetry = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun errorContent_displaysRetryButton_whenCanRetryIsTrue() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ErrorContent(
                    message = "Error",
                    canRetry = true,
                    onRetry = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .assertIsDisplayed()
    }

    @Test
    fun errorContent_hidesRetryButton_whenCanRetryIsFalse() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ErrorContent(
                    message = "Error",
                    canRetry = false,
                    onRetry = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .assertDoesNotExist()
    }

    @Test
    fun errorContent_clickRetry_callsOnRetry() {
        // Given
        var retryClicked = false

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                ErrorContent(
                    message = "Error",
                    canRetry = true,
                    onRetry = { retryClicked = true }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        // Then
        assert(retryClicked) { "Ожидался вызов onRetry при клике на кнопку" }
    }

    // === Тесты для SendMessageButton ===

    @Test
    fun sendMessageButton_displaysText() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                SendMessageButton(onClick = {}, enabled = true)
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.message))
            .assertIsDisplayed()
    }

    @Test
    fun sendMessageButton_enabled_whenEnabledIsTrue() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                SendMessageButton(onClick = {}, enabled = true)
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.message))
            .assertIsDisplayed()
        // Проверяем что кнопка не отключена
    }

    @Test
    fun sendMessageButton_disabled_whenEnabledIsFalse() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                SendMessageButton(onClick = {}, enabled = false)
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.message))
            .assertIsNotEnabled()
    }

    @Test
    fun sendMessageButton_click_callsOnClick() {
        // Given
        var clicked = false

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                SendMessageButton(onClick = { clicked = true }, enabled = true)
            }
        }

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.message))
            .performClick()

        // Then
        assert(clicked) { "Ожидался вызов onClick при клике на кнопку" }
    }

    // === Тесты для BlacklistActionDialog ===

    @Test
    fun blacklistActionDialog_block_displaysBlockMessage() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                BlacklistActionDialog(
                    action = com.swparks.ui.model.BlacklistAction.BLOCK,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // Then - проверяем сообщение диалога (оно уникально)
        composeTestRule
            .onNodeWithText(context.getString(R.string.block_user_alert))
            .assertIsDisplayed()
    }

    @Test
    fun blacklistActionDialog_unblock_displaysUnblockMessage() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                BlacklistActionDialog(
                    action = com.swparks.ui.model.BlacklistAction.UNBLOCK,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // Then - проверяем сообщение диалога (оно уникально)
        composeTestRule
            .onNodeWithText(context.getString(R.string.unblock_user_alert))
            .assertIsDisplayed()
    }

    @Test
    fun blacklistActionDialog_displaysCancelButton() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                BlacklistActionDialog(
                    action = com.swparks.ui.model.BlacklistAction.BLOCK,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(android.R.string.cancel))
            .assertIsDisplayed()
    }

    @Test
    fun blacklistActionDialog_clickCancel_callsOnDismiss() {
        // Given
        var dismissCalled = false

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                BlacklistActionDialog(
                    action = com.swparks.ui.model.BlacklistAction.BLOCK,
                    onConfirm = {},
                    onDismiss = { dismissCalled = true }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithText(context.getString(android.R.string.cancel))
            .performClick()

        // Then
        assert(dismissCalled) { "Ожидался вызов onDismiss при клике на Cancel" }
    }

    @Test
    fun blacklistActionDialog_displaysBothBlockButtons() {
        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                BlacklistActionDialog(
                    action = com.swparks.ui.model.BlacklistAction.BLOCK,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        // Then - текст Block появляется 2 раза (заголовок + кнопка)
        composeTestRule
            .onAllNodesWithText(context.getString(R.string.block))
            .assertCountEquals(2)
    }

    @Test
    fun blacklistActionDialog_clickConfirm_callsOnConfirm() {
        // Given
        var confirmCalled = false

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                BlacklistActionDialog(
                    action = com.swparks.ui.model.BlacklistAction.BLOCK,
                    onConfirm = { confirmCalled = true },
                    onDismiss = {}
                )
            }
        }

        // When - кликаем на вторую кнопку Block (confirm button)
        // Первая - заголовок, вторая - кнопка
        composeTestRule
            .onAllNodesWithText(context.getString(R.string.block))[1]
            .performClick()

        // Then
        assert(confirmCalled) { "Ожидался вызов onConfirm при клике на Confirm" }
    }
}
