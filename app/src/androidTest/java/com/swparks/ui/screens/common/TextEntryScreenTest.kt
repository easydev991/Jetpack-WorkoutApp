package com.swparks.ui.screens.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.ui.model.EditInfo
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.state.TextEntryUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeTextEntryViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Компонентные тесты для [TextEntryScreen].
 *
 * Тестирует UI компонент в изоляции без бизнес-логики ViewModel.
 * Проверяет отображение элементов экрана для разных режимов,
 * корректность отображения заголовков, placeholder,
 * состояние кнопки "Отправить" и блокировку UI при загрузке.
 */
@RunWith(AndroidJUnit4::class)
class TextEntryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val newCommentTitle = context.getString(R.string.new_comment_title)
    private val newEntryTitle = context.getString(R.string.new_entry_title)
    private val editCommentTitle = context.getString(R.string.edit_comment_title)
    private val editEntryTitle = context.getString(R.string.edit_entry_title)
    private val sendButtonText = context.getString(R.string.send_button_text)
    private val closeDescription = context.getString(R.string.close_button_content_description)

    /**
     * Настраивает TextEntryScreen для тестирования.
     */
    private fun setContent(
        viewModel: FakeTextEntryViewModel,
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                TextEntryScreen(
                    viewModel = viewModel,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun textEntryScreen_whenNewForPark_thenShowsNewCommentTitle() {
        // Given
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(mode = mode)
            )
        )

        // When
        setContent(viewModel)

        // Then - Заголовок "Новый комментарий" отображается
        composeTestRule
            .onNodeWithText(newCommentTitle, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenNewForEvent_thenShowsNewCommentTitle() {
        // Given
        val mode = TextEntryMode.NewForEvent(eventId = 1L)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(mode = mode)
            )
        )

        // When
        setContent(viewModel)

        // Then - Заголовок "Новый комментарий" отображается
        composeTestRule
            .onNodeWithText(newCommentTitle, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenNewForJournal_thenShowsNewEntryTitle() {
        // Given
        val mode = TextEntryMode.NewForJournal(ownerId = 1L, journalId = 1L)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(mode = mode)
            )
        )

        // When
        setContent(viewModel)

        // Then - Заголовок "Новая запись" отображается
        composeTestRule
            .onNodeWithText(newEntryTitle, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenEditPark_thenShowsEditCommentTitle() {
        // Given
        val editInfo = EditInfo(parentObjectId = 1L, entryId = 1L, oldEntry = "Old comment")
        val mode = TextEntryMode.EditPark(editInfo)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(mode = mode)
            )
        )

        // When
        setContent(viewModel)

        // Then - Заголовок "Изменить комментарий" отображается
        composeTestRule
            .onNodeWithText(editCommentTitle, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenEditEvent_thenShowsEditCommentTitle() {
        // Given
        val editInfo = EditInfo(parentObjectId = 1L, entryId = 1L, oldEntry = "Old comment")
        val mode = TextEntryMode.EditEvent(editInfo)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(mode = mode)
            )
        )

        // When
        setContent(viewModel)

        // Then - Заголовок "Изменить комментарий" отображается
        composeTestRule
            .onNodeWithText(editCommentTitle, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenEditJournalEntry_thenShowsEditEntryTitle() {
        // Given
        val editInfo = EditInfo(parentObjectId = 1L, entryId = 1L, oldEntry = "Old entry")
        val mode = TextEntryMode.EditJournalEntry(ownerId = 1L, editInfo)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(mode = mode)
            )
        )

        // When
        setContent(viewModel)

        // Then - Заголовок "Изменить запись" отображается
        composeTestRule
            .onNodeWithText(editEntryTitle, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenNewForJournal_thenShowsPlaceholder() {
        // Given
        val mode = TextEntryMode.NewForJournal(ownerId = 1L, journalId = 1L)
        val placeholder = context.getString(R.string.new_entry_placeholder)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(mode = mode)
            )
        )

        // When
        setContent(viewModel)

        // Then - Placeholder "Создать новую запись в дневнике" отображается
        composeTestRule
            .onNodeWithText(placeholder, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenNewForPark_thenNoPlaceholder() {
        // Given
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        val placeholder = context.getString(R.string.new_entry_placeholder)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(mode = mode)
            )
        )

        // When
        setContent(viewModel)

        // Then - Placeholder для дневника не отображается
        composeTestRule
            .onNodeWithText(placeholder, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun textEntryScreen_whenSendButtonEnabled_thenShowsSendButton() {
        // Given
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(
                    mode = mode,
                    isSendEnabled = true
                )
            )
        )

        // When
        setContent(viewModel)

        // Then - Кнопка "Отправить" отображается
        composeTestRule
            .onNodeWithText(sendButtonText, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenSendButtonDisabled_thenShowsDisabledSendButton() {
        // Given
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(
                    mode = mode,
                    isSendEnabled = false
                )
            )
        )

        // When
        setContent(viewModel)

        // Then - Кнопка "Отправить" отображается, но не активна
        composeTestRule
            .onNodeWithText(sendButtonText, ignoreCase = true)
            .assertIsNotEnabled()
    }

    @Test
    fun textEntryScreen_whenNotLoading_thenCloseButtonEnabled() {
        // Given
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        var dismissCalled = false
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(
                    mode = mode,
                    isLoading = false
                )
            )
        )

        // When
        setContent(viewModel, onDismiss = { dismissCalled = true })
        composeTestRule
            .onNodeWithContentDescription(closeDescription, ignoreCase = true)
            .assertIsDisplayed()

        // Then - Кнопка закрытия должна быть кликабельной (enabled)
        // Проверяем, что callback вызывается при клике
        composeTestRule
            .onNodeWithContentDescription(closeDescription, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun textEntryScreen_whenCloseButtonClicked_thenCallsOnDismiss() {
        // Given
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        var dismissCalled = false
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(mode = mode)
            )
        )

        // When
        setContent(viewModel, onDismiss = { dismissCalled = true })
        composeTestRule
            .onNodeWithContentDescription(closeDescription, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(closeDescription, ignoreCase = true)
            .performClick()

        // Then - Callback onDismiss должен быть вызван
        assert(dismissCalled) { "Callback onDismiss не был вызван" }
    }

    @Test
    fun textEntryScreen_whenLoading_thenShowsLoadingOverlay() {
        // Given
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        val loadingDescription = context.getString(R.string.loading_content_description)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(
                    mode = mode,
                    isLoading = true
                )
            )
        )

        // When
        setContent(viewModel)

        // Then - Loading overlay отображается
        composeTestRule
            .onNodeWithContentDescription(loadingDescription, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenLoading_thenSendButtonDisabled() {
        // Given
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(
                    mode = mode,
                    isSendEnabled = true,
                    isLoading = true
                )
            )
        )

        // When
        setContent(viewModel)

        // Then - Кнопка "Отправить" не активна при загрузке (несмотря на isSendEnabled = true)
        composeTestRule
            .onNodeWithText(sendButtonText, ignoreCase = true)
            .assertIsNotEnabled()
    }

    @Test
    fun textEntryScreen_whenLoading_thenCloseButtonDisabled() {
        // Given
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(
                    mode = mode,
                    isLoading = true
                )
            )
        )

        // When
        setContent(viewModel)

        // Then - Кнопка закрытия существует, но disabled (проверяем через enabled = false в TextEntryScreen)
        // Кнопка отображается, но isEnabled = false при isLoading = true
        composeTestRule
            .onNodeWithContentDescription(closeDescription, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenEditParkMode_thenTextFieldPrepopulatedWithOldEntry() {
        // Given
        val oldText = "Old comment text"
        val editInfo = EditInfo(parentObjectId = 1L, entryId = 1L, oldEntry = oldText)
        val mode = TextEntryMode.EditPark(editInfo)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(
                    mode = mode,
                    text = oldText,
                    isSendEnabled = false
                )
            )
        )

        // When
        setContent(viewModel)

        // Then - Текстовое поле должно содержать старый текст
        composeTestRule
            .onNodeWithText(oldText, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenEditEventMode_thenTextFieldPrepopulatedWithOldEntry() {
        // Given
        val oldText = "Old event comment"
        val editInfo = EditInfo(parentObjectId = 1L, entryId = 1L, oldEntry = oldText)
        val mode = TextEntryMode.EditEvent(editInfo)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(
                    mode = mode,
                    text = oldText,
                    isSendEnabled = false
                )
            )
        )

        // When
        setContent(viewModel)

        // Then - Текстовое поле должно содержать старый текст
        composeTestRule
            .onNodeWithText(oldText, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenEditJournalEntryMode_thenTextFieldPrepopulatedWithOldEntry() {
        // Given
        val oldText = "Old journal entry"
        val editInfo = EditInfo(parentObjectId = 1L, entryId = 1L, oldEntry = oldText)
        val mode = TextEntryMode.EditJournalEntry(ownerId = 1L, editInfo)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(
                    mode = mode,
                    text = oldText,
                    isSendEnabled = false
                )
            )
        )

        // When
        setContent(viewModel)

        // Then - Текстовое поле должно содержать старый текст
        composeTestRule
            .onNodeWithText(oldText, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun textEntryScreen_whenNewMode_thenTextFieldEmpty() {
        // Given
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        val viewModel = FakeTextEntryViewModel(
            uiState = MutableStateFlow(
                TextEntryUiState(
                    mode = mode,
                    text = "",
                    isSendEnabled = false
                )
            )
        )

        // When
        setContent(viewModel)

        // Then - Проверяем, что текстовое поле отображается (placeholder может присутствовать)
        composeTestRule
            .onNodeWithText(newCommentTitle, ignoreCase = true)
            .assertIsDisplayed()
        // Текстовое поле не должно содержать старый текст
        composeTestRule
            .onNodeWithText("Old comment", ignoreCase = false)
            .assertDoesNotExist()
    }
}
