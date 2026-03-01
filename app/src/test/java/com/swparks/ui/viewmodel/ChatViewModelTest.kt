package com.swparks.ui.viewmodel

import android.util.Log
import app.cash.turbine.test
import com.swparks.data.model.MessageResponse
import com.swparks.network.SWApi
import com.swparks.ui.state.ChatUiState
import com.swparks.util.AppError
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit тесты для ChatViewModel.
 *
 * Тестирует управление состоянием экрана чата,
 * включая загрузку сообщений, отправку и обработку ошибок.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var swApi: SWApi
    private lateinit var userNotifier: UserNotifier
    private lateinit var viewModel: ChatViewModel

    private val testMessage = MessageResponse(
        id = 1L,
        userId = 123,
        message = "Привет!",
        name = "Тестовый пользователь",
        created = "2024-01-15 12:30"
    )

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        swApi = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Initial State ====================

    @Test
    fun initialState_isLoading() = runTest {
        // Given - нет предварительных условий

        // When
        viewModel = ChatViewModel(swApi, userNotifier)

        // Then
        assertTrue(viewModel.uiState.value is ChatUiState.Loading)
    }

    // ==================== loadMessages ====================

    @Test
    fun loadMessages_updatesUiState_toSuccess() = runTest {
        // Given
        val dialogId = 1L
        val messages = listOf(testMessage)
        coEvery { swApi.getMessages(dialogId) } returns messages

        // When
        viewModel = ChatViewModel(swApi, userNotifier)
        viewModel.loadMessages(dialogId)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Expected Success state but got $state", state is ChatUiState.Success)
        assertEquals(messages, (state as ChatUiState.Success).messages)
    }

    @Test
    fun loadMessages_handlesError_andCallsUserNotifier() = runTest {
        // Given
        val dialogId = 1L
        val exception = Exception("Network error")
        coEvery { swApi.getMessages(dialogId) } throws exception

        // When
        viewModel = ChatViewModel(swApi, userNotifier)
        viewModel.loadMessages(dialogId)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Expected Error state but got $state", state is ChatUiState.Error)
        verify { userNotifier.handleError(any<AppError>()) }
    }

    // ==================== refreshMessages ====================

    @Test
    fun refreshMessages_updatesMessages() = runTest {
        // Given
        val dialogId = 1L
        val messages = listOf(testMessage)
        coEvery { swApi.getMessages(dialogId) } returns messages

        // When
        viewModel = ChatViewModel(swApi, userNotifier)
        viewModel.loadMessages(dialogId)
        advanceUntilIdle()
        viewModel.refreshMessages()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { swApi.getMessages(dialogId) }
    }

    @Test
    fun refreshMessages_handlesError_andCallsUserNotifier() = runTest {
        // Given
        val dialogId = 1L
        coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage) andThenThrows Exception("Network error")

        // When
        viewModel = ChatViewModel(swApi, userNotifier)
        viewModel.loadMessages(dialogId)
        advanceUntilIdle()
        viewModel.refreshMessages()
        advanceUntilIdle()

        // Then
        verify { userNotifier.handleError(any<AppError>()) }
    }

    // ==================== sendMessage ====================

    @Test
    fun sendMessage_withEmptyText_doesNotSend() = runTest {
        // Given
        val userId = 123
        coEvery { swApi.getMessages(any()) } returns emptyList()

        // When
        viewModel = ChatViewModel(swApi, userNotifier)
        viewModel.messageText.value = ""
        viewModel.sendMessage(userId)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { swApi.sendMessageTo(any(), any()) }
    }

    @Test
    fun sendMessage_withText_clearsMessageTextAndRefreshes() = runTest {
        // Given
        val dialogId = 1L
        val userId = 123
        val messages = listOf(testMessage)
        coEvery { swApi.getMessages(dialogId) } returns messages
        coEvery { swApi.sendMessageTo(userId.toLong(), "Test message") } returns mockk(relaxed = true)

        // When
        viewModel = ChatViewModel(swApi, userNotifier)
        viewModel.loadMessages(dialogId)
        advanceUntilIdle()
        viewModel.messageText.value = "Test message"
        viewModel.sendMessage(userId)
        advanceUntilIdle()

        // Then
        assertEquals("", viewModel.messageText.value)
        coVerify { swApi.sendMessageTo(userId.toLong(), "Test message") }
        coVerify(exactly = 2) { swApi.getMessages(dialogId) } // loadMessages + refreshMessages
    }

    @Test
    fun sendMessage_handlesError_andCallsUserNotifier() = runTest {
        // Given
        val dialogId = 1L
        val userId = 123
        coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
        coEvery { swApi.sendMessageTo(userId.toLong(), any()) } throws Exception("Network error")

        // When
        viewModel = ChatViewModel(swApi, userNotifier)
        viewModel.loadMessages(dialogId)
        advanceUntilIdle()
        viewModel.messageText.value = "Test message"
        viewModel.sendMessage(userId)
        advanceUntilIdle()

        // Then
        verify { userNotifier.handleError(any<AppError>()) }
    }

    @Test
    fun sendMessage_setsIsLoading() = runTest {
        // Given
        val dialogId = 1L
        val userId = 123
        coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
        coEvery { swApi.sendMessageTo(userId.toLong(), any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            mockk(relaxed = true)
        }

        // When
        viewModel = ChatViewModel(swApi, userNotifier)
        viewModel.loadMessages(dialogId)
        advanceUntilIdle()

        // Test isLoading flow
        viewModel.isLoading.test {
            assertEquals(false, awaitItem()) // Initial state
            viewModel.messageText.value = "Test message"
            viewModel.sendMessage(userId)
            assertEquals(true, awaitItem()) // During sending
            advanceUntilIdle()
            assertEquals(false, awaitItem()) // After sending
        }
    }

    // ==================== markAsRead ====================

    @Test
    fun markAsRead_callsApi() = runTest {
        // Given
        val dialogId = 1L
        val userId = 123
        coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
        coEvery { swApi.markAsRead(userId.toLong()) } returns mockk(relaxed = true)

        // When
        viewModel = ChatViewModel(swApi, userNotifier)
        viewModel.loadMessages(dialogId)
        advanceUntilIdle()
        viewModel.markAsRead(userId)
        advanceUntilIdle()

        // Then
        coVerify { swApi.markAsRead(userId.toLong()) }
    }

    @Test
    fun markAsRead_onError_logsButDoesNotNotifyUser() = runTest {
        // Given
        val dialogId = 1L
        val userId = 123
        coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
        coEvery { swApi.markAsRead(userId.toLong()) } throws Exception("Network error")

        // When
        viewModel = ChatViewModel(swApi, userNotifier)
        viewModel.loadMessages(dialogId)
        advanceUntilIdle()
        viewModel.markAsRead(userId)
        advanceUntilIdle()

        // Then - markAsRead errors should NOT call userNotifier.handleError
        verify(exactly = 0) { userNotifier.handleError(any<AppError>()) }
    }
}
