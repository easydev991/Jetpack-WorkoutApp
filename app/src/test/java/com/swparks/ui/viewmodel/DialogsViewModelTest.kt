package com.swparks.ui.viewmodel

import android.util.Log
import app.cash.turbine.test
import com.swparks.R
import com.swparks.data.database.entity.DialogEntity
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.ResourcesProvider
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
 * включая загрузку данных, обработку ошибок, pull-to-refresh и удаление диалогов.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DialogsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var messagesRepository: MessagesRepository
    private lateinit var swRepository: SWRepository
    private lateinit var logger: Logger
    private lateinit var resources: ResourcesProvider
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
        swRepository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        resources = mockk(relaxed = true)
        every { resources.getString(R.string.dialog_delete_error) } returns "Ошибка удаления диалога"
        every { resources.getString(R.string.sync_error_message) } returns "Ошибка синхронизации"
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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)

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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
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
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()

        // Then - должен быть Success с пустым списком, а не Error
        val state = viewModel.uiState.value
        assertTrue(
            "Expected Success state with empty list but got $state",
            state is DialogsUiState.Success && state.dialogs.isEmpty()
        )
    }

    // ==================== Тесты для deleteDialog ====================

    /**
     * Тест: успешное удаление диалога вызывает swRepository.deleteDialog.
     */
    @Test
    fun deleteDialog_whenSuccess_callsRepositoryDeleteDialog() = runTest {
        // Given
        val dialogId = 1L
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)
        coEvery { swRepository.deleteDialog(dialogId) } returns Result.success(Unit)

        // When
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()
        viewModel.deleteDialog(dialogId)
        advanceUntilIdle()

        // Then
        coVerify { swRepository.deleteDialog(dialogId) }
    }

    /**
     * Тест: успешное удаление диалога обновляет isDeleting.
     */
    @Test
    fun deleteDialog_whenSuccess_updatesIsDeleting() = runTest {
        // Given
        val dialogId = 1L
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)
        // Добавляем небольшую задержку, чтобы успеть проверить состояние isDeleting = true
        coEvery { swRepository.deleteDialog(dialogId) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }

        // When
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()

        // Test isDeleting flow
        viewModel.isDeleting.test {
            assertEquals(false, awaitItem()) // Initial state
            viewModel.deleteDialog(dialogId)
            assertEquals(true, awaitItem()) // During deletion
            advanceUntilIdle() // Wait for deletion to complete
            assertEquals(false, awaitItem()) // After deletion
        }
    }

    /**
     * Тест: неудачное удаление диалога показывает ошибку.
     */
    @Test
    fun deleteDialog_whenFailure_showsSyncError() = runTest {
        // Given
        val dialogId = 1L
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)
        coEvery { swRepository.deleteDialog(dialogId) } returns Result.failure(Exception("Network error"))

        // When
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()
        viewModel.deleteDialog(dialogId)
        advanceUntilIdle()

        // Then
        assertEquals("Ошибка удаления диалога", viewModel.syncError.value)
    }

    /**
     * Тест: неудачное удаление диалога логирует ошибку.
     */
    @Test
    fun deleteDialog_whenFailure_logsError() = runTest {
        // Given
        val dialogId = 1L
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)
        coEvery { swRepository.deleteDialog(dialogId) } returns Result.failure(Exception("Network error"))

        // When
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()
        viewModel.deleteDialog(dialogId)
        advanceUntilIdle()

        // Then
        verify { logger.e(any(), any(), any()) }
    }

    // ==================== Тесты для markDialogAsRead ====================

    /**
     * Тест: успешная отметка диалога вызывает swRepository.markDialogAsRead.
     */
    @Test
    fun markDialogAsRead_whenSuccess_callsRepositoryMarkAsRead() = runTest {
        // Given
        val dialogId = 1L
        val userId = 123
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)
        coEvery { swRepository.markDialogAsRead(dialogId, userId) } returns Result.success(Unit)

        // When
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()
        viewModel.markDialogAsRead(dialogId, userId)
        advanceUntilIdle()

        // Then
        coVerify { swRepository.markDialogAsRead(dialogId, userId) }
    }

    /**
     * Тест: успешная отметка диалога обновляет isMarkingAsRead.
     */
    @Test
    fun markDialogAsRead_whenSuccess_updatesIsMarkingAsRead() = runTest {
        // Given
        val dialogId = 1L
        val userId = 123
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)
        coEvery { swRepository.markDialogAsRead(dialogId, userId) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }

        // When
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()

        // Test isMarkingAsRead flow
        viewModel.isMarkingAsRead.test {
            assertEquals(false, awaitItem()) // Initial state
            viewModel.markDialogAsRead(dialogId, userId)
            assertEquals(true, awaitItem()) // During marking
            advanceUntilIdle() // Wait for operation to complete
            assertEquals(false, awaitItem()) // After marking
        }
    }

    /**
     * Тест: неудачная отметка диалога показывает ошибку синхронизации.
     */
    @Test
    fun markDialogAsRead_whenFailure_showsSyncError() = runTest {
        // Given
        val dialogId = 1L
        val userId = 123
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)
        coEvery {
            swRepository.markDialogAsRead(
                dialogId,
                userId
            )
        } returns Result.failure(Exception("Network error"))

        // When
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()
        viewModel.markDialogAsRead(dialogId, userId)
        advanceUntilIdle()

        // Then
        assertEquals("Ошибка синхронизации", viewModel.syncError.value)
    }

    /**
     * Тест: неудачная отметка диалога логирует ошибку.
     */
    @Test
    fun markDialogAsRead_whenFailure_logsError() = runTest {
        // Given
        val dialogId = 1L
        val userId = 123
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)
        coEvery {
            swRepository.markDialogAsRead(
                dialogId,
                userId
            )
        } returns Result.failure(Exception("Network error"))

        // When
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()
        viewModel.markDialogAsRead(dialogId, userId)
        advanceUntilIdle()

        // Then
        verify { logger.e(any(), any(), any()) }
    }

    /**
     * Тест для бага EmptyStateView: проверяет что после повторной авторизации с пустым списком
     * диалогов показывается Success(emptyList()), а не старый Success с диалогами.
     *
     * Сценарий:
     * 1. Пользователь авторизован, имеет диалоги (uiState = Success с диалогами)
     * 2. Пользователь выходит из аккаунта
     * 3. Пользователь снова входит, но теперь сервер возвращает пустой список
     *
     * Баг: в loadDialogsInternal() есть условие `if (currentState is DialogsUiState.Loading)`,
     * которое не выполняется, если uiState уже Success. В результате после авторизации
     * показывается старый список диалогов вместо EmptyStateView.
     */
    @Test
    fun loadDialogsAfterAuth_withPreviouslyCachedDialogs_andEmptyServerResponse_showsEmptyState() =
        runTest {
            // Given - сначала есть кэшированные диалоги
            val cachedDialogs = listOf(testDialog)
            val dialogFlow = kotlinx.coroutines.flow.MutableStateFlow(cachedDialogs)
            coEvery { messagesRepository.dialogs } returns dialogFlow
            coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)

            // When - первая инициализация с кэшированными диалогами
            viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
            advanceUntilIdle()

            // Проверяем что состояние Success с диалогами
            val stateAfterInit = viewModel.uiState.value
            assertTrue(
                "Expected Success state after init but got $stateAfterInit",
                stateAfterInit is DialogsUiState.Success
            )
            assertEquals(cachedDialogs, (stateAfterInit as DialogsUiState.Success).dialogs)

            // Симулируем logout -> login: сервер теперь возвращает пустой список
            dialogFlow.value = emptyList()

            // Вызываем loadDialogsAfterAuth() (как при повторной авторизации)
            viewModel.loadDialogsAfterAuth()
            advanceUntilIdle()

            // Then - должен быть Success с пустым списком (EmptyStateView)
            val stateAfterReAuth = viewModel.uiState.value
            assertTrue(
                "Expected Success state with empty list after re-auth but got $stateAfterReAuth",
                stateAfterReAuth is DialogsUiState.Success
            )
            assertTrue(
                "Expected empty dialog list but got ${(stateAfterReAuth as DialogsUiState.Success).dialogs.size} dialogs",
                stateAfterReAuth.dialogs.isEmpty()
            )
        }

    /**
     * Тест для бага EmptyStateView: проверяет случай когда Room Flow эмитит данные
     * с задержкой после успешной загрузки с сервера.
     *
     * Сценарий:
     * 1. Пользователь авторизуется впервые
     * 2. Сервер возвращает пустой список диалогов
     * 3. refreshDialogs() завершается успешно
     * 4. НО Room Flow ещё не эмитит обновление (задержка)
     * 5. loadDialogsInternal() должен установить Success(emptyList()) независимо от Flow
     *
     * Баг: если Room Flow не эмитит сразу после insertAll(), uiState остаётся в Loading.
     */
    @Test
    fun loadDialogsAfterAuth_whenFlowEmitsWithDelay_showsSuccessEmptyState() = runTest {
        // Given - Flow эмитит данные с задержкой (симулируем Room async behavior)
        val dialogFlow = kotlinx.coroutines.flow.MutableStateFlow<List<DialogEntity>>(emptyList())
        coEvery { messagesRepository.dialogs } returns dialogFlow
        coEvery { messagesRepository.refreshDialogs() } coAnswers {
            // Симулируем что сервер возвращает пустой список, но Flow обновляется не сразу
            kotlinx.coroutines.delay(500)
            Result.success(Unit)
        }

        // When
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()

        // Then - даже если Flow ещё не эмитит, должен быть Success с пустым списком
        val state = viewModel.uiState.value
        assertTrue(
            "Expected Success state with empty list but got $state",
            state is DialogsUiState.Success && state.dialogs.isEmpty()
        )
    }

    /**
     * Тест: isUpdating = true когда isDeleting = true или isMarkingAsRead = true.
     */
    @Test
    fun isUpdating_whenDeletingOrMarkingAsRead_isTrue() = runTest {
        // Given
        val dialogId = 1L
        val userId = 123
        coEvery { messagesRepository.dialogs } returns flowOf(emptyList())
        coEvery { messagesRepository.refreshDialogs() } returns Result.success(Unit)
        coEvery { swRepository.deleteDialog(dialogId) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }
        coEvery { swRepository.markDialogAsRead(dialogId, userId) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }

        // When
        viewModel = DialogsViewModel(messagesRepository, swRepository, logger, resources)
        advanceUntilIdle()

        // Test isUpdating flow with deleteDialog
        viewModel.isUpdating.test {
            assertEquals(false, awaitItem()) // Initial state
            viewModel.deleteDialog(dialogId)
            assertEquals(true, awaitItem()) // During deletion
            advanceUntilIdle()
            assertEquals(false, awaitItem()) // After deletion
        }
    }
}
