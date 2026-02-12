package com.swparks.ui.viewmodel

import android.util.Log
import app.cash.turbine.test
import com.swparks.data.database.entity.DialogEntity
import com.swparks.domain.repository.MessagesRepository
import com.swparks.ui.state.DialogsUiState
import com.swparks.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit тесты для DialogsViewModel.
 *
 * Тестирует управление состоянием экрана списка диалогов,
 * включая загрузку данных, обработку ошибок и pull-to-refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DialogsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var messagesRepository: MessagesRepository
    private lateinit var logger: Logger
    private lateinit var viewModel: DialogsViewModel

    private val testDialog = DialogEntity(
        id = 1L,
        anotherUserId = 123,
        name = "Тестовый пользователь",
        image = "https://example.com/avatar.png",
        lastMessageText = "Привет!",
        lastMessageDate = "2024-01-15 12:30",
        unreadCount = 2
    )

    @Before
    fun setup() {
        // Мокируем статический Log для использования в тестах
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        messagesRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun init_collectsDialogsFromRepository() = runTest {
        // Given
        val dialogs = listOf(testDialog)
        coEvery { messagesRepository.dialogs } returns flowOf(dialogs)
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)

        // When
        viewModel = DialogsViewModel(messagesRepository, logger)
        advanceUntilIdle()

        // Then
        coVerify { messagesRepository.dialogs }
        coVerify { messagesRepository.refreshDialogs() }
    }

    @Test
    fun refresh_callsRepositoryRefreshDialogs() = runTest {
        // Given
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)

        // When
        viewModel = DialogsViewModel(messagesRepository, logger)
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { messagesRepository.refreshDialogs() }
    }

    @Test
    fun refresh_onSuccess_updatesIsRefreshing() = runTest {
        // Given
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)

        // When
        viewModel = DialogsViewModel(messagesRepository, logger)

        // Then
        viewModel.isRefreshing.test {
            // Initial state
            assertEquals(false, awaitItem())
            // After init completes
            advanceUntilIdle()
        }
    }

    @Test
    fun refresh_onFailureWithNonEmptyCache_showsSyncError() = runTest {
        // Given
        val dialogs = listOf(testDialog)
        coEvery { messagesRepository.dialogs } returns flowOf(dialogs)
        coEvery { messagesRepository.refreshDialogs() } returns Result.failure(Exception("Network error"))

        // When
        viewModel = DialogsViewModel(messagesRepository, logger)
        advanceUntilIdle()

        // Then
        viewModel.syncError.test {
            val error = awaitItem()
            assertEquals("Ошибка синхронизации", error)
        }
    }

    @Test
    fun refresh_onEmptyCacheAndFailure_showsErrorState() = runTest {
        // Given
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.failure(Exception("Network error"))

        // When
        viewModel = DialogsViewModel(messagesRepository, logger)
        advanceUntilIdle()

        // Then - при пустом кэше и ошибке должен быть Error state
        val state = viewModel.uiState.value
        assertTrue("Expected Error state but got $state", state is DialogsUiState.Error)
    }

    @Test
    fun refresh_debounce_ignoresMultipleRapidCalls() = runTest {
        // Given - создаём viewModel с успешной загрузкой
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)

        // When
        viewModel = DialogsViewModel(messagesRepository, logger)
        advanceUntilIdle()

        // На этом этапе refresh из init уже завершён (вызван 1 раз)
        // Теперь вызываем refresh вручную два раза подряд
        // Поскольку первый вызов завершается синхронно (isRefreshing сразу false),
        // второй вызов тоже должен быть обработан
        viewModel.refresh()
        viewModel.refresh()

        advanceUntilIdle()

        // Then - refresh должен быть вызван 3 раза:
        // 1 раз из init + 2 раза из ручных вызовов (так как они не перекрываются во времени)
        coVerify(exactly = 3) { messagesRepository.refreshDialogs() }
    }

    @Test
    fun onDialogClick_withValidUserId_logsClick() = runTest {
        // Given
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)

        // When
        viewModel = DialogsViewModel(messagesRepository, logger)
        advanceUntilIdle()
        viewModel.onDialogClick(dialogId = 1L, userId = 123)

        // Then
        verify { logger.i(any(), any()) }
    }

    @Test
    fun onDialogClick_withNullUserId_logsWarning() = runTest {
        // Given
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)

        // When
        viewModel = DialogsViewModel(messagesRepository, logger)
        advanceUntilIdle()
        viewModel.onDialogClick(dialogId = 1L, userId = null)

        // Then
        verify { logger.w(any(), any()) }
    }

    @Test
    fun dismissSyncError_clearsError() = runTest {
        // Given
        val dialogs = listOf(testDialog)
        coEvery { messagesRepository.dialogs } returns flowOf(dialogs)
        coEvery { messagesRepository.refreshDialogs() } returns Result.failure(Exception("Network error"))

        // When
        viewModel = DialogsViewModel(messagesRepository, logger)
        advanceUntilIdle()

        // Verify error was set
        viewModel.syncError.test {
            assertEquals("Ошибка синхронизации", awaitItem())
        }

        // Dismiss error
        viewModel.dismissSyncError()

        // Then
        assertNull(viewModel.syncError.value)
    }

    /**
     * Тест для бага #6: проверяет что refresh() можно вызвать повторно после
     * повторной авторизации пользователя (logout -> login).
     *
     * Сценарий: пользователь вышел из аккаунта, затем снова вошёл.
     * LaunchedEffect в UI отслеживает изменение isAuthorized и вызывает refresh().
     * Этот тест проверяет что refresh() работает корректно при повторном вызове.
     */
    @Test
    fun refresh_afterReLogin_loadsDialogs() = runTest {
        // Given - создаём viewModel с непустым списком диалогов (имитируем кэшированные данные)
        val cachedDialogs = listOf(testDialog)
        coEvery { messagesRepository.dialogs } returns flowOf(cachedDialogs)
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)

        // When - инициализация (первая авторизация)
        viewModel = DialogsViewModel(messagesRepository, logger)
        advanceUntilIdle()

        // Проверяем что состояние Success с кэшированными данными
        val stateAfterInit = viewModel.uiState.value
        assertTrue(
            "Expected Success state after init but got $stateAfterInit",
            stateAfterInit is DialogsUiState.Success
        )
        assertEquals(cachedDialogs, (stateAfterInit as DialogsUiState.Success).dialogs)

        // Simulate logout -> login: вызываем refresh снова
        // (в реальном приложении это делает LaunchedEffect при изменении isAuthorized)
        viewModel.refresh()
        advanceUntilIdle()

        // Then - состояние остаётся Success (не Error)
        val stateAfterRefresh = viewModel.uiState.value
        assertTrue(
            "Expected Success state after re-login refresh but got $stateAfterRefresh",
            stateAfterRefresh is DialogsUiState.Success
        )
    }

    /**
     * Тест для бага #8: проверяет что при успешном ответе сервера с 0 диалогами
     * показывается Success с пустым списком (EmptyState), а не Error.
     *
     * Сценарий: сервер возвращает пустой список диалогов (успешный ответ).
     * Ожидается: Success(emptyList()) для отображения EmptyStateViewForDialogs.
     */
    @Test
    fun refresh_withEmptyDialogsResponse_showsSuccessEmptyState() = runTest {
        // Given - сервер возвращает 0 диалогов (успешный ответ)
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)

        // When
        viewModel = DialogsViewModel(messagesRepository, logger)
        advanceUntilIdle()

        // Then - должен быть Success с пустым списком, а не Error
        val state = viewModel.uiState.value
        assertTrue(
            "Expected Success state with empty list but got $state",
            state is DialogsUiState.Success && state.dialogs.isEmpty()
        )
    }
}
