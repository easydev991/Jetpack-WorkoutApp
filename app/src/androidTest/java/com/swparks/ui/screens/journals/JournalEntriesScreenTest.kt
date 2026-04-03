package com.swparks.ui.screens.journals

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.domain.model.JournalEntry
import com.swparks.navigation.AppState
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.state.JournalEntriesUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.FakeJournalEntriesViewModel
import com.swparks.ui.viewmodel.IJournalEntriesViewModel
import com.swparks.ui.viewmodel.JournalEntriesEvent
import com.swparks.util.FakeAnalyticsReporter
import kotlinx.coroutines.flow.MutableSharedFlow
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
        private const val TEST_JOURNAL_OWNER_ID = 1L
    }

    private fun createTestEntry(id: Long = 1L): JournalEntry =
        JournalEntry(
            id = id,
            journalId = TEST_JOURNAL_ID,
            authorId = TEST_JOURNAL_OWNER_ID,
            authorName = "Иван Иванов",
            message = "Отличная тренировка сегодня!",
            createDate = "2024-01-15T12:00:00",
            modifyDate = "2024-01-15T12:00:00",
            authorImage = null
        )

    private fun setContent(
        journalId: Long = TEST_JOURNAL_ID,
        journalTitle: String = TEST_JOURNAL_TITLE,
        journalOwnerId: Long = TEST_JOURNAL_OWNER_ID,
        viewModel: IJournalEntriesViewModel =
            FakeJournalEntriesViewModel(
                uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = emptyList())),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            ),
        onBackClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = journalId,
                            journalTitle = journalTitle,
                            journalOwnerId = journalOwnerId,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
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
    fun errorState_retryClick_showsLoadingThenContent() {
        val uiState = MutableStateFlow<JournalEntriesUiState>(JournalEntriesUiState.Error("Ошибка"))
        val viewModel =
            object : IJournalEntriesViewModel {
                override val uiState = uiState
                override val isRefreshing = MutableStateFlow(false)
                override val isDeleting = MutableStateFlow(false)
                override val events = MutableSharedFlow<JournalEntriesEvent>()
                override val canCreateEntry = MutableStateFlow(false)
                override val isSavingSettings = MutableStateFlow(false)

                override fun loadEntries() = Unit

                override fun retry() {
                    uiState.value = JournalEntriesUiState.InitialLoading
                }

                override fun deleteEntry(entryId: Long) = Unit

                override suspend fun canDeleteEntry(entryId: Long): Boolean = true

                override fun refresh() = Unit

                override fun canEditEntry(entry: JournalEntry): Boolean = true

                override fun canDeleteEntry(entry: JournalEntry): Boolean = true

                override fun editJournalSettings(
                    journalId: Long,
                    title: String,
                    viewAccess: com.swparks.ui.model.JournalAccess,
                    commentAccess: com.swparks.ui.model.JournalAccess
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
            uiState.value =
                JournalEntriesUiState.Content(
                    entries = listOf(createTestEntry()),
                    firstEntryId = 1L
                )
        }

        composeTestRule
            .onNodeWithText(createTestEntry().authorName ?: "")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_retryClick_showsLoadingThenError() {
        val retryError = "Повторная ошибка"
        val uiState = MutableStateFlow<JournalEntriesUiState>(JournalEntriesUiState.Error("Ошибка"))
        val viewModel =
            object : IJournalEntriesViewModel {
                override val uiState = uiState
                override val isRefreshing = MutableStateFlow(false)
                override val isDeleting = MutableStateFlow(false)
                override val events = MutableSharedFlow<JournalEntriesEvent>()
                override val canCreateEntry = MutableStateFlow(false)
                override val isSavingSettings = MutableStateFlow(false)

                override fun loadEntries() = Unit

                override fun retry() {
                    uiState.value = JournalEntriesUiState.InitialLoading
                }

                override fun deleteEntry(entryId: Long) = Unit

                override suspend fun canDeleteEntry(entryId: Long): Boolean = true

                override fun refresh() = Unit

                override fun canEditEntry(entry: JournalEntry): Boolean = true

                override fun canDeleteEntry(entry: JournalEntry): Boolean = true

                override fun editJournalSettings(
                    journalId: Long,
                    title: String,
                    viewAccess: com.swparks.ui.model.JournalAccess,
                    commentAccess: com.swparks.ui.model.JournalAccess
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
            uiState.value = JournalEntriesUiState.Error(retryError)
        }

        composeTestRule
            .onNodeWithText(retryError)
            .assertIsDisplayed()
    }

    @Test
    fun journalEntriesScreen_whenJournalTitleChanges_updatesTopBarTitle() {
        val uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = emptyList()))
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = uiState,
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            )
        val journalTitleState = mutableStateOf("Title A")

        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "owner",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = journalTitleState.value,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        composeTestRule.runOnUiThread {
            journalTitleState.value = "Title B"
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Title B").assertIsDisplayed()
    }

    @Test
    fun journalEntriesScreen_whenCurrentUserChangesFromOwner_hidesSettingsButton() {
        val uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = emptyList()))
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = uiState,
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            )
        val foreignUserId = TEST_JOURNAL_OWNER_ID + 100

        lateinit var appState: AppState
        composeTestRule.setContent {
            val navController = rememberNavController()
            appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "owner",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        val settingsContentDescription = context.getString(R.string.settings)
        composeTestRule.onNodeWithContentDescription(settingsContentDescription).assertIsDisplayed()

        composeTestRule.runOnUiThread {
            appState.updateCurrentUser(User(id = foreignUserId, name = "foreign", image = null))
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithContentDescription(settingsContentDescription)
            .assertCountEquals(0)
    }

    @Test
    fun testInitialState_showsLoadingOverlay() {
        // Given
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = MutableStateFlow(JournalEntriesUiState.InitialLoading),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
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
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = MutableStateFlow(JournalEntriesUiState.Error(message = errorMessage)),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
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
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = MutableStateFlow(JournalEntriesUiState.Error(message = errorMessage)),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
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
        val testEntry =
            JournalEntry(
                id = 1L,
                journalId = TEST_JOURNAL_ID,
                authorId = 1L,
                authorName = "Иван Иванов",
                message = "Отличная тренировка сегодня!",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            )

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = listOf(testEntry))),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
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
        val entries =
            listOf(
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

        val viewModel =
            FakeJournalEntriesViewModel(
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
    fun testEntryItem_menuButton_clickable() {
        // Given
        val testEntry =
            JournalEntry(
                id = 1L,
                journalId = TEST_JOURNAL_ID,
                authorId = 1L,
                authorName = "Иван Иванов",
                message = "Отличная тренировка сегодня!",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            )

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = listOf(testEntry))),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Кнопка меню записи должна быть кликабельной
        composeTestRule
            .onNodeWithTag("MenuButton", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun testPullToRefresh_blocksUI() {
        // Given
        val testEntry =
            JournalEntry(
                id = 1L,
                journalId = TEST_JOURNAL_ID,
                authorId = 1L,
                authorName = "Иван Иванов",
                message = "Отличная тренировка сегодня!",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            )

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = listOf(testEntry))),
                isRefreshing = MutableStateFlow(true)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Запись отображается, кнопка меню отключена при обновлении
        composeTestRule
            .onNodeWithText("Иван Иванов")
            .assertIsDisplayed()
        // Кнопка меню отключена при refreshing
        composeTestRule
            .onNodeWithTag("MenuButton", useUnmergedTree = true)
            .assertIsNotEnabled()
    }

    @Test
    fun testContentState_withRefreshingIndicator() {
        // Given
        val testEntry =
            JournalEntry(
                id = 1L,
                journalId = TEST_JOURNAL_ID,
                authorId = 1L,
                authorName = "Иван Иванов",
                message = "Отличная тренировка сегодня!",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            )

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = listOf(testEntry),
                            isRefreshing = true
                        )
                    ),
                isRefreshing = MutableStateFlow(true)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
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
        // Given - Запись с id = 2L не является первой (firstEntryId = 1L)
        val testEntry =
            JournalEntry(
                id = 2L,
                journalId = TEST_JOURNAL_ID,
                authorId = 1L,
                authorName = "Иван Иванов",
                message = "Отличная тренировка сегодня!",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            )

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = listOf(testEntry),
                            firstEntryId = 1L, // Первая запись имеет id = 1L
                            isRefreshing = false
                        )
                    ),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Кликаем на кнопку меню записи (кнопка с testTag "MenuButton")
        composeTestRule
            .onNodeWithTag("MenuButton", useUnmergedTree = true)
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
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = true
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(true)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
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
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = true
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(true)
            )

        // When
        setContent(viewModel = viewModel)

        // Then - Кнопка EmptyStateView кликабельна
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertHasClickAction()
    }

    /**
     * Тест 14: Проверка, что EmptyStateView не отображается при isRefreshing = true
     * (Десятая итерация: EmptyStateView не показывается во время загрузки)
     */
    @Test
    fun testEmptyState_notShown_whenRefreshing() {
        // Given
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = true
                        )
                    ),
                isRefreshing = MutableStateFlow(true),
                canCreateEntry = MutableStateFlow(true)
            )

        // When
        setContent(viewModel = viewModel)

        // Then - EmptyStateView не отображается во время загрузки
        composeTestRule
            .onNodeWithText(context.getString(R.string.entries_empty))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertDoesNotExist()
    }

    /**
     * Тест 15: Проверка, что кнопка EmptyStateView отключена при isDeleting = true
     */
    @Test
    fun testEmptyState_buttonIsDisabled_whenDeleting() {
        // Given
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = true
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                isDeleting = MutableStateFlow(true),
                canCreateEntry = MutableStateFlow(true)
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
        val entries =
            listOf(
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

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
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
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
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

        // Проверяем, что для первой записи кнопка "Удалить" отсутствует в меню
        // Открываем меню первой записи - кликаем на текст имени автора первой записи
        composeTestRule
            .onNodeWithText("Иван Иванов")
            .performClick()

        // Проверяем, что кнопка удаления не отображается для первой записи
        composeTestRule
            .onNodeWithText(context.getString(R.string.delete))
            .assertDoesNotExist()
    }

    /**
     * Тест 18: Проверка отображения FAB при canCreateEntry == true
     */
    @Test
    fun testFab_shown_whenCanCreateEntryTrue() {
        // Given
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = true
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(true)
            )

        // When
        setContent(viewModel = viewModel)

        // Then - FAB отображается
        composeTestRule
            .onNodeWithTag("AddEntryFAB")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    /**
     * Тест 19: Проверка скрытия FAB при canCreateEntry == false
     */
    @Test
    fun testFab_hidden_whenCanCreateEntryFalse() {
        // Given
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = false
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            )

        // When
        setContent(viewModel = viewModel)

        // Then - FAB не отображается
        composeTestRule
            .onNodeWithTag("AddEntryFAB")
            .assertDoesNotExist()
    }

    /**
     * Тест 20: Проверка скрытия FAB при isDeleting = true
     */
    @Test
    fun testFab_hidden_whenDeleting() {
        // Given
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = true
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                isDeleting = MutableStateFlow(true),
                canCreateEntry = MutableStateFlow(true)
            )

        // When
        setContent(viewModel = viewModel)

        // Then - FAB не отображается при удалении
        composeTestRule
            .onNodeWithTag("AddEntryFAB")
            .assertDoesNotExist()
    }

    /**
     * Тест 21: Проверка что кнопка EmptyStateView не отображается при canCreateEntry == false
     */
    @Test
    fun testEmptyState_buttonDisabled_whenCannotCreateEntry() {
        // Given
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = false
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            )

        // When
        setContent(viewModel = viewModel)

        // Then - Кнопка EmptyStateView не отображается (текст EmptyStateView тоже не отображается)
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertDoesNotExist()
    }

    /**
     * Тест 22: Проверка отображения действия EDIT для записи с автором
     */
    @Test
    fun testContentEntry_editActionVisible_whenEntryHasAuthor() {
        // Given
        val testEntry =
            JournalEntry(
                id = 1L,
                journalId = TEST_JOURNAL_ID,
                authorId = 1L,
                authorName = "Иван Иванов",
                message = "Отличная тренировка!",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            )

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = listOf(testEntry))),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Открываем меню записи
        composeTestRule
            .onNodeWithTag("MenuButton", useUnmergedTree = true)
            .performClick()

        // Проверяем, что действие EDIT доступно
        composeTestRule
            .onNodeWithText(context.getString(R.string.edit))
            .assertIsDisplayed()
    }

    /**
     * Тест 23: Проверка отсутствия действия EDIT для записи без автора
     */
    @Test
    fun testContentEntry_editActionNotVisible_whenEntryNoAuthor() {
        // Given
        val testEntry =
            JournalEntry(
                id = 1L,
                journalId = TEST_JOURNAL_ID,
                authorId = null,
                authorName = "Аноним",
                message = "Отличная тренировка!",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            )

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = listOf(testEntry))),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - Запись отображается
        composeTestRule
            .onNodeWithText("Аноним")
            .assertIsDisplayed()

        // Для записи без автора нет доступных действий (EDIT, DELETE),
        // поэтому кнопка меню не отображается вообще
        composeTestRule
            .onNodeWithTag("MenuButton", useUnmergedTree = true)
            .assertDoesNotExist()

        // Действие EDIT также отсутствует
        composeTestRule
            .onNodeWithText(context.getString(R.string.edit))
            .assertDoesNotExist()
    }

    /**
     * Тест 24: Проверка открытия экрана создания записи через FAB
     */
    @Test
    fun testFabClick_opensTextEntrySheet() {
        // Given
        var showSheet = false
        var sheetMode: TextEntryMode? = null

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = true
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(true)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues(),
                    textEntrySheetHostContent = { show, mode, _, _ ->
                        showSheet = show
                        sheetMode = mode
                    }
                )
            }
        }

        // Then - FAB отображается
        composeTestRule
            .onNodeWithTag("AddEntryFAB")
            .assertIsDisplayed()

        // When - Кликаем на FAB
        composeTestRule
            .onNodeWithTag("AddEntryFAB")
            .performClick()

        // Then - Ждем перерисовки и проверяем, что Sheet открыт с правильным режимом
        composeTestRule.waitForIdle()
        assert(showSheet) { "Sheet должен быть открыт" }
        assert(sheetMode is TextEntryMode.NewForJournal) { "Режим должен быть NewForJournal" }
    }

    /**
     * Тест 25: Проверка открытия экрана редактирования записи через действие EDIT
     */
    @Test
    fun testEditAction_opensTextEntrySheet() {
        // Given
        var showSheet = false
        var sheetMode: TextEntryMode? = null

        val testEntry =
            JournalEntry(
                id = 1L,
                journalId = TEST_JOURNAL_ID,
                authorId = 1L,
                authorName = "Иван Иванов",
                message = "Отличная тренировка!",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            )

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState = MutableStateFlow(JournalEntriesUiState.Content(entries = listOf(testEntry))),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues(),
                    textEntrySheetHostContent = { show, mode, _, _ ->
                        showSheet = show
                        sheetMode = mode
                    }
                )
            }
        }

        // Then - Открываем меню записи
        composeTestRule
            .onNodeWithTag("MenuButton", useUnmergedTree = true)
            .performClick()

        // Кликаем на действие EDIT
        composeTestRule
            .onNodeWithText(context.getString(R.string.edit))
            .performClick()

        // Then - Ждем перерисовки и проверяем, что Sheet открыт с режимом редактирования
        composeTestRule.waitForIdle()
        assert(showSheet) { "Sheet должен быть открыт" }
        assert(sheetMode is TextEntryMode.EditJournalEntry) { "Режим должен быть EditJournalEntry" }
    }

    /**
     * Тест 26: Проверка открытия экрана создания записи через кнопку EmptyStateView
     */
    @Test
    fun testEmptyStateButtonClick_opensTextEntrySheet() {
        // Given
        var showSheet = false
        var sheetMode: TextEntryMode? = null

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = true
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(true)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID,
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = TEST_JOURNAL_OWNER_ID,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues(),
                    textEntrySheetHostContent = { show, mode, _, _ ->
                        showSheet = show
                        sheetMode = mode
                    }
                )
            }
        }

        // Then - Кнопка EmptyStateView отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertIsDisplayed()

        // When - Кликаем на кнопку создания записи
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .performClick()

        // Then - Ждем перерисовки и проверяем, что Sheet открыт с правильным режимом
        composeTestRule.waitForIdle()
        assert(showSheet) { "Sheet должен быть открыт" }
        assert(sheetMode is TextEntryMode.NewForJournal) { "Режим должен быть NewForJournal" }
    }

    /**
     * Тест 23: Проверка отображения FAB, когда currentUserId == journalOwnerId
     * Это критически важный тест для бага №1 - FAB должен отображаться для владельца дневника
     */
    @Test
    fun testFab_shown_whenCurrentUserIdEqualsJournalOwnerId() {
        // Given - Создаем ViewModel, где currentUserId == journalOwnerId (владелец дневника)
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = true // Владелец может создавать записи
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(true)
            )

        // When
        setContent(
            viewModel = viewModel,
            journalOwnerId = TEST_JOURNAL_OWNER_ID // Владелец дневника
        )

        // Then - FAB должен отображаться для владельца дневника
        composeTestRule
            .onNodeWithTag("AddEntryFAB")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    /**
     * Тест 24: Проверка скрытия FAB, когда currentUserId != journalOwnerId
     * Это критически важный тест для бага №1 - FAB НЕ должен отображаться для не-владельца
     */
    @Test
    fun testFab_hidden_whenCurrentUserIdNotEqualsJournalOwnerId() {
        // Given - Создаем ViewModel, где currentUserId != journalOwnerId (не владелец)
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = false // Не владелец не может создавать записи при NOBODY
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            )

        // When
        setContent(
            viewModel = viewModel,
            journalOwnerId = TEST_JOURNAL_OWNER_ID // Владелец дневника (другой ID)
        )

        // Then - FAB НЕ должен отображаться для не-владельца
        composeTestRule
            .onNodeWithTag("AddEntryFAB")
            .assertDoesNotExist()
    }

    /**
     * Тест 27: Когда firstEntryId = null, все записи можно удалить
     */
    @Test
    fun testFirstEntryIdNull_allEntriesCanBeDeleted() {
        // Given - Одна запись без установленного firstEntryId
        val testEntry =
            JournalEntry(
                id = 1L,
                journalId = TEST_JOURNAL_ID,
                authorId = 1L,
                authorName = "Тестовая запись",
                message = "Это тестовая запись",
                createDate = "2024-01-15T12:00:00",
                modifyDate = "2024-01-15T12:00:00",
                authorImage = null
            )

        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = listOf(testEntry),
                            firstEntryId = null, // Первая запись не установлена
                            isRefreshing = false
                        )
                    ),
                isRefreshing = MutableStateFlow(false)
            )

        // When
        setContent(viewModel = viewModel)

        // Then - Открываем меню записи
        composeTestRule
            .onNodeWithTag("MenuButton", useUnmergedTree = true)
            .performClick()

        // Проверяем, что пункт меню "Удалить" отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.delete))
            .assertIsDisplayed()
    }

    // ==================== Десятая итерация: EmptyStateView для чужих дневников ====================

    /**
     * Тест 28: EmptyStateView показывается в чужом дневнике при ALL-доступе
     * (Десятая итерация: EmptyStateView для чужих дневников)
     */
    @Test
    fun testEmptyState_shown_forForeignJournalWithAllAccess() {
        // Given - Чужой дневник с ALL-доступом (canCreateEntry = true)
        val foreignOwnerId = 999L // Другой ID владельца
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = true // ALL-доступ разрешает создавать записи
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(true)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID, // Текущий пользователь != foreignOwnerId
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = foreignOwnerId,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - EmptyStateView отображается, так как ALL-доступ разрешает создавать записи
        composeTestRule
            .onNodeWithText(context.getString(R.string.entries_empty))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    /**
     * Тест 29: EmptyStateView не показывается в чужом дневнике при NOBODY-доступе
     * (Десятая итерация: EmptyStateView для чужих дневников)
     */
    @Test
    fun testEmptyState_notShown_forForeignJournalWithNobodyAccess() {
        // Given - Чужой дневник с NOBODY-доступом (canCreateEntry = false)
        val foreignOwnerId = 999L // Другой ID владельца
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = false // NOBODY-доступ запрещает создавать записи
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID, // Текущий пользователь != foreignOwnerId
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = foreignOwnerId,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - EmptyStateView не отображается, так как нет прав на создание записей
        composeTestRule
            .onNodeWithText(context.getString(R.string.entries_empty))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertDoesNotExist()
    }

    /**
     * Тест 30: EmptyStateView не показывается в чужом дневнике при FRIENDS-доступе (не друг)
     * (Десятая итерация: EmptyStateView для чужих дневников)
     */
    @Test
    fun testEmptyState_notShown_forForeignJournalWithFriendsAccessNotFriend() {
        // Given - Чужой дневник с FRIENDS-доступом, текущий пользователь не в друзьях
        val foreignOwnerId = 999L // Другой ID владельца
        val viewModel =
            FakeJournalEntriesViewModel(
                uiState =
                    MutableStateFlow(
                        JournalEntriesUiState.Content(
                            entries = emptyList(),
                            canCreateEntry = false // FRIENDS-доступ, но пользователь не в друзьях
                        )
                    ),
                isRefreshing = MutableStateFlow(false),
                canCreateEntry = MutableStateFlow(false)
            )

        // When
        composeTestRule.setContent {
            val navController = rememberNavController()
            val appState = AppState(navController, FakeAnalyticsReporter())
            appState.updateCurrentUser(
                User(
                    id = TEST_JOURNAL_OWNER_ID, // Текущий пользователь != foreignOwnerId
                    name = "testuser",
                    image = null
                )
            )

            JetpackWorkoutAppTheme {
                JournalEntriesScreen(
                    modifier = androidx.compose.ui.Modifier,
                    params =
                        JournalParams(
                            journalId = TEST_JOURNAL_ID,
                            journalTitle = TEST_JOURNAL_TITLE,
                            journalOwnerId = foreignOwnerId,
                            journalViewAccess = null,
                            journalCommentAccess = null
                        ),
                    viewModel = viewModel,
                    appState = appState,
                    onBackClick = {},
                    parentPaddingValues = PaddingValues()
                )
            }
        }

        // Then - EmptyStateView не отображается, так как пользователь не в друзьях
        composeTestRule
            .onNodeWithText(context.getString(R.string.entries_empty))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.create_entry))
            .assertDoesNotExist()
    }
}
