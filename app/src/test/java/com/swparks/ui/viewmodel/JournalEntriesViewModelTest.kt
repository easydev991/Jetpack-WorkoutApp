package com.swparks.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.swparks.R
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.model.JournalEntry
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.usecase.ICanDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IEditJournalSettingsUseCase
import com.swparks.domain.usecase.IGetJournalEntriesUseCase
import com.swparks.domain.usecase.ISyncJournalEntriesUseCase
import com.swparks.ui.model.JournalAccess
import com.swparks.ui.state.JournalEntriesUiState
import com.swparks.util.ErrorReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var canDeleteJournalEntryUseCase: ICanDeleteJournalEntryUseCase
    private lateinit var editJournalSettingsUseCase: IEditJournalSettingsUseCase
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var swRepository: SWRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var errorReporter: ErrorReporter
    private lateinit var resources: ResourcesProvider
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
    private val testFriend = User(
        id = 2L,
        name = "Друг пользователя",
        image = null
    )

    /**
     * Вспомогательная функция для создания ViewModel с JournalEntriesDeps.
     * Настраивает моки для новых зависимостей по умолчанию.
     */
    private fun createViewModel(
        currentUserId: Long? = null,
        journalOwnerId: Long = testUserId,
        friends: List<User> = emptyList(),
        commentAccess: JournalAccess = JournalAccess.NOBODY
    ): JournalEntriesViewModel {
        // Настраиваем моки для preferencesRepository
        every { preferencesRepository.currentUserId } returns MutableStateFlow(currentUserId)

        // Настраиваем моки для swRepository
        every { swRepository.getFriendsFlow() } returns flowOf(friends)

        // Настраиваем моки для savedStateHandle
        every { savedStateHandle.get<String>("commentAccess") } returns commentAccess.name

        val deps = JournalEntriesDeps(
            getJournalEntriesUseCase = getJournalEntriesUseCase,
            syncJournalEntriesUseCase = syncJournalEntriesUseCase,
            deleteJournalEntryUseCase = deleteJournalEntryUseCase,
            canDeleteJournalEntryUseCase = canDeleteJournalEntryUseCase,
            editJournalSettingsUseCase = editJournalSettingsUseCase,
            preferencesRepository = preferencesRepository,
            swRepository = swRepository,
            savedStateHandle = savedStateHandle,
            errorReporter = errorReporter,
            resources = resources
        )

        return JournalEntriesViewModel(journalOwnerId, testJournalId, deps)
    }

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
        editJournalSettingsUseCase = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        savedStateHandle = mockk(relaxed = true)
        errorReporter = mockk(relaxed = true)
        resources = mockk(relaxed = true)

        // Мокируем getString для локализованных строк
        every { resources.getString(R.string.entry_deleted) } returns "Entry deleted"
        every { resources.getString(R.string.error_delete_entry) } returns "Error deleting entry"
        every { resources.getString(R.string.error_deleting) } returns "Error deleting"
        every { resources.getString(R.string.error_loading_entries) } returns "Error loading entries"

        // Настраиваем значения по умолчанию для SavedStateHandle
        every { savedStateHandle.get<String>("commentAccess") } returns "NOBODY"
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
        viewModel = createViewModel()

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
        viewModel = createViewModel()
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
        viewModel = createViewModel()
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
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Error при неудачной загрузке",
            state is JournalEntriesUiState.Error
        )
        val errorState = state as JournalEntriesUiState.Error
        assertEquals(
            "Сообщение об ошибке должно содержать Error loading entries",
            "Error loading entries",
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
        viewModel = createViewModel()
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
        viewModel = createViewModel()
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

        viewModel = createViewModel()
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
        viewModel = createViewModel()
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
        viewModel = createViewModel()
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
                "Entry deleted",
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
        viewModel = createViewModel()
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
        viewModel = createViewModel()
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
        viewModel = createViewModel()
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
                "Error deleting",
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
        viewModel = createViewModel()
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

        viewModel = createViewModel()

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

        viewModel = createViewModel()

        // When
        val result = viewModel.canDeleteEntry(2L)

        // Then
        assertEquals(true, result)
        coVerify(exactly = 1) { canDeleteJournalEntryUseCase(2L, testJournalId) }
    }

    // ==================== ТЕСТЫ ДЛЯ ВОЗМОЖНОСТИ СОЗДАНИЯ ЗАПИСЕЙ ====================

    /**
     * Тест 18: canCreateEntry возвращает true для JournalAccess.ALL при авторизованном пользователе
     */
    @Test
    fun testCanCreateEntry_ALL_withAuthorizedUser_returnsTrue() = runTest {
        // Given
        val currentUserId = 100L
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel(
            currentUserId = currentUserId,
            friends = emptyList(),
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Then
        val canCreate = viewModel.canCreateEntry.value
        assertTrue(
            "При JournalAccess.ALL можно создавать записи авторизованному пользователю",
            canCreate
        )
    }

    /**
     * Тест 19: canCreateEntry возвращает false для JournalAccess.ALL без авторизации
     */
    @Test
    fun testCanCreateEntry_ALL_withoutAuthorization_returnsFalse() = runTest {
        // Given
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel(
            currentUserId = null,
            friends = emptyList(),
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Then
        val canCreate = viewModel.canCreateEntry.value
        assertFalse(
            "Без авторизации нельзя создавать записи",
            canCreate
        )
    }

    /**
     * Тест 20: canCreateEntry возвращает true для JournalAccess.FRIENDS для друга владельца
     */
    @Test
    fun testCanCreateEntry_FRIENDS_isFriend_returnsTrue() = runTest {
        // Given
        val currentUserId = 100L
        val friends =
            listOf(testFriend.copy(id = testUserId)) // текущий пользователь дружит с владельцем
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel(
            currentUserId = currentUserId,
            friends = friends,
            commentAccess = JournalAccess.FRIENDS
        )
        advanceUntilIdle()

        // Then
        val canCreate = viewModel.canCreateEntry.value
        assertTrue(
            "При JournalAccess.FRIENDS друг может создавать записи",
            canCreate
        )
    }

    /**
     * Тест 21: canCreateEntry возвращает false для JournalAccess.FRIENDS для не-друга
     */
    @Test
    fun testCanCreateEntry_FRIENDS_notFriend_returnsFalse() = runTest {
        // Given
        val currentUserId = 100L
        val friends = listOf(testFriend) // владелец не в списке друзей
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel(
            currentUserId = currentUserId,
            friends = friends,
            commentAccess = JournalAccess.FRIENDS
        )
        advanceUntilIdle()

        // Then
        val canCreate = viewModel.canCreateEntry.value
        assertFalse(
            "При JournalAccess.FRIENDS не-друг не может создавать записи",
            canCreate
        )
    }

    /**
     * Тест 22: canCreateEntry возвращает true для JournalAccess.FRIENDS для владельца
     */
    @Test
    fun testCanCreateEntry_FRIENDS_isOwner_returnsTrue() = runTest {
        // Given
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel(
            currentUserId = testUserId, // текущий пользователь - владелец
            friends = emptyList(),
            commentAccess = JournalAccess.FRIENDS
        )
        advanceUntilIdle()

        // Then
        val canCreate = viewModel.canCreateEntry.value
        assertTrue(
            "Владелец всегда может создавать записи",
            canCreate
        )
    }

    /**
     * Тест 23: canCreateEntry возвращает true для JournalAccess.NOBODY для владельца
     */
    @Test
    fun testCanCreateEntry_NOBODY_isOwner_returnsTrue() = runTest {
        // Given
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel(
            currentUserId = testUserId, // текущий пользователь - владелец
            friends = emptyList(),
            commentAccess = JournalAccess.NOBODY
        )
        advanceUntilIdle()

        // Then
        val canCreate = viewModel.canCreateEntry.value
        assertTrue(
            "Владелец может создавать записи даже при JournalAccess.NOBODY",
            canCreate
        )
    }

    /**
     * Тест 24: canCreateEntry возвращает false для JournalAccess.NOBODY для не-владельца
     */
    @Test
    fun testCanCreateEntry_NOBODY_notOwner_returnsFalse() = runTest {
        // Given
        val currentUserId = 100L
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel(
            currentUserId = currentUserId, // текущий пользователь - не владелец
            friends = emptyList(),
            commentAccess = JournalAccess.NOBODY
        )
        advanceUntilIdle()

        // Then
        val canCreate = viewModel.canCreateEntry.value
        assertFalse(
            "При JournalAccess.NOBODY не-владелец не может создавать записи",
            canCreate
        )
    }

    // ==================== ТЕСТ ДЛЯ REFRESH ====================

    /**
     * Тест 25: refresh() вызывает loadEntries() и обновляет список
     */
    @Test
    fun testRefresh_callsLoadEntries() = runTest {
        // Given
        val entries = listOf(testEntry)
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(entries)
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Сбрасываем верификацию (init уже вызвал loadEntries)
        coVerify(atLeast = 1) { syncJournalEntriesUseCase(testUserId, testJournalId) }

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then - проверяем, что loadEntries был вызван снова (внутри refresh)
        coVerify(atLeast = 2) { syncJournalEntriesUseCase(testUserId, testJournalId) }
    }

    // ==================== ТЕСТЫ ДЛЯ РЕДАКТИРОВАНИЯ ЗАПИСЕЙ ====================

    /**
     * Тест 26: canEditEntry возвращает true для автора записи
     */
    @Test
    fun testCanEditEntry_author_returnsTrue() = runTest {
        // Given
        val currentUserId = testUserId
        val entry = testEntry.copy(authorId = currentUserId)
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel(currentUserId = currentUserId)
        advanceUntilIdle()

        val result = viewModel.canEditEntry(entry)

        // Then
        assertTrue(
            "Автор записи может редактировать её",
            result
        )
    }

    /**
     * Тест 27: canEditEntry возвращает false для не-автора записи
     */
    @Test
    fun testCanEditEntry_notAuthor_returnsFalse() = runTest {
        // Given
        val currentUserId = 100L // другой пользователь
        val entry = testEntry.copy(authorId = testUserId) // запись другого автора
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel(currentUserId = currentUserId)
        advanceUntilIdle()

        val result = viewModel.canEditEntry(entry)

        // Then
        assertFalse(
            "Не-автор записи не может редактировать её",
            result
        )
    }

    /**
     * Тест 28: canCreateEntry обновляется в UI State при изменении
     */
    @Test
    fun testCanCreateEntry_updatesInUiState() = runTest {
        // Given
        val currentUserId = testUserId
        val entries = listOf(testEntry)
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(entries)
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel(
            currentUserId = currentUserId,
            commentAccess = JournalAccess.ALL
        )
        advanceUntilIdle()

        // Then - проверяем, что canCreateEntry присутствует в Content состоянии
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalEntriesUiState.Content
        )
        val contentState = state as JournalEntriesUiState.Content
        assertTrue(
            "canCreateEntry должен быть true для владельца при ALL",
            contentState.canCreateEntry
        )
    }

    // ==================== ТЕСТЫ ДЛЯ ПРОВЕРКИ ВЛАДЕЛЬЦА ДНЕВНИКА ====================

    /**
     * Тест 29: canCreateEntry возвращает true, когда currentUserId == journalOwnerId
     */
    @Test
    fun testCanCreateEntry_currentUserIdEqualsJournalOwnerId_returnsTrue() = runTest {
        // Given
        val currentUserId = testUserId // текущий пользователь - владелец дневника
        val journalOwnerId = testUserId // тот же ID
        coEvery { getJournalEntriesUseCase(journalOwnerId, testJournalId) } returns emptyFlow()
        coEvery { syncJournalEntriesUseCase(journalOwnerId, testJournalId) } returns Result.success(
            Unit
        )

        // When
        viewModel = createViewModel(
            currentUserId = currentUserId,
            journalOwnerId = journalOwnerId,
            commentAccess = JournalAccess.NOBODY
        )
        advanceUntilIdle()

        // Then
        val canCreate = viewModel.canCreateEntry.value
        assertTrue(
            "Владелец дневника может создавать записи",
            canCreate
        )
    }

    /**
     * Тест 30: canCreateEntry возвращает false, когда currentUserId != journalOwnerId
     */
    @Test
    fun testCanCreateEntry_currentUserIdNotEqualsJournalOwnerId_returnsFalse() = runTest {
        // Given
        val currentUserId = 100L // другой пользователь
        val journalOwnerId = testUserId // владелец дневника
        coEvery { getJournalEntriesUseCase(journalOwnerId, testJournalId) } returns emptyFlow()
        coEvery { syncJournalEntriesUseCase(journalOwnerId, testJournalId) } returns Result.success(
            Unit
        )

        // When
        viewModel = createViewModel(
            currentUserId = currentUserId,
            journalOwnerId = journalOwnerId,
            commentAccess = JournalAccess.NOBODY
        )
        advanceUntilIdle()

        // Then
        val canCreate = viewModel.canCreateEntry.value
        assertFalse(
            "Не владелец дневника не может создавать записи при NOBODY",
            canCreate
        )
    }

    // ==================== ТЕСТЫ ДЛЯ ИСПРАВЛЕНИЯ БАГОВ ====================

    /**
     * Тест 31: observeEntries сохраняет journal при обновлении записей
     *
     * Проверяет баг #1: Кнопка настроек исчезает после загрузки.
     * При обновлении списка записей journal должен сохраняться в состоянии.
     */
    @Test
    fun testObserveEntries_preservesJournal() = runTest {
        // Given
        val testJournal = com.swparks.domain.model.Journal(
            id = testJournalId,
            title = "Тестовый дневник",
            lastMessageImage = null,
            createDate = "2024-01-01T10:00:00",
            modifyDate = "2024-01-15T15:30:00",
            lastMessageDate = "2024-01-15T15:30:00",
            lastMessageText = "Последнее сообщение",
            entriesCount = 5,
            ownerId = testUserId,
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )
        val entries = listOf(testEntry)

        // Настраиваем моки
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns flowOf(entries)
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)
        every { swRepository.observeJournalById(testJournalId) } returns flowOf(testJournal)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - journal должен быть сохранён в состоянии Content
        val state = viewModel.uiState.value
        assertTrue(
            "Состояние должно быть Content",
            state is JournalEntriesUiState.Content
        )
        val contentState = state as JournalEntriesUiState.Content
        assertEquals(
            "Journal должен быть сохранён в состоянии",
            testJournal.id,
            contentState.journal?.id
        )
        assertEquals(
            "Заголовок journal должен быть корректным",
            testJournal.title,
            contentState.journal?.title
        )
    }

    /**
     * Тест 32: loadJournal пропускает загрузку, если дневник уже в кэше
     *
     * Проверяет баг #3: Лишняя загрузка дневника с сервера.
     * Если дневник уже есть в кэше, запрос к серверу не должен выполняться.
     */
    @Test
    fun testLoadJournal_skipsWhenCached() = runTest {
        // Given
        val testJournal = com.swparks.domain.model.Journal(
            id = testJournalId,
            title = "Тестовый дневник",
            lastMessageImage = null,
            createDate = "2024-01-01T10:00:00",
            modifyDate = "2024-01-15T15:30:00",
            lastMessageDate = "2024-01-15T15:30:00",
            lastMessageText = "Последнее сообщение",
            entriesCount = 5,
            ownerId = testUserId,
            viewAccess = JournalAccess.ALL,
            commentAccess = JournalAccess.ALL
        )

        // Настраиваем моки - дневник уже в кэше
        every { swRepository.observeJournalById(testJournalId) } returns flowOf(testJournal)
        coEvery { getJournalEntriesUseCase(testUserId, testJournalId) } returns emptyFlow()
        coEvery {
            syncJournalEntriesUseCase(
                testUserId,
                testJournalId
            )
        } returns Result.success(Unit)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - getJournal НЕ должен быть вызван, т.к. дневник уже в кэше
        coVerify(exactly = 0) { swRepository.getJournal(any(), any()) }
    }
}
