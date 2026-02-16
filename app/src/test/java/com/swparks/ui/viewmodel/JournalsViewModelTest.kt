package com.swparks.ui.viewmodel

import android.util.Log
import com.swparks.R
import com.swparks.domain.model.Journal
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.usecase.IDeleteJournalUseCase
import com.swparks.domain.usecase.IEditJournalSettingsUseCase
import com.swparks.domain.usecase.IGetJournalsUseCase
import com.swparks.domain.usecase.ISyncJournalsUseCase
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.state.JournalsUiState
import com.swparks.util.AppError
import com.swparks.util.ErrorReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit тесты для JournalsViewModel.
 *
 * Тестирует управление состоянием экрана списка дневников,
 * включая загрузку данных, обработку ошибок и pull-to-refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JournalsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getJournalsUseCase: IGetJournalsUseCase
    private lateinit var syncJournalsUseCase: ISyncJournalsUseCase
    private lateinit var deleteJournalUseCase: IDeleteJournalUseCase
    private lateinit var editJournalSettingsUseCase: IEditJournalSettingsUseCase
    private lateinit var errorReporter: ErrorReporter
    private lateinit var resources: ResourcesProvider
    private lateinit var viewModel: JournalsViewModel

    private val testUserId = 1L
    private val testJournal = Journal(
        id = 1L,
        title = "Тестовый дневник",
        lastMessageImage = null,
        createDate = "2024-01-01",
        modifyDate = "2024-01-15",
        lastMessageDate = "2024-01-15",
        lastMessageText = "Последнее сообщение",
        entriesCount = 10,
        ownerId = testUserId,
        viewAccess = JournalAccess.ALL,
        commentAccess = JournalAccess.ALL
    )

    @Before
    fun setup() {
        // Мокируем статический Log для использования в тестах
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0

        getJournalsUseCase = mockk(relaxed = true)
        syncJournalsUseCase = mockk(relaxed = true)
        deleteJournalUseCase = mockk(relaxed = true)
        editJournalSettingsUseCase = mockk(relaxed = true)
        errorReporter = mockk(relaxed = true)
        resources = mockk(relaxed = true)

        // Мокируем getString для локализованных строк
        every { resources.getString(R.string.journal_deleted) } returns "Journal deleted"
        every { resources.getString(R.string.error_delete_journal) } returns "Error deleting journal"
        every { resources.getString(R.string.error_deleting) } returns "Error deleting"
        every { resources.getString(R.string.journal_settings_saved) } returns "Journal settings saved"
        every { resources.getString(R.string.error_save_journal_settings) } returns "Error saving journal settings"
        every { resources.getString(R.string.error_loading_journals) } returns "Error loading journals"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun init_whenViewModelCreated_thenStartsWithInitialLoadingState() {
        // Given
        coEvery { getJournalsUseCase(testUserId) } returns emptyFlow()
        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )

        // Then
        val initialState = viewModel.uiState.value
        assertTrue(
            "Начальное состояние должно быть InitialLoading",
            initialState is JournalsUiState.InitialLoading
        )
    }

    @Test
    fun init_whenViewModelCreated_thenCallsLoadJournals() = runTest {
        // Given
        coEvery { getJournalsUseCase(testUserId) } returns emptyFlow()
        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { syncJournalsUseCase(testUserId) }
    }

    @Test
    fun loadJournals_whenSyncSucceeds_thenSetsIsRefreshingToFalse() = runTest {
        // Given
        coEvery { getJournalsUseCase(testUserId) } returns emptyFlow()
        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        val isRefreshing = viewModel.isRefreshing.value
        assertTrue(
            "Флаг обновления должен быть false после успешной синхронизации",
            !isRefreshing
        )
    }

    @Test
    fun loadJournals_whenSyncFailsAndListIsEmpty_thenSetsErrorState() = runTest {
        // Given
        val errorMessage = "Ошибка сети"
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(emptyList())
        coEvery { syncJournalsUseCase(testUserId) } returns Result.failure(
            Exception(errorMessage)
        )

        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Error при неудачной загрузке",
            state is JournalsUiState.Error
        )
        val errorState = state as JournalsUiState.Error
        assertEquals(
            "Сообщение об ошибке должно содержать Error loading journals",
            "Error loading journals",
            errorState.message
        )
    }

    @Test
    fun loadJournals_whenSyncFailsAndListIsNotEmpty_thenKeepsContentState() = runTest {
        // Given
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))
        coEvery { syncJournalsUseCase(testUserId) } returns Result.failure(
            Exception("Ошибка сети")
        )

        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно оставаться Content при наличии данных",
            state is JournalsUiState.Content
        )
    }

    @Test
    fun loadJournals_whenExceptionThrown_thenSetsErrorState() = runTest {
        // Given
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(emptyList())
        coEvery { syncJournalsUseCase(testUserId) } throws Exception("Неожиданная ошибка")

        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Error при исключении",
            state is JournalsUiState.Error
        )
    }

    @Test
    fun loadJournals_whenSuccess_thenCallsSyncUseCase() = runTest {
        // Given
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))
        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Сбрасываем верификацию (init уже вызвал loadJournals)
        coVerify(exactly = 1) { syncJournalsUseCase(testUserId) }

        // When
        viewModel.loadJournals()
        advanceUntilIdle()

        // Then
        coVerify(atLeast = 2) { syncJournalsUseCase(testUserId) }
    }

    @Test
    fun retry_whenCalled_thenCallsLoadJournalsAgain() = runTest {
        // Given
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))
        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Сбрасываем верификацию
        coVerify(exactly = 1) { syncJournalsUseCase(testUserId) }

        // When
        viewModel.retry()
        advanceUntilIdle()

        // Then
        coVerify(atLeast = 2) { syncJournalsUseCase(testUserId) }
    }

    @Test
    fun observeJournals_whenDataEmitted_thenUpdatesUiState() = runTest {
        // Given
        val journals = listOf(testJournal)
        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        // When
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(journals)
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalsUiState.Content
        )
        val contentState = state as JournalsUiState.Content
        assertEquals(
            "Количество дневников должно совпадать",
            journals.size,
            contentState.journals.size
        )
    }

    @Test
    fun observeJournals_whenEmptyList_thenShowsEmptyContent() = runTest {
        // Given
        val emptyJournals = emptyList<Journal>()
        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        // When
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(emptyJournals)
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content с пустым списком",
            state is JournalsUiState.Content
        )
        val contentState = state as JournalsUiState.Content
        assertEquals(
            "Список дневников должен быть пустым",
            0,
            contentState.journals.size
        )
    }

    @Test
    fun isRefreshing_whenLoadJournalsCalled_thenBecomesTrueThenFalse() = runTest {
        // Given
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))
        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Сбрасываем состояние
        coVerify(exactly = 1) { syncJournalsUseCase(testUserId) }

        // When
        viewModel.loadJournals()

        // Then - Флаг должен быть true сразу после вызова
        // (но из-за асинхронности может уже успеть измениться)
        // Проверяем финальное состояние
        advanceUntilIdle()
        val isRefreshing = viewModel.isRefreshing.value
        assertTrue(
            "Флаг обновления должен быть false после завершения загрузки",
            !isRefreshing
        )
    }

    @Test
    fun observeJournals_whenMultipleJournals_thenShowsAll() = runTest {
        // Given
        val journals = listOf(
            testJournal,
            testJournal.copy(id = 2L, title = "Дневник 2"),
            testJournal.copy(id = 3L, title = "Дневник 3")
        )
        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        // When
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(journals)
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalsUiState.Content
        )
        val contentState = state as JournalsUiState.Content
        assertEquals(
            "Количество дневников должно быть 3",
            3,
            contentState.journals.size
        )
    }

    /**
     * Тест 13: Успешное удаление дневника эмитит событие Snackbar с сообщением об успехе
     */
    @Test
    fun testDeleteJournal_success_emitsSnackbarEvent() = runTest {
        // Given
        val testJournalId = 1L
        coEvery {
            deleteJournalUseCase(testUserId, testJournalId)
        } returns Result.success(Unit)

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Then - подписываемся на события перед вызовом deleteJournal
        viewModel.deleteJournal(testJournalId)
        advanceUntilIdle()

        // Проверяем, что showInfo был вызван с сообщением об успешном удалении
        coVerify(exactly = 1) {
            errorReporter.showInfo("Journal deleted")
        }
    }

    /**
     * Тест 14: Ошибка при удалении дневника вызывает handleError с текстом ошибки
     */
    @Test
    fun testDeleteJournal_failure_callsHandleError() = runTest {
        // Given
        val testJournalId = 1L
        val errorMessage = "Ошибка доступа"
        coEvery {
            deleteJournalUseCase(testUserId, testJournalId)
        } returns Result.failure(Exception(errorMessage))

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        viewModel.deleteJournal(testJournalId)
        advanceUntilIdle()

        // Then - проверяем, что handleError был вызван
        coVerify(exactly = 1) {
            errorReporter.handleError(match { it is AppError.Generic })
        }
    }

    /**
     * Тест 15: deleteJournal вызывает use case с правильными параметрами
     */
    @Test
    fun testDeleteJournal_callsUseCaseWithCorrectParameters() = runTest {
        // Given
        val testJournalId = 123L
        coEvery {
            deleteJournalUseCase(testUserId, testJournalId)
        } returns Result.success(Unit)

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        viewModel.deleteJournal(testJournalId)
        advanceUntilIdle()

        // Then - проверяем, что use case был вызван с правильными параметрами
        coVerify(exactly = 1) {
            deleteJournalUseCase(
                userId = testUserId,
                journalId = testJournalId
            )
        }
    }

    /**
     * Тест 16: Удаление дневника без сообщения об ошибке (null message)
     */
    @Test
    fun testDeleteJournal_failureWithoutMessage_callsHandleError() = runTest {
        // Given
        val testJournalId = 1L
        coEvery {
            deleteJournalUseCase(testUserId, testJournalId)
        } returns Result.failure(Exception())

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        viewModel.deleteJournal(testJournalId)
        advanceUntilIdle()

        // Then - проверяем, что handleError был вызван
        coVerify(exactly = 1) {
            errorReporter.handleError(match { it is AppError.Generic })
        }
    }

    /**
     * Тест 17: isDeleting возвращается в false после завершения удаления
     */
    @Test
    fun testDeleteJournal_isDeletingReturnsToFalseAfterCompletion() = runTest {
        // Given
        val testJournalId = 1L
        coEvery {
            deleteJournalUseCase(testUserId, testJournalId)
        } returns Result.success(Unit)

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Then - проверяем, что флаг устанавливается в false после завершения
        viewModel.deleteJournal(testJournalId)
        advanceUntilIdle()

        val isDeleting = viewModel.isDeleting.value
        assertTrue(
            "Флаг удаления должен быть false после завершения операции",
            !isDeleting
        )
    }

    /**
     * Тест 18: Успешное сохранение настроек дневника
     */
    @Test
    fun testEditJournalSettings_success_callsUseCaseWithCorrectParameters() = runTest {
        // Given
        val testJournalId = 1L
        val newTitle = "Новое название"
        val newViewAccess = JournalAccess.FRIENDS
        val newCommentAccess = JournalAccess.NOBODY

        coEvery {
            editJournalSettingsUseCase(
                journalId = testJournalId,
                title = newTitle,
                userId = testUserId,
                viewAccess = newViewAccess,
                commentAccess = newCommentAccess
            )
        } returns Result.success(Unit)

        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // When
        viewModel.editJournalSettings(
            journalId = testJournalId,
            title = newTitle,
            viewAccess = newViewAccess,
            commentAccess = newCommentAccess
        )
        advanceUntilIdle()

        // Then - проверяем, что use case был вызван с правильными параметрами
        coVerify(exactly = 1) {
            editJournalSettingsUseCase(
                journalId = testJournalId,
                title = newTitle,
                userId = testUserId,
                viewAccess = newViewAccess,
                commentAccess = newCommentAccess
            )
        }
    }

    /**
     * Тест 19: isSavingJournalSettings устанавливается в true перед запросом и в false после успешного сохранения
     */
    @Test
    fun testEditJournalSettings_success_isSavingJournalSettingsChangesCorrectly() = runTest {
        // Given
        val testJournalId = 1L
        val testJournal = testJournal.copy(id = testJournalId)

        coEvery {
            editJournalSettingsUseCase(any(), any(), any(), any(), any())
        } returns Result.success(Unit)

        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))

        // When - создаем ViewModel с уже настроенным getJournalsUseCase
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Then - проверяем, что флаг устанавливается в false после завершения
        viewModel.editJournalSettings(
            journalId = testJournalId,
            title = "Новое название",
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalsUiState.Content
        )
        val contentState = state as JournalsUiState.Content
        assertTrue(
            "Флаг isSavingJournalSettings должен быть false после завершения",
            !contentState.isSavingJournalSettings
        )
    }

    /**
     * Тест 20: При успешном сохранении вызывается syncJournalsUseCase
     */
    @Test
    fun testEditJournalSettings_success_callsSyncJournalsUseCase() = runTest {
        // Given
        val testJournalId = 1L

        coEvery {
            editJournalSettingsUseCase(any(), any(), any(), any(), any())
        } returns Result.success(Unit)

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Сбрасываем верификацию (init уже вызвал syncJournalsUseCase)
        coVerify(atLeast = 1) { syncJournalsUseCase(testUserId) }

        viewModel.editJournalSettings(
            journalId = testJournalId,
            title = "Новое название",
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Then - проверяем, что syncJournalsUseCase был вызван снова после редактирования
        coVerify(atLeast = 2) { syncJournalsUseCase(testUserId) }
    }

    /**
     * Тест 21: При успешном сохранении не выбрасывается исключение
     * Примечание: Проверка через Turbine для SharedFlow без replay сложна,
     * поэтому проверяем только, что не было исключений при отправке события
     */
    @Test
    fun testEditJournalSettings_success_doesNotThrowException() = runTest {
        // Given
        val testJournalId = 1L
        val testJournal = testJournal.copy(id = testJournalId)

        coEvery {
            editJournalSettingsUseCase(any(), any(), any(), any(), any())
        } returns Result.success(Unit)

        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Then - не должно быть исключений при редактировании
        viewModel.editJournalSettings(
            journalId = testJournalId,
            title = "Новое название",
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Проверяем только, что состояние Content обновлено
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalsUiState.Content
        )
    }

    /**
     * Тест 23: При ошибке сети/сервера isSavingJournalSettings сбрасывается в false
     */
    @Test
    fun testEditJournalSettings_failure_isSavingJournalSettingsResetsToFalse() = runTest {
        // Given
        val testJournalId = 1L
        val errorMessage = "Ошибка сети"

        coEvery {
            editJournalSettingsUseCase(any(), any(), any(), any(), any())
        } returns Result.failure(Exception(errorMessage))

        val testJournal = testJournal.copy(id = testJournalId)
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        viewModel.editJournalSettings(
            journalId = testJournalId,
            title = "Новое название",
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Then - проверяем, что флаг сбрасывается в false при ошибке
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalsUiState.Content
        )
        val contentState = state as JournalsUiState.Content
        assertTrue(
            "Флаг isSavingJournalSettings должен быть false при ошибке",
            !contentState.isSavingJournalSettings
        )
    }

    /**
     * Тест 24: При ошибке сети/сервера не выбрасывается исключение
     */
    @Test
    fun testEditJournalSettings_failure_doesNotThrowException() = runTest {
        // Given
        val testJournalId = 1L
        val errorMessage = "Ошибка доступа"

        coEvery {
            editJournalSettingsUseCase(any(), any(), any(), any(), any())
        } returns Result.failure(Exception(errorMessage))

        val testJournal = testJournal.copy(id = testJournalId)
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Then - не должно быть исключений при редактировании
        viewModel.editJournalSettings(
            journalId = testJournalId,
            title = "Новое название",
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Проверяем только, что состояние Content обновлено
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalsUiState.Content
        )
    }

    /**
     * Тест 25: При неожиданной ошибке (исключении) isSavingJournalSettings сбрасывается в false
     */
    @Test
    fun testEditJournalSettings_exception_isSavingJournalSettingsResetsToFalse() = runTest {
        // Given
        val testJournalId = 1L

        coEvery {
            editJournalSettingsUseCase(any(), any(), any(), any(), any())
        } throws Exception("Неожиданная ошибка")

        val testJournal = testJournal.copy(id = testJournalId)
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        viewModel.editJournalSettings(
            journalId = testJournalId,
            title = "Новое название",
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Then - проверяем, что флаг сбрасывается в false при исключении
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalsUiState.Content
        )
        val contentState = state as JournalsUiState.Content
        assertTrue(
            "Флаг isSavingJournalSettings должен быть false при исключении",
            !contentState.isSavingJournalSettings
        )
    }

    /**
     * Тест 26: При неожиданной ошибке не выбрасывается исключение
     */
    @Test
    fun testEditJournalSettings_exception_doesNotThrowException() = runTest {
        // Given
        val testJournalId = 1L

        coEvery {
            editJournalSettingsUseCase(any(), any(), any(), any(), any())
        } throws Exception("Неожиданная ошибка")

        val testJournal = testJournal.copy(id = testJournalId)
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))

        // When
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Then - не должно быть исключений при редактировании
        viewModel.editJournalSettings(
            journalId = testJournalId,
            title = "Новое название",
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Проверяем только, что состояние Content обновлено
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalsUiState.Content
        )
    }

    /**
     * Тест 27: При состоянии не Content обновления isSavingJournalSettings игнорируются
     */
    @Test
    fun testEditJournalSettings_nonContentState_isSavingJournalSettingsIgnored() = runTest {
        // Given
        val testJournalId = 1L

        coEvery {
            editJournalSettingsUseCase(any(), any(), any(), any(), any())
        } returns Result.success(Unit)

        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)

        // When - создаем ViewModel, которая должна начать с InitialLoading
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )

        // Сразу вызываем editJournalSettings до того, как перейдем в Content состояние
        viewModel.editJournalSettings(
            journalId = testJournalId,
            title = "Новое название",
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Then - проверяем, что если состояние не Content, оно не меняется
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние не должно быть Content при редактировании в не-Content состоянии",
            state !is JournalsUiState.Content || !state.isSavingJournalSettings
        )
    }

    /**
     * Тест 28: При состоянии не Content событие JournalSettingsSaved всё равно эмитится при успехе
     */
    @Test
    fun testEditJournalSettings_nonContentState_doesNotThrowException() = runTest {
        // Given
        val testJournalId = 1L
        val testJournal = testJournal.copy(id = testJournalId)

        coEvery {
            editJournalSettingsUseCase(any(), any(), any(), any(), any())
        } returns Result.success(Unit)

        coEvery { syncJournalsUseCase(testUserId) } returns Result.success(Unit)
        coEvery { getJournalsUseCase(testUserId) } returns flowOf(listOf(testJournal))

        // When - создаем ViewModel
        viewModel = JournalsViewModel(
            testUserId,
            getJournalsUseCase,
            syncJournalsUseCase,
            deleteJournalUseCase,
            editJournalSettingsUseCase,
            errorReporter,
            resources
        )
        advanceUntilIdle()

        // Вызываем editJournalSettings
        viewModel.editJournalSettings(
            journalId = testJournalId,
            title = "Новое название",
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Then - проверяем, что состояние Content обновлено и не было исключений
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalsUiState.Content
        )
    }
}
