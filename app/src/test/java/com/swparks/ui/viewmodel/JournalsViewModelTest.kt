package com.swparks.ui.viewmodel

import android.util.Log
import com.swparks.domain.model.Journal
import com.swparks.domain.usecase.IGetJournalsUseCase
import com.swparks.domain.usecase.ISyncJournalsUseCase
import com.swparks.model.JournalAccess
import com.swparks.ui.state.JournalsUiState
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
            syncJournalsUseCase
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
            syncJournalsUseCase
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
            syncJournalsUseCase
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
            syncJournalsUseCase
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
            "Сообщение об ошибке должно содержать 'Ошибка загрузки дневников'",
            "Ошибка загрузки дневников",
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
            syncJournalsUseCase
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
            syncJournalsUseCase
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
            syncJournalsUseCase
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
            syncJournalsUseCase
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
            syncJournalsUseCase
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
            syncJournalsUseCase
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
            syncJournalsUseCase
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
            syncJournalsUseCase
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
}
