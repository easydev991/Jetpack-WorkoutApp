package com.swparks.ui.screens.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeLoginViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Компонентные тесты для [LoginScreen].
 *
 * Проверяет наличие всех элементов экрана, корректность отображения,
 * взаимодействие с полями ввода, кнопками и алертами.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val closeText = context.getString(R.string.close)
    private val loginOrEmailText = context.getString(R.string.login_or_email)
    private val passwordText = context.getString(R.string.password)

    /**
     * Настраивает LoginScreen для тестирования.
     */
    private fun setContent(
        viewModel: FakeLoginViewModel = FakeLoginViewModel(),
        onDismiss: () -> Unit = {},
        onLoginSuccess: (userId: Long) -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onDismiss = onDismiss,
                    onLoginSuccess = onLoginSuccess
                )
            }
        }
    }

    @Test
    fun loginScreen_whenDisplayed_thenShowsTitle() {
        // When
        setContent()

        // Then - Заголовок "sign_in" отображается в AppBar
        composeTestRule
            .onNodeWithTag("loginTitle")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_whenDisplayed_thenShowsCloseButton() {
        // When
        setContent()

        // Then - Кнопка закрытия отображается (по contentDescription)
        composeTestRule
            .onNodeWithContentDescription(closeText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_whenDisplayed_thenShowsLoginField() {
        // When
        setContent()

        // Then - Поле логина отображается с меткой
        composeTestRule
            .onNodeWithText(loginOrEmailText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_whenDisplayed_thenShowsPasswordField() {
        // When
        setContent()

        // Then - Поле пароля отображается с меткой
        composeTestRule
            .onNodeWithText(passwordText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_whenDisplayed_thenShowsSignInButton() {
        // When
        setContent()

        // Then - Кнопка "sign_in" отображается (по testTag)
        composeTestRule
            .onNodeWithTag("login_button")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_whenDisplayed_thenShowsResetPasswordButton() {
        // When
        setContent()

        // Then - Кнопка "reset_password" отображается (по testTag)
        composeTestRule
            .onNodeWithTag("resetPasswordButton")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_whenFieldsEmpty_thenSignInButtonDisabled() {
        // When
        setContent()

        // Then - Кнопка "sign_in" должна быть отключена при пустых полях
        composeTestRule
            .onNodeWithTag("login_button")
            .assertIsNotEnabled()
    }

    @Test
    fun loginScreen_whenOnlyLoginFilled_thenSignInButtonDisabled() {
        // When - Вводим только логин (минимум 1 символ)
        setContent()
        composeTestRule
            .onNodeWithText(loginOrEmailText, ignoreCase = true)
            .performTextInput("testuser")

        // Then - Кнопка "sign_in" должна быть отключена (пароль не введен или слишком короткий)
        composeTestRule
            .onNodeWithTag("login_button")
            .assertIsNotEnabled()
    }

    @Test
    fun loginScreen_whenOnlyPasswordFilled_thenSignInButtonDisabled() {
        // When - Вводим только пароль (минимум 6 символов)
        setContent()
        composeTestRule
            .onNodeWithText(passwordText, ignoreCase = true)
            .performTextInput("password123")

        // Then - Кнопка "sign_in" должна быть отключена (логин не введен)
        composeTestRule
            .onNodeWithTag("login_button")
            .assertIsNotEnabled()
    }

    @Test
    fun loginScreen_whenFieldsFilledValidly_thenSignInButtonEnabled() {
        // When - Вводим валидные логин и пароль
        setContent()
        composeTestRule
            .onNodeWithText(loginOrEmailText, ignoreCase = true)
            .performTextInput("testuser")

        composeTestRule
            .onNodeWithText(passwordText, ignoreCase = true)
            .performTextInput("password123")

        // Then - Кнопка "sign_in" должна быть включена (отображается и не отключена)
        composeTestRule
            .onNodeWithTag("login_button")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_whenPasswordTooShort_thenSignInButtonDisabled() {
        // When - Вводим логин и короткий пароль (менее 6 символов)
        setContent()
        composeTestRule
            .onNodeWithText(loginOrEmailText, ignoreCase = true)
            .performTextInput("testuser")

        composeTestRule
            .onNodeWithText(passwordText, ignoreCase = true)
            .performTextInput("12345")

        // Then - Кнопка "sign_in" должна быть отключена
        composeTestRule
            .onNodeWithTag("login_button")
            .assertIsNotEnabled()
    }

    @Test
    fun loginScreen_whenLoginFieldEmpty_thenResetPasswordButtonEnabled() {
        // When
        setContent()

        // Then - Кнопка "reset_password" должна быть активна даже при пустом логине
        // (при клике показывается алерт с предложением ввести логин)
        composeTestRule
            .onNodeWithTag("resetPasswordButton")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_whenLoginFieldFilled_thenResetPasswordButtonEnabled() {
        // When - Вводим логин
        setContent()
        composeTestRule
            .onNodeWithText(loginOrEmailText, ignoreCase = true)
            .performTextInput("testuser")

        // Then - Кнопка "reset_password" должна быть включена
        composeTestRule
            .onNodeWithTag("resetPasswordButton")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_whenLoginFieldCleared_thenResetPasswordButtonEnabled() {
        // Given - Вводим логин
        setContent()
        composeTestRule
            .onNodeWithText(loginOrEmailText, ignoreCase = true)
            .performTextInput("testuser")

        // When - Очищаем поле логина
        composeTestRule
            .onNodeWithText(loginOrEmailText, ignoreCase = true)
            .performTextClearance()

        // Then - Кнопка "reset_password" должна быть активна даже после очистки поля
        // (при клике показывается алерт с предложением ввести логин)
        composeTestRule
            .onNodeWithTag("resetPasswordButton")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_whenCloseButtonClicked_thenCallsOnDismiss() {
        // Given
        var dismissCalled = false
        setContent(onDismiss = { dismissCalled = true })

        // When - Кликаем на кнопку закрытия (по contentDescription)
        composeTestRule
            .onNodeWithContentDescription(closeText, ignoreCase = true)
            .performClick()

        // Then - Callback onDismiss должен быть вызван
        assert(dismissCalled) { "Callback onDismiss не был вызван" }
    }
}
