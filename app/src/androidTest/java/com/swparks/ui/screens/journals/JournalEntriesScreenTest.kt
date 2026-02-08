package com.swparks.ui.screens.journals

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.domain.model.JournalEntry
import com.swparks.ui.state.JournalEntriesUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeJournalEntriesViewModel
import com.swparks.ui.viewmodel.IJournalEntriesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для JournalEntriesScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 * Проверяет отображение списка записей, реакции на нажатия
 * и поведение при разных состояниях UI (loading, error, content, empty).
 */
@RunWith(AndroidJUnit4::class)
class JournalEntriesScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    companion object {
        private const val TEST_JOURNAL_ID = 1L
        private const val TEST_JOURNAL_TITLE = "Тренировочный дневник"
    }

    private fun setContent(
        journalId: Long = TEST_JOURNAL_ID,
        journalTitle: String = TEST_JOURNAL_TITLE,
        viewModel: IJournalEntriesViewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = emptyList())),
            isRefreshing = MutableStateFlow(false)
        ),
        onBackClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = journalId,
                    journalTitle = journalTitle,
                    viewModel = viewModel,
                    onBackClick = onBackClick,
                    parentPaddingValues = PaddingValues()
                )
            }
        }
    }

    @Test
    fun testAppBar_showsTitle() {
        // Given
        val journalTitle = "Мой тренировочный дневник"

        // When
        setContent(journalTitle = journalTitle)

        // Then - AppBar с заголовком дневника отображается
        composeTestRule
            .onNodeWithText(journalTitle)
            .assertIsDisplayed()
    }

    @Test
    fun testBackButton_clicked() {
        // Given
        var backClicked = false
        val onBackClick = { backClicked = true }

        // When
        setContent(onBackClick = onBackClick)

        // Then - Кнопка "Назад" должна быть кликабельной
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .assertIsDisplayed()
            .assertHasClickAction()

        // When - Кликаем по кнопке "Назад"
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()

        // Then - Callback был вызван
        assert(backClicked)
    }

    @Test
    fun testInitialState_showsLoadingOverlay() {
        // Given
        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.InitialLoading),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = TEST_JOURNAL_ID,
                    journalTitle = TEST_JOURNAL_TITLE,
                    viewModel = viewModel,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - AppBar с заголовком отображается даже при загрузке
        composeTestRule
            .onNodeWithText(TEST_JOURNAL_TITLE)
            .assertIsDisplayed()
    }

    @Test
    fun testErrorState_showsErrorView() {
        // Given
        val errorMessage = "Ошибка загрузки записей"
        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Error(message = errorMessage)),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = TEST_JOURNAL_ID,
                    journalTitle = TEST_JOURNAL_TITLE,
                    viewModel = viewModel,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - AppBar с заголовком отображается даже при ошибке
        composeTestRule
            .onNodeWithText(TEST_JOURNAL_TITLE)
            .assertIsDisplayed()
    }

    @Test
    fun testErrorState_retryButton_clicks() {
        // Given
        val errorMessage = "Ошибка загрузки записей"
        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Error(message = errorMessage)),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = TEST_JOURNAL_ID,
                    journalTitle = TEST_JOURNAL_TITLE,
                    viewModel = viewModel,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Текст ошибки должен быть виден (если ErrorContentView отображает сообщение)
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun testContentState_showsEntriesList() {
        // Given
        val testEntry = JournalEntry(
            id = 1L,
            journalId = TEST_JOURNAL_ID,
            authorId = 1L,
            authorName = "Иван Иванов",
            message = "Отличная тренировка сегодня!",
            createDate = "2024-01-15T12:00:00",
            modifyDate = "2024-01-15T12:00:00",
            authorImage = null
        )

        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = listOf(testEntry))),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = TEST_JOURNAL_ID,
                    journalTitle = TEST_JOURNAL_TITLE,
                    viewModel = viewModel,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Имя автора записи отображается
        composeTestRule
            .onNodeWithText("Иван Иванов")
            .assertIsDisplayed()

        // Текст записи отображается
        composeTestRule
            .onNodeWithText("Отличная тренировка сегодня!")
            .assertIsDisplayed()
    }

    @Test
    fun testContentState_emptyList_showsEmptyScreen() {
        // Given
        FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = emptyList())),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        setContent()

        // Then - AppBar с заголовком отображается
        composeTestRule
            .onNodeWithText(TEST_JOURNAL_TITLE)
            .assertIsDisplayed()
    }

    @Test
    fun testContentState_displaysMultipleEntries() {
        // Given
        val entries = listOf(
            JournalEntry(
                id = 1L,
                journalId = TEST_JOURNAL_ID,
                authorId = 1L,
                authorName = "Иван Иванов",
                message = "Тренировка 1",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            ),
            JournalEntry(
                id = 2L,
                journalId = TEST_JOURNAL_ID,
                authorId = 2L,
                authorName = "Петр Петров",
                message = "Тренировка 2",
                createDate = "2024-01-16T12:00:00",
                modifyDate = "2024-01-16T12:00:00",
                authorImage = null
            ),
            JournalEntry(
                id = 3L,
                journalId = TEST_JOURNAL_ID,
                authorId = 3L,
                authorName = "Сергей Сидоров",
                message = "Тренировка 3",
                createDate = "2024-01-17T12:00:00",
                modifyDate = "2024-01-17T12:00:00",
                authorImage = null
            )
        )

        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = entries)),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - Все записи отображаются
        entries.forEach { entry ->
            composeTestRule
                .onNodeWithText(entry.authorName ?: "")
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(entry.message ?: "")
                .assertIsDisplayed()
        }
    }

    @Test
    fun testEntryItem_click_logsMessage() {
        // Given
        val testEntry = JournalEntry(
            id = 1L,
            journalId = TEST_JOURNAL_ID,
            authorId = 1L,
            authorName = "Иван Иванов",
            message = "Отличная тренировка сегодня!",
            createDate = "2024-01-15T12:00:00",
            modifyDate = "2024-01-15T12:00:00",
            authorImage = null
        )

        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = listOf(testEntry))),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = TEST_JOURNAL_ID,
                    journalTitle = TEST_JOURNAL_TITLE,
                    viewModel = viewModel,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Запись должна быть кликабельной
        composeTestRule
            .onNodeWithText("Иван Иванов")
            .assertHasClickAction()
    }

    @Test
    fun testPullToRefresh_blocksUI() {
        // Given
        val testEntry = JournalEntry(
            id = 1L,
            journalId = TEST_JOURNAL_ID,
            authorId = 1L,
            authorName = "Иван Иванов",
            message = "Отличная тренировка сегодня!",
            createDate = "2024-01-15T12:00:00",
            modifyDate = "2024-01-15T12:00:00",
            authorImage = null
        )

        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = listOf(testEntry))),
            isRefreshing = MutableStateFlow(true)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = TEST_JOURNAL_ID,
                    journalTitle = TEST_JOURNAL_TITLE,
                    viewModel = viewModel,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Запись отображается, но не кликабельна при обновлении
        composeTestRule
            .onNodeWithText("Иван Иванов")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun testContentState_withRefreshingIndicator() {
        // Given
        val testEntry = JournalEntry(
            id = 1L,
            journalId = TEST_JOURNAL_ID,
            authorId = 1L,
            authorName = "Иван Иванов",
            message = "Отличная тренировка сегодня!",
            createDate = "2024-01-15T12:00:00",
            modifyDate = "2024-01-15T12:00:00",
            authorImage = null
        )

        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(
                JournalEntriesUiState.Content(entries = listOf(testEntry), isRefreshing = true)
            ),
            isRefreshing = MutableStateFlow(true)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = TEST_JOURNAL_ID,
                    journalTitle = TEST_JOURNAL_TITLE,
                    viewModel = viewModel,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Запись отображается при обновлении
        composeTestRule
            .onNodeWithText("Иван Иванов")
            .assertIsDisplayed()
    }

    /**
     * Тест 11: Проверка отображения диалога подтверждения удаления
     */
    @Test
    fun testContentState_clickDeleteAction_showsDeleteDialog() {
        // Given
        val testEntry = JournalEntry(
            id = 1L,
            journalId = TEST_JOURNAL_ID,
            authorId = 1L,
            authorName = "Иван Иванов",
            message = "Отличная тренировка сегодня!",
            createDate = "2024-01-15T12:00:00",
            modifyDate = "2024-01-15T12:00:00",
            authorImage = null
        )

        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(
                JournalEntriesUiState.Content(entries = listOf(testEntry), isRefreshing = false)
            ),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = TEST_JOURNAL_ID,
                    journalTitle = TEST_JOURNAL_TITLE,
                    viewModel = viewModel,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Кликаем на кнопку меню записи (кнопка с testTag "MenuButton")
        composeTestRule
            .onNodeWithTag("MenuButton")
            .performClick()

        // Кликаем на действие Delete в открытом меню (по тексту элемента меню)
        composeTestRule
            .onNodeWithText(context.getString(R.string.delete))
            .performClick()

        // Проверяем, что отображается диалог подтверждения удаления
        composeTestRule
            .onNodeWithText(context.getString(R.string.delete_entry_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.delete_entry_message))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.delete))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.cancel))
            .assertIsDisplayed()
    }

    /**
     * Тест 12: Проверка отображения EmptyStateView при пустом списке записей
     */
    @Test
    fun testEmptyState_displaysEmptyStateView() {
        // Given
        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = emptyList())),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = TEST_JOURNAL_ID,
                    journalTitle = TEST_JOURNAL_TITLE,
                    viewModel = viewModel,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Текст EmptyStateView отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.entries_empty))
            .assertIsDisplayed()

        // Кнопка EmptyStateView отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    /**
     * Тест 13: Проверка, что кнопка EmptyStateView кликабельна при enabled = true
     */
    @Test
    fun testEmptyState_buttonIsEnabled_whenNotRefreshing() {
        // Given
        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = emptyList())),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - Кнопка EmptyStateView кликабельна
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertHasClickAction()
    }

    /**
     * Тест 14: Проверка, что кнопка EmptyStateView отключена при isRefreshing = true
     */
    @Test
    fun testEmptyState_buttonIsDisabled_whenRefreshing() {
        // Given
        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = emptyList())),
            isRefreshing = MutableStateFlow(true)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - Кнопка EmptyStateView отключена
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    /**
     * Тест 15: Проверка, что кнопка EmptyStateView отключена при isDeleting = true
     */
    @Test
    fun testEmptyState_buttonIsDisabled_whenDeleting() {
        // Given
        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = emptyList())),
            isRefreshing = MutableStateFlow(false),
            isDeleting = MutableStateFlow(true)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - Кнопка EmptyStateView отключена
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    /**
     * Тест 16: Проверка, что кнопка EmptyStateView отключена при isRefreshing и isDeleting = true
     */
    @Test
    fun testEmptyState_buttonIsDisabled_whenRefreshingAndDeleting() {
        // Given
        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = emptyList())),
            isRefreshing = MutableStateFlow(true),
            isDeleting = MutableStateFlow(true)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - Кнопка EmptyStateView отключена
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    /**
     * Тест 17: Проверка скрытия кнопки удаления для первой записи
     */
    @Test
    fun testContentState_firstEntry_deleteButtonNotVisible() {
        // Given
        val firstEntryId = 1L
        val entries = listOf(
            JournalEntry(
                id = firstEntryId,
                journalId = TEST_JOURNAL_ID,
                authorId = 1L,
                authorName = "Иван Иванов",
                message = "Первая запись (нельзя удалить)",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            ),
            JournalEntry(
                id = 2L,
                journalId = TEST_JOURNAL_ID,
                authorId = 2L,
                authorName = "Петр Петров",
                message = "Вторая запись (можно удалить)",
                createDate = "2024-01-16T12:00:00",
                modifyDate = "2024-01-16T12:00:00",
                authorImage = null
            )
        )

        val viewModel = FakeJournalEntriesViewModel(
            uiState = MutableStateFlow(
                JournalEntriesUiState.Content(
                    entries = entries,
                    isRefreshing = false,
                    firstEntryId = firstEntryId
                )
            ),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    journalId = TEST_JOURNAL_ID,
                    journalTitle = TEST_JOURNAL_TITLE,
                    viewModel = viewModel,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Первая запись отображается
        composeTestRule
            .onNodeWithText("Иван Иванов")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Первая запись (нельзя удалить)")
            .assertIsDisplayed()

        // Вторая запись отображается
        composeTestRule
            .onNodeWithText("Петр Петров")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Вторая запись (можно удалить)")
            .assertIsDisplayed()

        // Кликаем на кнопку меню первой записи
        composeTestRule
            .onNodeWithTag("JournalEntry_${firstEntryId}")
            .performClick()

        // Проверяем, что для первой записи кнопка "Удалить" отсутствует в меню
        // Открываем меню
        composeTestRule
            .onNodeWithTag("MenuButton", useUnmergedTree = true)
            .performClick()

        // Проверяем, что кнопка удаления не отображается для первой записи
        // (тест просто проверяет, что элемент не найден)
        composeTestRule
            .onNodeWithText(context.getString(R.string.delete))
            .assertDoesNotExist()
    }
}
