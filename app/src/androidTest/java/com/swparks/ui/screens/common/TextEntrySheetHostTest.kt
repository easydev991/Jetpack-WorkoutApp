package com.swparks.ui.screens.common

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
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.state.TextEntryEvent
import com.swparks.ui.state.TextEntryUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeTextEntryViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты хоста [TextEntrySheetHost]: способы закрытия модального листа.
 *
 * Проверяют поведение возврата (системная кнопка и жест возврата идут через
 * один обработчик ModalBottomSheet), тапа по затемнённой области и свайпа
 * листа вниз, в том числе во время отправки (guard по `uiState.isLoading`).
 */
@RunWith(AndroidJUnit4::class)
class TextEntrySheetHostTest : TimeoutTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val commentTitle = context.getString(R.string.new_comment_title)

    /**
     * Настраивает хост с фейковой ViewModel на управляемом состоянии.
     *
     * @return изменяемое состояние ViewModel для переключения `isLoading` в тесте
     */
    private fun setContent(
        onDismissed: () -> Unit = {},
        viewModel: FakeTextEntryViewModel? = null
    ): MutableStateFlow<TextEntryUiState> {
        val uiState =
            MutableStateFlow(
                TextEntryUiState(mode = TextEntryMode.NewForPark(parkId = 1L))
            )
        val fake = viewModel ?: FakeTextEntryViewModel(uiState = uiState)
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                TextEntrySheetHost(
                    show = true,
                    mode = TextEntryMode.NewForPark(parkId = 1L),
                    onDismissed = onDismissed,
                    viewModel = fake
                )
            }
        }
        composeTestRule.waitForIdle()
        return uiState
    }

    @Test
    fun textEntrySheetHost_onBackPressed_whenIdle_thenClosesSheetAndCallsOnDismissed() {
        // Given
        var dismissedCalled = false
        setContent(onDismissed = { dismissedCalled = true })
        composeTestRule
            .onNodeWithText(commentTitle)
            .assertIsDisplayed()

        // When
        pressBack()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { dismissedCalled }

        // Then - обратный вызов закрытия листа вызван
        assert(dismissedCalled) { "Callback onDismissed не был вызван после нажатия «назад»" }
    }

    @Test
    fun textEntrySheetHost_onBackPressed_whenLoading_thenSheetStaysOpen() {
        // Given
        var dismissedCalled = false
        val uiState = setContent(onDismissed = { dismissedCalled = true })
        uiState.value = uiState.value.copy(isLoading = true)
        composeTestRule.waitForIdle()

        // When
        pressBack()
        composeTestRule.waitForIdle()

        // Then - лист остаётся открытым, закрытие не вызывается
        composeTestRule
            .onNodeWithText(commentTitle)
            .assertIsDisplayed()
        assert(!dismissedCalled) { "Лист закрылся во время отправки: onDismissed не должен вызываться" }
    }

    @Test
    fun textEntrySheetHost_onTapOutside_thenSheetStaysOpen() {
        // Given
        var dismissedCalled = false
        setContent(onDismissed = { dismissedCalled = true })
        composeTestRule
            .onNodeWithText(commentTitle)
            .assertIsDisplayed()

        // When
        tapOutside()
        composeTestRule.waitForIdle()

        // Then - лист остаётся открытым
        composeTestRule
            .onNodeWithText(commentTitle)
            .assertIsDisplayed()
        assert(!dismissedCalled) { "Лист закрылся по тапу вне области: onDismissed не должен вызываться" }
    }

    @Test
    fun textEntrySheetHost_onSwipeDown_thenSheetStaysOpen() {
        // Given
        var dismissedCalled = false
        setContent(onDismissed = { dismissedCalled = true })
        val titleNode = composeTestRule.onNodeWithText(commentTitle)
        titleNode.assertIsDisplayed()

        // When
        titleNode.performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        // Then - лист остаётся открытым
        composeTestRule
            .onNodeWithText(commentTitle)
            .assertIsDisplayed()
        assert(!dismissedCalled) { "Лист закрылся по свайпу вниз: onDismissed не должен вызываться" }
    }

    @Test
    fun textEntrySheetHost_onSendError_thenShowsSnackbarAndKeepsSheetAndText() {
        // Given
        val errorMessage = "Не удалось отправить сообщение"
        val typedText = "Мой комментарий"
        val uiState =
            MutableStateFlow(
                TextEntryUiState(
                    mode = TextEntryMode.NewForPark(parkId = 1L),
                    text = typedText,
                    isSendEnabled = true
                )
            )
        val viewModel = FakeTextEntryViewModel(uiState = uiState)
        setContent(viewModel = viewModel)

        // When
        viewModel.sendEvent(TextEntryEvent.Error(message = errorMessage))

        // Then - ошибка видна в snackbar'е листа, лист открыт, введённый текст сохранён
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(errorMessage)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(commentTitle)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(typedText)
            .assertIsDisplayed()
    }
}
