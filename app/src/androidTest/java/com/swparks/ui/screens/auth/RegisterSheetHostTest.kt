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
import com.swparks.ui.state.RegisterUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeRegisterViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты хоста [RegisterSheetHost]: способы закрытия модального листа.
 *
 * Проверяют поведение возврата (системная кнопка и жест возврата идут через
 * один обработчик ModalBottomSheet), тапа по затемнённой области и свайпа
 * листа вниз, в том числе во время регистрации (guard по `uiState.isBusy`).
 */
@RunWith(AndroidJUnit4::class)
class RegisterSheetHostTest : TimeoutTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val registerTitle = context.getString(R.string.registration)

    /**
     * Настраивает хост с фейковой ViewModel на управляемом состоянии.
     *
     * @return изменяемое состояние ViewModel для переключения `isBusy` в тесте
     */
    private fun setContent(onDismissed: () -> Unit = {}): MutableStateFlow<RegisterUiState> {
        val uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                RegisterSheetHost(
                    show = true,
                    onDismissed = onDismissed,
                    onRegisterSuccess = {},
                    viewModel = FakeRegisterViewModel(uiState = uiState)
                )
            }
        }
        composeTestRule.waitForIdle()
        return uiState
    }

    @Test
    fun registerSheetHost_onBackPressed_whenIdle_thenClosesSheetAndCallsOnDismissed() {
        // Given
        var dismissedCalled = false
        setContent(onDismissed = { dismissedCalled = true })
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText(registerTitle, ignoreCase = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // When
        pressBack()
        composeTestRule.waitUntil(timeoutMillis = 10_000) { dismissedCalled }

        // Then - обратный вызов закрытия листа вызван
        assert(dismissedCalled) { "Callback onDismissed не был вызван после нажатия «назад»" }
    }

    @Test
    fun registerSheetHost_onBackPressed_whenBusy_thenSheetStaysOpen() {
        // Given
        var dismissedCalled = false
        val uiState = setContent(onDismissed = { dismissedCalled = true })
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText(registerTitle, ignoreCase = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        uiState.value = RegisterUiState.Loading
        composeTestRule.waitForIdle()

        // When
        pressBack()
        composeTestRule.waitForIdle()

        // Then - лист остаётся открытым, закрытие не вызывается
        composeTestRule
            .onNodeWithText(registerTitle, ignoreCase = true)
            .assertIsDisplayed()
        assert(!dismissedCalled) { "Лист закрылся во время регистрации: onDismissed не должен вызываться" }
    }

    @Test
    fun registerSheetHost_onTapOutside_thenSheetStaysOpen() {
        // Given
        var dismissedCalled = false
        setContent(onDismissed = { dismissedCalled = true })
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText(registerTitle, ignoreCase = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // When
        tapOutside()
        composeTestRule.waitForIdle()

        // Then - лист остаётся открытым
        composeTestRule
            .onNodeWithText(registerTitle, ignoreCase = true)
            .assertIsDisplayed()
        assert(!dismissedCalled) { "Лист закрылся по тапу вне области: onDismissed не должен вызываться" }
    }

    @Test
    fun registerSheetHost_onSwipeDown_thenSheetStaysOpen() {
        // Given
        var dismissedCalled = false
        setContent(onDismissed = { dismissedCalled = true })
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithText(registerTitle, ignoreCase = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val titleNode = composeTestRule.onNodeWithText(registerTitle, ignoreCase = true)
        titleNode.assertIsDisplayed()

        // When
        titleNode.performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        // Then - лист остаётся открытым
        composeTestRule
            .onNodeWithText(registerTitle, ignoreCase = true)
            .assertIsDisplayed()
        assert(!dismissedCalled) { "Лист закрылся по свайпу вниз: onDismissed не должен вызываться" }
    }
}
