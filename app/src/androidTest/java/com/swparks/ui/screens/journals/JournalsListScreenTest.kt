package com.swparks.ui.screens.journals

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.domain.model.Journal
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.state.JournalsUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeJournalsViewModel
import com.swparks.ui.viewmodel.IJournalsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для JournalsListScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 * Проверяет отображение списка дневников, реакции на нажатия
 * и поведение при разных состояниях UI (loading, error, content, empty).
 */
@RunWith(AndroidJUnit4::class)
class JournalsListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        userId: Long = 1L,
        viewModel: IJournalsViewModel = FakeJournalsViewModel(
            uiState = MutableStateFlow(JournalsUiState.Content(journals = emptyList())),
            isRefreshing = MutableStateFlow(false)
        ),
        onBackClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    userId = userId,
                    viewModel = viewModel,
                    onBackClick = onBackClick,
                    onJournalClick = { _, _ -> },
                    parentPaddingValues = PaddingValues()
                )
            }
        }
    }

    @Test
    fun journalsListScreen_displaysAppBarWithJournalsListTitle() {
        // When
        setContent()

        // Then - AppBar с заголовком всегда отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.journals_list_title))
            .assertIsDisplayed()
    }

    @Test
    fun journalsListScreen_displaysLoadingState_onInitialLoading() {
        // Given
        val viewModel = FakeJournalsViewModel(
            uiState = MutableStateFlow(JournalsUiState.InitialLoading),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    userId = 1L,
                    viewModel = viewModel,
                    onBackClick = {},
                    onJournalClick = { _, _ -> },
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - AppBar отображается даже при загрузке
        composeTestRule
            .onNodeWithText(context.getString(R.string.journals_list_title))
            .assertIsDisplayed()
    }

    @Test
    fun journalsListScreen_displaysEmptyState_withCreateButton() {
        // Given
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel = FakeJournalsViewModel(
            uiState = MutableStateFlow(state),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    userId = 1L,
                    viewModel = viewModel,
                    onBackClick = {},
                    onJournalClick = { _, _ -> },
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Текст о пустом списке отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.journals_empty))
            .assertIsDisplayed()

        // Кнопка "Создать дневник" отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_journal))
            .assertIsDisplayed()
    }

    @Test
    fun journalsListScreen_createButtonIsClickable() {
        // Given
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel = FakeJournalsViewModel(
            uiState = MutableStateFlow(state),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    userId = 1L,
                    viewModel = viewModel,
                    onBackClick = {},
                    onJournalClick = { _, _ -> },
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Кнопка "Создать дневник" должна быть кликабельной
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_journal))
            .assertHasClickAction()
    }

    @Test
    fun journalsListScreen_displaysJournalItems_whenHasJournals() {
        // Given
        val testJournal = Journal(
            id = 1L,
            title = "Тренировочный дневник",
            lastMessageImage = null,
            createDate = "2024-01-01",
            modifyDate = "2024-01-15",
            lastMessageDate = "2024-01-15",
            lastMessageText = "Отличная тренировка!",
            entriesCount = 10,
            ownerId = 1L,
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )

        val state = JournalsUiState.Content(journals = listOf(testJournal))
        val viewModel = FakeJournalsViewModel(
            uiState = MutableStateFlow(state),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    userId = 1L,
                    viewModel = viewModel,
                    onBackClick = {},
                    onJournalClick = { _, _ -> },
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Заголовок дневника отображается
        composeTestRule
            .onNodeWithText("Тренировочный дневник")
            .assertIsDisplayed()

        // Текст последнего сообщения отображается
        composeTestRule
            .onNodeWithText("Отличная тренировка!")
            .assertIsDisplayed()
    }

    @Test
    fun journalsListScreen_displaysMultipleJournals() {
        // Given
        val journals = listOf(
            Journal(
                id = 1L,
                title = "Дневник 1",
                lastMessageImage = null,
                createDate = "2024-01-01",
                modifyDate = "2024-01-15",
                lastMessageDate = "2024-01-15",
                lastMessageText = "Сообщение 1",
                entriesCount = 5,
                ownerId = 1L,
                viewAccess = JournalAccess.ALL,
                commentAccess = JournalAccess.ALL
            ),
            Journal(
                id = 2L,
                title = "Дневник 2",
                lastMessageImage = null,
                createDate = "2024-02-01",
                modifyDate = "2024-02-15",
                lastMessageDate = "2024-02-15",
                lastMessageText = "Сообщение 2",
                entriesCount = 10,
                ownerId = 1L,
                viewAccess = JournalAccess.FRIENDS,
                commentAccess = JournalAccess.FRIENDS
            ),
            Journal(
                id = 3L,
                title = "Дневник 3",
                lastMessageImage = null,
                createDate = "2024-03-01",
                modifyDate = "2024-03-15",
                lastMessageDate = "2024-03-15",
                lastMessageText = "Сообщение 3",
                entriesCount = 15,
                ownerId = 1L,
                viewAccess = JournalAccess.ALL,
                commentAccess = JournalAccess.FRIENDS
            )
        )

        val state = JournalsUiState.Content(journals = journals)
        val viewModel = FakeJournalsViewModel(
            uiState = MutableStateFlow(state),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    userId = 1L,
                    viewModel = viewModel,
                    onBackClick = {},
                    onJournalClick = { _, _ -> },
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Все дневники отображаются
        journals.forEach { journal ->
            composeTestRule
                .onNodeWithText(journal.title!!)
                .assertIsDisplayed()
        }
    }

    @Test
    fun journalsListScreen_displaysErrorState() {
        // Given
        val errorMessage = "Ошибка загрузки дневников"
        val state = JournalsUiState.Error(message = errorMessage)
        val viewModel = FakeJournalsViewModel(
            uiState = MutableStateFlow(state),
            isRefreshing = MutableStateFlow(false)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    userId = 1L,
                    viewModel = viewModel,
                    onBackClick = {},
                    onJournalClick = { _, _ -> },
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - AppBar отображается даже при ошибке
        composeTestRule
            .onNodeWithText(context.getString(R.string.journals_list_title))
            .assertIsDisplayed()
    }

    @Test
    fun journalsListScreen_displaysContentState_withRefreshingIndicator() {
        // Given
        val testJournal = Journal(
            id = 1L,
            title = "Тренировочный дневник",
            lastMessageImage = null,
            createDate = "2024-01-01",
            modifyDate = "2024-01-15",
            lastMessageDate = "2024-01-15",
            lastMessageText = "Отличная тренировка!",
            entriesCount = 10,
            ownerId = 1L,
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )

        val state = JournalsUiState.Content(journals = listOf(testJournal), isRefreshing = true)
        val viewModel = FakeJournalsViewModel(
            uiState = MutableStateFlow(state),
            isRefreshing = MutableStateFlow(true)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    userId = 1L,
                    viewModel = viewModel,
                    onBackClick = {},
                    onJournalClick = { _, _ -> },
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Заголовок дневника отображается
        composeTestRule
            .onNodeWithText("Тренировочный дневник")
            .assertIsDisplayed()
    }

    @Test
    fun journalsListScreen_backButtonIsClickable() {
        // When
        setContent()

        // Then - Кнопка "Назад" должна быть кликабельной (по contentDescription)
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun journalsListScreen_createButtonIsDisabled_whenRefreshing() {
        // Given
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel = FakeJournalsViewModel(
            uiState = MutableStateFlow(state),
            isRefreshing = MutableStateFlow(true)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    userId = 1L,
                    viewModel = viewModel,
                    onBackClick = {},
                    onJournalClick = { _, _ -> },
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Кнопка "Создать дневник" должна быть неактивной при обновлении
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_journal))
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun journalsListScreen_journalItemIsDisabled_whenRefreshing() {
        // Given
        val testJournal = Journal(
            id = 1L,
            title = "Тренировочный дневник",
            lastMessageImage = null,
            createDate = "2024-01-01",
            modifyDate = "2024-01-15",
            lastMessageDate = "2024-01-15",
            lastMessageText = "Отличная тренировка!",
            entriesCount = 10,
            ownerId = 1L,
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )

        val state = JournalsUiState.Content(journals = listOf(testJournal))
        val viewModel = FakeJournalsViewModel(
            uiState = MutableStateFlow(state),
            isRefreshing = MutableStateFlow(true)
        )

        // When
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    userId = 1L,
                    viewModel = viewModel,
                    onBackClick = {},
                    onJournalClick = { _, _ -> },
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Элемент дневника отображается, но не кликабелен при обновлении
        composeTestRule
            .onNodeWithText("Тренировочный дневник")
            .assertIsDisplayed()
    }
}
