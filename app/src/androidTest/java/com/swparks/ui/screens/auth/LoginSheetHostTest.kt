package com.swparks.ui.screens.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.testing.TimeoutTest
import com.swparks.testing.tapOutside
import com.swparks.ui.state.LoginUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeLoginViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты хоста [LoginSheetHost]: способы закрытия модального листа.
 *
 * Проверяют поведение возврата (системная кнопка и жест возврата идут через
 * один обработчик ModalBottomSheet), тапа по затемнённой области и свайпа
 * листа вниз, в том числе во время логина (guard по `uiState.isBusy`).
 */
@RunWith(AndroidJUnit4::class)
class LoginSheetHostTest : TimeoutTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val loginOrEmailText = context.getString(R.string.login_or_email)

    /**
     * Настраивает хост с фейковой ViewModel на управляемом состоянии.
     *
     * @return изменяемое состояние ViewModel для переключения `isBusy` в тесте
     */
    private fun setContent(onDismissed: () -> Unit = {}): MutableStateFlow<LoginUiState> {
        val uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                LoginSheetHost(
                    show = true,
                    onDismissed = onDismissed,
                    onLoginSuccess = {},
                    viewModel = FakeLoginViewModel(uiState = uiState)
                )
            }
        }
        composeTestRule.waitForIdle()
        return uiState
    }

    /**
     * Закрывает лист двумя «возвратами»: LoginScreen запрашивает фокус в поле логина,
     * и первый «возврат» уходит на скрытие клавиатуры этого поля, второй — листу.
     */
    private fun pressBackClosingSheet() {
        pressBack()
        pressBack()
    }

    @Test
    fun loginSheetHost_onBackPressed_whenIdle_thenClosesSheetAndCallsOnDismissed() {
        // Given
        var dismissedCalled = false
        setContent(onDismissed = { dismissedCalled = true })
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText(loginOrEmailText, ignoreCase = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // When
        pressBackClosingSheet()
        composeTestRule.waitUntil(timeoutMillis = 10_000) { dismissedCalled }

        // Then - обратный вызов закрытия листа вызван
        assert(dismissedCalled) { "Callback onDismissed не был вызван после нажатия «назад»" }
    }

    @Test
    fun loginSheetHost_onBackPressed_whenBusy_thenSheetStaysOpen() {
        // Given
        var dismissedCalled = false
        val uiState = setContent(onDismissed = { dismissedCalled = true })
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText(loginOrEmailText, ignoreCase = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        uiState.value = LoginUiState.Loading
        composeTestRule.waitForIdle()

        // When
        pressBack()
        composeTestRule.waitForIdle()

        // Then - лист остаётся открытым, закрытие не вызывается
        composeTestRule
            .onNodeWithText(loginOrEmailText, ignoreCase = true)
            .assertIsDisplayed()
        assert(!dismissedCalled) { "Лист закрылся во время логина: onDismissed не должен вызываться" }
    }

    @Test
    fun loginSheetHost_onTapOutside_thenSheetStaysOpen() {
        // Given
        var dismissedCalled = false
        setContent(onDismissed = { dismissedCalled = true })
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText(loginOrEmailText, ignoreCase = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // When
        tapOutside()
        composeTestRule.waitForIdle()

        // Then - лист остаётся открытым
        composeTestRule
            .onNodeWithText(loginOrEmailText, ignoreCase = true)
            .assertIsDisplayed()
        assert(!dismissedCalled) { "Лист закрылся по тапу вне области: onDismissed не должен вызываться" }
    }

    @Test
    fun loginSheetHost_onSwipeDown_thenSheetStaysOpen() {
        // Given
        var dismissedCalled = false
        setContent(onDismissed = { dismissedCalled = true })
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText(loginOrEmailText, ignoreCase = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val labelNode = composeTestRule.onNodeWithText(loginOrEmailText, ignoreCase = true)
        labelNode.assertIsDisplayed()

        // When
        labelNode.performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        // Then - лист остаётся открытым
        composeTestRule
            .onNodeWithText(loginOrEmailText, ignoreCase = true)
            .assertIsDisplayed()
        assert(!dismissedCalled) { "Лист закрылся по свайпу вниз: onDismissed не должен вызываться" }
    }
}
