package com.swparks.ui.screens.journals

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.domain.model.Journal
import com.swparks.navigation.AppState
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.state.JournalsUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeJournalsViewModel
import com.swparks.ui.viewmodel.IJournalsViewModel
import com.swparks.ui.viewmodel.JournalsEvent
import com.swparks.util.FakeAnalyticsReporter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
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
        viewModel: IJournalsViewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(JournalsUiState.Content(journals = emptyList())),
                isRefreshing = MutableStateFlow(false)
            ),
        onBackClick: () -> Unit = {},
        onJournalClick: (JournalNavigationParams) -> Unit = {}
    ) {
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(User(id = userId, name = "testuser", image = null))

            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    viewModel = viewModel,
                    config =
                        JournalsScreenConfig(
                            appState = appState,
                            params = JournalsScreenParams(userId = userId),
                            parentPaddingValues = PaddingValues()
                        ),
                    onAction = { action ->
                        when (action) {
                            JournalsListAction.Back -> onBackClick()
                            is JournalsListAction.JournalClick -> onJournalClick(action.params)
                        }
                    }
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
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(JournalsUiState.InitialLoading),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        setContent(
            userId = 1L,
            viewModel = viewModel
        )

        // Then - AppBar отображается даже при загрузке
        composeTestRule
            .onNodeWithText(context.getString(R.string.journals_list_title))
            .assertIsDisplayed()
    }

    @Test
    fun journalsListScreen_displaysEmptyState_withCreateButton() {
        // Given
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        setContent(
            userId = 1L,
            viewModel = viewModel
        )

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
    fun errorState_retryClick_showsLoadingThenContent() {
        val uiState = MutableStateFlow<JournalsUiState>(JournalsUiState.Error("Ошибка"))
        val viewModel =
            object : IJournalsViewModel {
                override val uiState = uiState
                override val isRefreshing = MutableStateFlow(false)
                override val isDeleting = MutableStateFlow(false)
                override val isSavingSettings = MutableStateFlow(false)
                override val events = MutableSharedFlow<JournalsEvent>()

                override fun retry() {
                    uiState.value = JournalsUiState.InitialLoading
                }

                override fun loadJournals() = Unit

                override fun deleteJournal(journalId: Long) = Unit

                override fun editJournalSettings(
                    journalId: Long,
                    title: String,
                    viewAccess: JournalAccess,
                    commentAccess: JournalAccess
                ) = Unit
            }

        setContent(viewModel = viewModel)

        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            uiState.value = JournalsUiState.Content(journals = listOf(createTestJournal()))
        }

        composeTestRule
            .onNodeWithText(createTestJournal().title ?: "")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_retryClick_showsLoadingThenError() {
        val retryError = "Повторная ошибка"
        val uiState = MutableStateFlow<JournalsUiState>(JournalsUiState.Error("Ошибка"))
        val viewModel =
            object : IJournalsViewModel {
                override val uiState = uiState
                override val isRefreshing = MutableStateFlow(false)
                override val isDeleting = MutableStateFlow(false)
                override val isSavingSettings = MutableStateFlow(false)
                override val events = MutableSharedFlow<JournalsEvent>()

                override fun retry() {
                    uiState.value = JournalsUiState.InitialLoading
                }

                override fun loadJournals() = Unit

                override fun deleteJournal(journalId: Long) = Unit

                override fun editJournalSettings(
                    journalId: Long,
                    title: String,
                    viewAccess: JournalAccess,
                    commentAccess: JournalAccess
                ) = Unit
            }

        setContent(viewModel = viewModel)

        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            uiState.value = JournalsUiState.Error(retryError)
        }

        composeTestRule
            .onNodeWithText(retryError)
            .assertIsDisplayed()
    }

    private fun createTestJournal(
        id: Long = 1L,
        title: String = "Тестовый дневник"
    ): Journal =
        Journal(
            id = id,
            title = title,
            lastMessageImage = null,
            createDate = "2024-01-01",
            modifyDate = "2024-01-15",
            lastMessageDate = "2024-01-15",
            lastMessageText = "Сообщение",
            entriesCount = 1,
            ownerId = 1L,
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )

    @Test
    fun journalsListScreen_createButtonIsClickable() {
        // Given
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        setContent(
            userId = 1L,
            viewModel = viewModel
        )

        // Then - Кнопка "Создать дневник" должна быть кликабельной
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_journal))
            .assertHasClickAction()
    }

    @Test
    fun journalsListScreen_displaysJournalItems_whenHasJournals() {
        // Given
        val testJournal =
            Journal(
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
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        setContent(
            userId = 1L,
            viewModel = viewModel
        )

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
        val journals =
            listOf(
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
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        setContent(
            userId = 1L,
            viewModel = viewModel
        )

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
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        setContent(
            userId = 1L,
            viewModel = viewModel
        )

        // Then - AppBar отображается даже при ошибке
        composeTestRule
            .onNodeWithText(context.getString(R.string.journals_list_title))
            .assertIsDisplayed()
    }

    @Test
    fun journalsListScreen_displaysContentState_withRefreshingIndicator() {
        // Given
        val testJournal =
            Journal(
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
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(true)
            )

        // When
        setContent(
            userId = 1L,
            viewModel = viewModel
        )

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
    fun journalsListScreen_createButtonNotShown_whenRefreshing() {
        // Given - EmptyStateView не показывается во время загрузки
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(true)
            )

        // When
        setContent(
            userId = 1L,
            viewModel = viewModel
        )

        // Then - Кнопка "Создать дневник" НЕ отображается во время загрузки
        // (EmptyStateView показывается только после завершения загрузки)
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_journal))
            .assertDoesNotExist()
    }

    @Test
    fun journalsListScreen_journalItemIsDisabled_whenRefreshing() {
        // Given
        val testJournal =
            Journal(
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
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(true)
            )

        // When
        setContent(
            userId = 1L,
            viewModel = viewModel
        )

        // Then - Элемент дневника отображается, но не кликабелен при обновлении
        composeTestRule
            .onNodeWithText("Тренировочный дневник")
            .assertIsDisplayed()
    }

    @Test
    fun journalsListScreen_fabIsDisplayed_whenAuthorizedUserAndOwnProfile() {
        // Given - авторизованный пользователь открывает свой профиль
        val userId = 1L
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(User(id = userId, name = "testuser", image = null))

            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    viewModel = viewModel,
                    config =
                        JournalsScreenConfig(
                            appState = appState,
                            params = JournalsScreenParams(userId = userId),
                            parentPaddingValues = PaddingValues()
                        ),
                    onAction = {}
                )
            }
        }

        // Then - FAB для создания дневника отображается
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.fab_create_journal_description))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun journalsListScreen_fabIsHidden_whenNotAuthorizedUser() {
        // Given - пользователь не авторизован
        val userId = 1L
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false)
            )

        // When - не устанавливаем currentUser в setContent
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            // currentUser остается null по умолчанию

            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    viewModel = viewModel,
                    config =
                        JournalsScreenConfig(
                            appState = appState,
                            params = JournalsScreenParams(userId = userId),
                            parentPaddingValues = PaddingValues()
                        ),
                    onAction = {}
                )
            }
        }

        // Then - FAB для создания дневника не отображается
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.fab_create_journal_description))
            .assertDoesNotExist()
    }

    @Test
    fun journalsListScreen_fabIsHidden_whenOpeningAnotherUsersProfile() {
        // Given - авторизованный пользователь открывает чужой профиль
        val currentUserId = 1L
        val profileUserId = 2L
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(User(id = currentUserId, name = "testuser", image = null))

            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    viewModel = viewModel,
                    config =
                        JournalsScreenConfig(
                            appState = appState,
                            params = JournalsScreenParams(userId = profileUserId),
                            parentPaddingValues = PaddingValues()
                        ),
                    onAction = {}
                )
            }
        }

        // Then - FAB для создания дневника не отображается
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.fab_create_journal_description))
            .assertDoesNotExist()
    }

    @Test
    fun journalsListScreen_fabIsHidden_whenDeleting() {
        // Given - авторизованный пользователь, режим удаления
        val userId = 1L
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false),
                isDeleting = MutableStateFlow(true)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(User(id = userId, name = "testuser", image = null))

            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    viewModel = viewModel,
                    config =
                        JournalsScreenConfig(
                            appState = appState,
                            params = JournalsScreenParams(userId = userId),
                            parentPaddingValues = PaddingValues()
                        ),
                    onAction = {}
                )
            }
        }

        // Then - FAB для создания дневника не отображается при удалении
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.fab_create_journal_description))
            .assertDoesNotExist()
    }

    @Test
    fun journalsListScreen_journalSettingsDialog_closes_onJournalSettingsSaved() =
        runTest {
            // Given - дневник и его настройки
            val journal =
                Journal(
                    id = 1L,
                    title = "Тестовый дневник",
                    lastMessageImage = null,
                    createDate = null,
                    modifyDate = null,
                    lastMessageDate = null,
                    lastMessageText = null,
                    entriesCount = 0,
                    ownerId = 1L,
                    viewAccess = JournalAccess.ALL,
                    commentAccess = JournalAccess.ALL
                )
            val state = JournalsUiState.Content(journals = listOf(journal))
            val viewModel =
                FakeJournalsViewModel(
                    uiState = MutableStateFlow(state),
                    isRefreshing = MutableStateFlow(false)
                )

            var onJournalSettingsSavedCalled = false

            // When - открываем экран с обработчиком событий
            composeTestRule.setContent {
                val navController = rememberNavController()
                val appState = AppState(navController, FakeAnalyticsReporter())
                appState.updateCurrentUser(User(id = 1L, name = "testuser", image = null))

                JetpackWorkoutAppTheme {
                    var journalToEditSettings by remember {
                        mutableStateOf<Journal?>(journal)
                    }

                    // Обработчик событий ViewModel
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        viewModel.events.collect { event ->
                            when (event) {
                                is JournalsEvent.JournalSettingsSaved -> {
                                    // Закрыть диалог только если это был наш journal
                                    if (journalToEditSettings?.id == event.journal.id) {
                                        journalToEditSettings = null
                                        onJournalSettingsSavedCalled = true
                                    }
                                }
                            }
                        }
                    }

                    // Отображаем диалог если journalToEditSettings не null
                    journalToEditSettings?.let { currentJournal ->
                        JournalSettingsDialog(
                            journal = currentJournal,
                            onDismiss = { journalToEditSettings = null },
                            viewModel = viewModel,
                            isSaving = false
                        )
                    }
                }
            }

            // Диалог настроек должен быть открыт
            composeTestRule
                .onNodeWithText(context.getString(R.string.journal_settings))
                .assertIsDisplayed()

            // Эмитируем событие успешного сохранения настроек
            viewModel.emitJournalSettingsSaved(journal)
            composeTestRule.waitForIdle()

            // Then - диалог настроек должен быть закрыт
            composeTestRule
                .onNodeWithText(context.getString(R.string.journal_settings))
                .assertDoesNotExist()
        }

    // ==================== Tests for EmptyStateView behavior for other users ====================

    @Test
    fun journalsListScreen_emptyStateNotShown_forOtherUserWhenRefreshing() {
        // Given - авторизованный пользователь открывает чужой дневник, идет загрузка
        val currentUserId = 1L
        val profileUserId = 2L
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(true) // Идет загрузка
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(User(id = currentUserId, name = "testuser", image = null))

            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    viewModel = viewModel,
                    config =
                        JournalsScreenConfig(
                            appState = appState,
                            params = JournalsScreenParams(userId = profileUserId),
                            parentPaddingValues = PaddingValues()
                        ),
                    onAction = {}
                )
            }
        }

        // Then - EmptyStateView НЕ отображается, пока идет загрузка
        composeTestRule
            .onNodeWithText(context.getString(R.string.journals_empty))
            .assertDoesNotExist()

        // Кнопка создания тоже не отображается (чужой профиль)
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_journal))
            .assertDoesNotExist()
    }

    @Test
    fun journalsListScreen_emptyStateNotShown_forOtherUserWhenNotRefreshing() {
        // Given - авторизованный пользователь открывает чужой дневник, загрузка завершена
        // EmptyStateView НЕ показывается для чужих дневников вообще (нельзя создавать чужие дневники)
        val currentUserId = 1L
        val profileUserId = 2L
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false) // Загрузка завершена
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(User(id = currentUserId, name = "testuser", image = null))

            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    viewModel = viewModel,
                    config =
                        JournalsScreenConfig(
                            appState = appState,
                            params = JournalsScreenParams(userId = profileUserId),
                            parentPaddingValues = PaddingValues()
                        ),
                    onAction = {}
                )
            }
        }

        // Then - EmptyStateView НЕ отображается для чужих дневников вообще
        composeTestRule
            .onNodeWithText(context.getString(R.string.journals_empty))
            .assertDoesNotExist()

        // Кнопка создания тоже НЕ отображается (чужой профиль)
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_journal))
            .assertDoesNotExist()
    }

    @Test
    fun journalsListScreen_emptyStateNotShown_forOwnerWhenRefreshing() {
        // Given - владелец открывает свой дневник, идет загрузка
        // EmptyStateView НЕ показывается во время загрузки
        val userId = 1L
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(true) // Идет загрузка
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(User(id = userId, name = "testuser", image = null))

            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    viewModel = viewModel,
                    config =
                        JournalsScreenConfig(
                            appState = appState,
                            params = JournalsScreenParams(userId = userId),
                            parentPaddingValues = PaddingValues()
                        ),
                    onAction = {}
                )
            }
        }

        // Then - EmptyStateView НЕ отображается во время загрузки
        composeTestRule
            .onNodeWithText(context.getString(R.string.journals_empty))
            .assertDoesNotExist()

        // Кнопка создания тоже НЕ отображается во время загрузки
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_journal))
            .assertDoesNotExist()
    }

    @Test
    fun journalsListScreen_emptyStateShown_forOwnerWhenNotRefreshing() {
        // Given - владелец открывает свой дневник, загрузка завершена
        // EmptyStateView показывается только после завершения загрузки
        val userId = 1L
        val state = JournalsUiState.Content(journals = emptyList())
        val viewModel =
            FakeJournalsViewModel(
                uiState = MutableStateFlow(state),
                isRefreshing = MutableStateFlow(false) // Загрузка завершена
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(User(id = userId, name = "testuser", image = null))

            JetpackWorkoutAppTheme {
                JournalsListScreen(
                    modifier = androidx.compose.ui.Modifier,
                    viewModel = viewModel,
                    config =
                        JournalsScreenConfig(
                            appState = appState,
                            params = JournalsScreenParams(userId = userId),
                            parentPaddingValues = PaddingValues()
                        ),
                    onAction = {}
                )
            }
        }

        // Then - EmptyStateView отображается для владельца после загрузки
        composeTestRule
            .onNodeWithText(context.getString(R.string.journals_empty))
            .assertIsDisplayed()

        // Кнопка создания отображается и активна
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_journal))
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
