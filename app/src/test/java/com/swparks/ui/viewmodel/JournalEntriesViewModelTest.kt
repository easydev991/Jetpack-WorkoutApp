package com.swparks.ui.viewmodel

import android.util.Log
import app.cash.turbine.test
import com.swparks.domain.model.JournalEntry
import com.swparks.domain.usecase.IDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IGetJournalEntriesUseCase
import com.swparks.domain.usecase.ISyncJournalEntriesUseCase
import com.swparks.ui.state.JournalEntriesUiState
import com.swparks.util.ErrorReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit тесты для JournalEntriesViewModel.
 *
 * Тестирует управление состоянием экрана списка записей в дневнике,
 * включая загрузку данных, обработку ошибок и pull-to-refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JournalEntriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getJournalEntriesUseCase: IGetJournalEntriesUseCase
    private lateinit var syncJournalEntriesUseCase: ISyncJournalEntriesUseCase
    private lateinit var deleteJournalEntryUseCase: IDeleteJournalEntryUseCase
    private lateinit var canDeleteJournalEntryUseCase: com.swparks.domain.usecase.ICanDeleteJournalEntryUseCase
    private lateinit var errorReporter: ErrorReporter
    private lateinit var viewModel: JournalEntriesViewModel

    private val testUserId = 1L
    private val testJournalId = 10L
    private val testEntry = JournalEntry(
        id = 1L,
        journalId = testJournalId,
        authorId = testUserId,
        authorName = "Тестовый автор",
        message = "Тестовое сообщение",
        createDate = "2024-01-01T10:00:00",
        modifyDate = "2024-01-15T15:30:00",
        authorImage = "https://example.com/avatar.jpg"
    )

    @Before
    fun setup() {
        // Мокируем статический Log для использования в тестах
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0

        getJournalEntriesUseCase = mockk(relaxed = true)
        syncJournalEntriesUseCase = mockk(relaxed = true)
        deleteJournalEntryUseCase = mockk(relaxed = true)
        canDeleteJournalEntryUseCase = mockk(relaxed = true)
        errorReporter = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testInitialState_isInitialLoading() {
        // Given
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )

        // Then
        val initialState = viewModel.uiState.value
        assertTrue(
            "Начальное состояние должно быть InitialLoading",
            initialState is JournalEntriesUiState.InitialLoading
        )
    }

    @Test
    fun testLoadEntries_success_showsContent() = runTest {
        // Given
        val entries = listOf(testEntry)
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(entries)
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalEntriesUiState.Content
        )
        val contentState = state as JournalEntriesUiState.Content
        assertEquals(
            "Количество записей должно совпадать",
            entries.size,
            contentState.entries.size
        )
        assertEquals(
            "Первая запись должна совпадать",
            testEntry.id,
            contentState.entries[0].id
        )
    }

    @Test
    fun testLoadEntries_success_updatesIsRefreshing() = runTest {
        // Given
        val entries = listOf(testEntry)
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(entries)
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Then
        val isRefreshing = viewModel.isRefreshing.value
        assertFalse(
            "Флаг обновления должен быть false после успешной загрузки",
            isRefreshing
        )
    }

    @Test
    fun testLoadEntries_error_showsError() = runTest {
        // Given
        val errorMessage = "Ошибка сети"
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(emptyList())
        coEvery { syncJournalEntriesUseCase(testUserId, testJournalId) } returns Result.failure(
            Exception(errorMessage)
        )

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Error при неудачной загрузке",
            state is JournalEntriesUiState.Error
        )
        val errorState = state as JournalEntriesUiState.Error
        assertEquals(
            "Сообщение об ошибке должно содержать 'Ошибка загрузки записей'",
            "Ошибка загрузки записей",
            errorState.message
        )
    }

    @Test
    fun testLoadEntries_error_emptyList_showsError() = runTest {
        // Given
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(emptyList())
        coEvery { syncJournalEntriesUseCase(testUserId, testJournalId) } returns Result.failure(
            Exception("Ошибка сети")
        )

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "При пустом списке и ошибке должно быть состояние Error",
            state is JournalEntriesUiState.Error
        )
    }

    @Test
    fun testLoadEntries_error_nonEmptyList_keepsContent() = runTest {
        // Given
        val entries = listOf(testEntry)
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(entries)
        coEvery { syncJournalEntriesUseCase(testUserId, testJournalId) } returns Result.failure(
            Exception("Ошибка сети")
        )

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "При наличии записей и ошибке должно сохраняться состояние Content",
            state is JournalEntriesUiState.Content
        )
        val contentState = state as JournalEntriesUiState.Content
        assertEquals(
            "Количество записей должно быть сохранено",
            entries.size,
            contentState.entries.size
        )
    }

    @Test
    fun testRetry_callsLoadEntries() = runTest {
        // Given
        val entries = listOf(testEntry)
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(entries)
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Сбрасываем верификацию (init уже вызвал loadEntries)
        coVerify(atLeast = 1) { syncJournalEntriesUseCase(testUserId, testJournalId) }

        // When
        viewModel.retry()
        advanceUntilIdle()

        // Then
        coVerify(atLeast = 2) { syncJournalEntriesUseCase(testUserId, testJournalId) }
    }

    @Test
    fun testLoadEntries_logsSuccess() = runTest {
        // Given
        val entries = listOf(testEntry)
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(entries)
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Then
        verify(atLeast = 1) {
            Log.i(
                withArg<String> { tag ->
                    assertTrue(
                        "Tag должен содержать JournalEntriesViewModel",
                        tag.contains("JournalEntriesViewModel")
                    )
                },
                withArg<String> { message ->
                    assertTrue(
                        "Сообщение должно содержать 'Синхронизация записей успешна'",
                        message.contains("Синхронизация записей успешна")
                    )
                }
            )
        }
    }

    @Test
    fun testLoadEntries_logsError() = runTest {
        // Given
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(emptyList())
        coEvery { syncJournalEntriesUseCase(testUserId, testJournalId) } returns Result.failure(
            Exception("Ошибка сети")
        )

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Then
        verify(atLeast = 1) {
            Log.e(
                withArg<String> { tag ->
                    assertTrue(
                        "Tag должен содержать JournalEntriesViewModel",
                        tag.contains("JournalEntriesViewModel")
                    )
                },
                withArg<String> { message ->
                    assertTrue(
                        "Сообщение должно содержать 'Ошибка при синхронизации записей'",
                        message.contains("Ошибка при синхронизации записей")
                    )
                }
            )
        }
    }

    @Test
    fun testLoadEntries_concurrentCalls_handled() = runTest {
        // Given
        val entries = listOf(testEntry)
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(entries)
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Имитируем конкурентные вызовы
        viewModel.loadEntries()
        viewModel.loadEntries()
        viewModel.loadEntries()
        advanceUntilIdle()

        // Then
        // Проверяем, что состояние остается Content и не было исключений
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalEntriesUiState.Content
        )
        val contentState = state as JournalEntriesUiState.Content
        assertEquals(
            "Количество записей должно сохраняться",
            entries.size,
            contentState.entries.size
        )
        // Проверяем, что флаг обновления в конечном итоге сбрасывается
        assertFalse(
            "Флаг обновления должен быть false после завершения",
            viewModel.isRefreshing.value
        )
    }

    // ==================== ТЕСТЫ ДЛЯ УДАЛЕНИЯ ЗАПИСЕЙ ====================

    /**
     * Тест 11: Успешное удаление записи эмитит событие Snackbar
     */
    @Test
    fun testDeleteEntry_success_emitsSnackbarEvent() = runTest {
        // Given
        val testEntryId = 1L
        coEvery {
            deleteJournalEntryUseCase(testUserId, testJournalId, testEntryId)
        } returns Result.success(Unit)

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Then - подписываемся на события перед вызовом deleteEntry
        viewModel.events.test {
            viewModel.deleteEntry(testEntryId)
            advanceUntilIdle()

            // Проверяем, что события эмитятся корректно
            val event = awaitItem()
            assertTrue(
                "Должно быть событие ShowSnackbar",
                event is JournalEntriesEvent.ShowSnackbar
            )
            val snackbarEvent = event as JournalEntriesEvent.ShowSnackbar
            assertEquals(
                "Сообщение об успешном удалении",
                "Запись удалена",
                snackbarEvent.message
            )
        }
    }

    /**
     * Тест 12: Ошибка при удалении записи эмитит событие Snackbar с текстом ошибки
     */
    @Test
    fun testDeleteEntry_failure_emitsSnackbarEventWithError() = runTest {
        // Given
        val testEntryId = 1L
        val errorMessage = "Ошибка доступа"
        coEvery {
            deleteJournalEntryUseCase(testUserId, testJournalId, testEntryId)
        } returns Result.failure(Exception(errorMessage))

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Then - подписываемся на события перед вызовом deleteEntry
        viewModel.events.test {
            viewModel.deleteEntry(testEntryId)
            advanceUntilIdle()

            // Проверяем, что событие содержит текст ошибки
            val event = awaitItem()
            assertTrue(
                "Должно быть событие ShowSnackbar",
                event is JournalEntriesEvent.ShowSnackbar
            )
            val snackbarEvent = event as JournalEntriesEvent.ShowSnackbar
            assertEquals(
                "Сообщение об ошибке",
                errorMessage,
                snackbarEvent.message
            )
        }
    }

    /**
     * Тест 13: deleteEntry вызывает use case с правильными параметрами
     */
    @Test
    fun testDeleteEntry_callsUseCaseWithCorrectParameters() = runTest {
        // Given
        val testEntryId = 123L
        coEvery {
            deleteJournalEntryUseCase(testUserId, testJournalId, testEntryId)
        } returns Result.success(Unit)

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        viewModel.deleteEntry(testEntryId)
        advanceUntilIdle()

        // Then - проверяем, что use case был вызван с правильными параметрами
        coVerify(exactly = 1) {
            deleteJournalEntryUseCase(
                userId = testUserId,
                journalId = testJournalId,
                entryId = testEntryId
            )
        }
    }

    /**
     * Тест 14: Удаление несуществующей записи (ошибка без сообщения)
     */
    @Test
    fun testDeleteEntry_failureWithoutMessage_emitsGenericError() = runTest {
        // Given
        val testEntryId = 1L
        coEvery {
            deleteJournalEntryUseCase(testUserId, testJournalId, testEntryId)
        } returns Result.failure(Exception())

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Then - подписываемся на события перед вызовом deleteEntry
        viewModel.events.test {
            viewModel.deleteEntry(testEntryId)
            advanceUntilIdle()

            // Проверяем, что используется сообщение об ошибке по умолчанию
            val event = awaitItem()
            assertTrue(
                "Должно быть событие ShowSnackbar",
                event is JournalEntriesEvent.ShowSnackbar
            )
            val snackbarEvent = event as JournalEntriesEvent.ShowSnackbar
            assertEquals(
                "Сообщение об ошибке по умолчанию",
                "Ошибка удаления",
                snackbarEvent.message
            )
        }
    }

    /**
     * Тест 15: isDeleting возвращается в false после завершения удаления
     */
    @Test
    fun testDeleteEntry_isDeletingReturnsToFalseAfterCompletion() = runTest {
        // Given
        val testEntryId = 1L
        coEvery {
            deleteJournalEntryUseCase(testUserId, testJournalId, testEntryId)
        } returns Result.success(Unit)

        // When
        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )
        advanceUntilIdle()

        // Проверяем, что initially isDeleting = false
        assertFalse(
            "Изначально isDeleting должен быть false",
            viewModel.isDeleting.value
        )

        // Вызываем удаление и ждем завершения
        viewModel.deleteEntry(testEntryId)
        advanceUntilIdle()

        // Проверяем, что isDeleting сбросился в false
        assertFalse(
            "После завершения удаления isDeleting должен быть false",
            viewModel.isDeleting.value
        )
    }

    // ==================== ТЕСТЫ ДЛЯ ПРОВЕРКИ ВОЗМОЖНОСТИ УДАЛЕНИЯ ====================

    /**
     * Тест 16: Первую запись нельзя удалить
     */
    @Test
    fun testCanDeleteEntry_firstEntry_returnsFalse() = runTest {
        // Given
        coEvery { canDeleteJournalEntryUseCase(1L, testJournalId) } returns false

        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )

        // When
        val result = viewModel.canDeleteEntry(1L)

        // Then
        assertEquals(false, result)
        coVerify(exactly = 1) { canDeleteJournalEntryUseCase(1L, testJournalId) }
    }

    /**
     * Тест 17: Не первую запись можно удалить
     */
    @Test
    fun testCanDeleteEntry_notFirstEntry_returnsTrue() = runTest {
        // Given
        coEvery { canDeleteJournalEntryUseCase(2L, testJournalId) } returns true

        viewModel = JournalEntriesViewModel(
            testUserId,
            testJournalId,
            getJournalEntriesUseCase,
            syncJournalEntriesUseCase,
            deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase,
            errorReporter
        )

        // When
        val result = viewModel.canDeleteEntry(2L)

        // Then
        assertEquals(true, result)
        coVerify(exactly = 1) { canDeleteJournalEntryUseCase(2L, testJournalId) }
    }
}
