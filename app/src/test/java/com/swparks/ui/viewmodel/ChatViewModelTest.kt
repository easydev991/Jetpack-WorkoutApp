package com.swparks.ui.viewmodel

import app.cash.turbine.test
import com.swparks.analytics.AnalyticsEvent
import com.swparks.analytics.AnalyticsService
import com.swparks.analytics.AppErrorOperation
import com.swparks.analytics.UserActionType
import com.swparks.data.model.MessageResponse
import com.swparks.data.repository.SWRepository
import com.swparks.network.SWApi
import com.swparks.ui.state.ChatEvent
import com.swparks.ui.state.ChatUiState
import com.swparks.util.AppError
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

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
    private lateinit var swRepository: SWRepository
    private lateinit var userNotifier: UserNotifier
    private lateinit var logger: Logger
    private lateinit var crashReporter: CrashReporter
    private lateinit var viewModel: ChatViewModel
    private lateinit var analyticsService: AnalyticsService

    private val testMessage =
        MessageResponse(
            id = 1L,
            userId = 123,
            message = "Привет!",
            name = "Тестовый пользователь",
            created = "2024-01-15 12:30"
        )

    @Before
    fun setup() {
        swApi = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        userNotifier = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        crashReporter = mockk(relaxed = true)
        analyticsService = mockk(relaxed = true)
    }

    // ==================== Initial State ====================

    @Test
    fun initialState_isLoading() =
        runTest {
            // Given - нет предварительных условий

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)

            // Then
            assertTrue(viewModel.uiState.value is ChatUiState.Loading)
        }

    // ==================== loadMessages ====================

    @Test
    fun loadMessages_updatesUiState_toSuccess() =
        runTest {
            // Given
            val dialogId = 1L
            val messages = listOf(testMessage)
            coEvery { swApi.getMessages(dialogId) } returns messages

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue("Expected Success state but got $state", state is ChatUiState.Success)
            assertEquals(messages, (state as ChatUiState.Success).messages)
        }

    @Test
    fun loadMessages_sortsMessagesByCreatedDate_oldestToNewest() =
        runTest {
            // Given - сообщения приходят в обратном порядке (новые первыми)
            val dialogId = 1L
            val message1 =
                MessageResponse(
                    id = 1L,
                    userId = 123,
                    message = "Первое сообщение",
                    name = "Пользователь",
                    created = "2024-01-15 10:00"
                )
            val message2 =
                MessageResponse(
                    id = 2L,
                    userId = 123,
                    message = "Второе сообщение",
                    name = "Пользователь",
                    created = "2024-01-15 11:00"
                )
            val message3 =
                MessageResponse(
                    id = 3L,
                    userId = 123,
                    message = "Третье сообщение",
                    name = "Пользователь",
                    created = "2024-01-15 12:00"
                )
            // API возвращает в обратном порядке (новые первыми)
            coEvery { swApi.getMessages(dialogId) } returns listOf(message3, message1, message2)

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()

            // Then - должны быть отсортированы по created от старых к новым
            val state = viewModel.uiState.value
            assertTrue("Expected Success state but got $state", state is ChatUiState.Success)
            val sortedMessages = (state as ChatUiState.Success).messages
            assertEquals(3, sortedMessages.size)
            assertEquals("Первое сообщение", sortedMessages[0].message)
            assertEquals("Второе сообщение", sortedMessages[1].message)
            assertEquals("Третье сообщение", sortedMessages[2].message)
        }

    @Test
    fun loadMessages_handlesError_andCallsUserNotifier() =
        runTest {
            // Given
            val dialogId = 1L
            val exception = IOException("Network error")
            coEvery { swApi.getMessages(dialogId) } throws exception

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue("Expected Error state but got $state", state is ChatUiState.Error)
            coVerify { userNotifier.handleError(any<AppError>()) }
            verify {
                logger.e(
                    any(),
                    match { it.contains("Ошибка загрузки сообщений") },
                    exception
                )
            }
            verify { crashReporter.logException(exception, "Ошибка загрузки сообщений") }
        }

    // ==================== refreshMessages ====================

    @Test
    fun refreshMessages_updatesMessages() =
        runTest {
            // Given
            val dialogId = 1L
            val messages = listOf(testMessage)
            coEvery { swApi.getMessages(dialogId) } returns messages

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.refreshMessages()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 2) { swApi.getMessages(dialogId) }
        }

    @Test
    fun refreshMessages_handlesError_andCallsUserNotifier() =
        runTest {
            // Given
            val dialogId = 1L
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage) andThenThrows
                IOException(
                    "Network error"
                )

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.refreshMessages()
            advanceUntilIdle()

            // Then
            coVerify { userNotifier.handleError(any<AppError>()) }
            verify {
                logger.e(
                    any(),
                    match { it.contains("Ошибка обновления сообщений") },
                    any<IOException>()
                )
            }
            verify { crashReporter.logException(any<IOException>(), "Ошибка обновления сообщений") }
        }

    @Test
    fun refreshMessages_setsIsLoading() =
        runTest {
            // Given
            val dialogId = 1L
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
            coEvery { swApi.getMessages(dialogId) } coAnswers {
                kotlinx.coroutines.delay(100)
                listOf(testMessage)
            }

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()

            // Test isLoading flow
            viewModel.isLoading.test {
                assertEquals(false, awaitItem()) // Initial state after loadMessages
                viewModel.refreshMessages()
                assertEquals(true, awaitItem()) // During refresh
                advanceUntilIdle()
                assertEquals(false, awaitItem()) // After refresh
            }
        }

    // ==================== sendMessage ====================

    @Test
    fun sendMessage_withEmptyText_doesNotSend() =
        runTest {
            // Given
            val userId = 123
            coEvery { swApi.getMessages(any()) } returns emptyList()

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.onMessageTextChange("")
            viewModel.sendMessage(userId)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { swApi.sendMessageTo(any(), any()) }
        }

    @Test
    fun sendMessage_withText_clearsMessageTextAndRefreshes() =
        runTest {
            // Given
            val dialogId = 1L
            val userId = 123
            val messages = listOf(testMessage)
            coEvery { swApi.getMessages(dialogId) } returns messages
            coEvery {
                swApi.sendMessageTo(
                    userId.toLong(),
                    "Test message"
                )
            } returns mockk(relaxed = true)

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.onMessageTextChange("Test message")
            viewModel.sendMessage(userId)
            advanceUntilIdle()

            // Then
            assertEquals("", viewModel.messageText.value)
            coVerify { swApi.sendMessageTo(userId.toLong(), "Test message") }
            coVerify(exactly = 2) { swApi.getMessages(dialogId) } // loadMessages + refreshMessages
        }

    @Test
    fun sendMessage_handlesError_andCallsUserNotifier() =
        runTest {
            // Given
            val dialogId = 1L
            val userId = 123
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
            coEvery {
                swApi.sendMessageTo(
                    userId.toLong(),
                    any()
                )
            } throws IOException("Network error")

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.onMessageTextChange("Test message")
            viewModel.sendMessage(userId)
            advanceUntilIdle()

            // Then
            coVerify { userNotifier.handleError(any<AppError>()) }
            verify {
                logger.e(
                    any(),
                    match { it.contains("Ошибка отправки сообщения") },
                    any<IOException>()
                )
            }
            verify { crashReporter.logException(any<IOException>(), "Ошибка отправки сообщения") }
        }

    @Test
    fun sendMessage_setsIsLoading() =
        runTest {
            // Given
            val dialogId = 1L
            val userId = 123
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
            coEvery { swApi.sendMessageTo(userId.toLong(), any()) } coAnswers {
                kotlinx.coroutines.delay(100)
                mockk(relaxed = true)
            }

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()

            // Test isLoading flow
            viewModel.isLoading.test {
                assertEquals(false, awaitItem()) // Initial state
                viewModel.onMessageTextChange("Test message")
                viewModel.sendMessage(userId)
                assertEquals(true, awaitItem()) // During sending
                advanceUntilIdle()
                assertEquals(false, awaitItem()) // After sending
            }
        }

    // ==================== markAsRead ====================

    @Test
    fun markAsRead_callsRepository() =
        runTest {
            // Given
            val dialogId = 1L
            val userId = 123
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
            coEvery { swRepository.markDialogAsRead(dialogId, userId) } returns Result.success(Unit)

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.markAsRead(userId)
            advanceUntilIdle()

            // Then
            coVerify { swRepository.markDialogAsRead(dialogId, userId) }
        }

    @Test
    fun markAsRead_withoutDialogId_returnsEarly() =
        runTest {
            // Given
            val userId = 123
            // No dialogId loaded

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            // Don't call loadMessages, so currentDialogId is null
            viewModel.markAsRead(userId)
            advanceUntilIdle()

            // Then - should not call repository
            coVerify(exactly = 0) { swRepository.markDialogAsRead(any(), any()) }
        }

    @Test
    fun markAsRead_onError_logsButDoesNotNotifyUser() =
        runTest {
            // Given
            val dialogId = 1L
            val userId = 123
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
            coEvery {
                swRepository.markDialogAsRead(dialogId, userId)
            } returns Result.failure(Exception("Network error"))

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.markAsRead(userId)
            advanceUntilIdle()

            // Then - markAsRead errors should NOT call userNotifier.handleError
            verify(exactly = 0) { userNotifier.handleError(any<AppError>()) }
            verify { logger.e(any(), match { it.contains("Ошибка markAsRead") }, any()) }
            verify { crashReporter.logException(any(), "Ошибка markAsRead") }
        }

    // ==================== events flow ====================

    @Test
    fun sendMessage_emitsMessageSentEvent_onSuccess() =
        runTest {
            // Given
            val dialogId = 1L
            val userId = 123
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
            coEvery { swApi.sendMessageTo(userId.toLong(), any()) } returns mockk(relaxed = true)

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()

            // Test events flow
            viewModel.events.test {
                viewModel.onMessageTextChange("Test message")
                viewModel.sendMessage(userId)
                advanceUntilIdle()

                // Then
                val event = awaitItem()
                assertTrue("Expected MessageSent event", event is ChatEvent.MessageSent)
                assertEquals(dialogId, (event as ChatEvent.MessageSent).dialogId)
            }
        }

    // ==================== HttpException Tests ====================

    @Test
    fun loadMessages_handlesHttpException_andCallsUserNotifier() =
        runTest {
            // Given
            val dialogId = 1L
            val mockResponse = mockk<Response<*>>(relaxed = true)
            every { mockResponse.code() } returns 500
            every { mockResponse.message() } returns "Server Error"
            val httpException = HttpException(mockResponse)
            coEvery { swApi.getMessages(dialogId) } throws httpException

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue("Expected Error state but got $state", state is ChatUiState.Error)
            coVerify { userNotifier.handleError(any<AppError>()) }
        }

    @Test
    fun refreshMessages_handlesHttpException_andCallsUserNotifier() =
        runTest {
            // Given
            val dialogId = 1L
            val mockResponse = mockk<Response<*>>(relaxed = true)
            every { mockResponse.code() } returns 500
            every { mockResponse.message() } returns "Server Error"
            val httpException = HttpException(mockResponse)
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage) andThenThrows httpException

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.refreshMessages()
            advanceUntilIdle()

            // Then
            coVerify { userNotifier.handleError(any<AppError>()) }
        }

    @Test
    fun sendMessage_handlesHttpException_andCallsUserNotifier() =
        runTest {
            // Given
            val dialogId = 1L
            val userId = 123
            val mockResponse = mockk<Response<*>>(relaxed = true)
            every { mockResponse.code() } returns 500
            every { mockResponse.message() } returns "Server Error"
            val httpException = HttpException(mockResponse)
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
            coEvery { swApi.sendMessageTo(userId.toLong(), any()) } throws httpException

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.onMessageTextChange("Test message")
            viewModel.sendMessage(userId)
            advanceUntilIdle()

            // Then
            coVerify { userNotifier.handleError(any<AppError>()) }
        }

    // ==================== messageText StateFlow ====================

    @Test
    fun onMessageTextChange_whenCalled_thenUpdatesMessageTextFlow() =
        runTest {
            // Given
            val dialogId = 1L
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()

            // Then — начальное значение пустое
            assertEquals("", viewModel.messageText.value)

            // When — изменяем текст
            viewModel.onMessageTextChange("Hello")

            // Then — текст обновился
            assertEquals("Hello", viewModel.messageText.value)

            // When — заменяем текст
            viewModel.onMessageTextChange("World")

            // Then — текст обновился
            assertEquals("World", viewModel.messageText.value)
        }

    @Test
    fun sendMessage_withBlankText_doesNotSendAndKeepsTrimmedBehavior() =
        runTest {
            // Given
            val userId = 123
            coEvery { swApi.getMessages(any()) } returns emptyList()

            // When — ввод только пробелов
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.onMessageTextChange("   ")
            viewModel.sendMessage(userId)
            advanceUntilIdle()

            // Then — сообщение не отправлено
            coVerify(exactly = 0) { swApi.sendMessageTo(any(), any()) }

            // И убедимся, что текст в поле не очистился (не было отправки)
            assertEquals("   ", viewModel.messageText.value)
        }

    @Test
    fun onMessageTextChange_whenCalled_doesNotTrimInput() =
        runTest {
            // Given
            val dialogId = 1L
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)

            // When
            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.onMessageTextChange("  Hello  ")

            // Then — значение сохраняется без trim
            assertEquals("  Hello  ", viewModel.messageText.value)
        }

    // ==================== Analytics ====================

    @Test
    fun sendMessage_whenCalled_thenLogsUserActionSendMessage() =
        runTest {
            val dialogId = 1L
            val userId = 123
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
            coEvery { swApi.sendMessageTo(userId.toLong(), any()) } returns mockk(relaxed = true)

            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.onMessageTextChange("Test message")
            viewModel.sendMessage(userId)
            advanceUntilIdle()

            verify {
                analyticsService.log(AnalyticsEvent.UserAction(UserActionType.SEND_MESSAGE))
            }
        }

    @Test
    fun sendMessage_whenIOException_thenLogsAppErrorSendMessageFailed() =
        runTest {
            val dialogId = 1L
            val userId = 123
            coEvery { swApi.getMessages(dialogId) } returns listOf(testMessage)
            coEvery { swApi.sendMessageTo(userId.toLong(), any()) } throws IOException("Network error")

            viewModel = ChatViewModel(swApi, swRepository, userNotifier, logger, crashReporter, analyticsService)
            viewModel.loadMessages(dialogId)
            advanceUntilIdle()
            viewModel.onMessageTextChange("Test message")
            viewModel.sendMessage(userId)
            advanceUntilIdle()

            verify {
                analyticsService.log(
                    match { event ->
                        event is AnalyticsEvent.AppError &&
                            event.operation == AppErrorOperation.SEND_MESSAGE_FAILED
                    }
                )
            }
        }
}
