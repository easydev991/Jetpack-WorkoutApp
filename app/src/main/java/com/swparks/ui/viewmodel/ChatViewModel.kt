package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.analytics.AnalyticsEvent
import com.swparks.analytics.AnalyticsService
import com.swparks.analytics.AppErrorOperation
import com.swparks.analytics.UserActionType
import com.swparks.data.repository.SWRepository
import com.swparks.network.SWApi
import com.swparks.ui.state.ChatEvent
import com.swparks.ui.state.ChatUiState
import com.swparks.util.AppError
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * ViewModel для экрана чата (диалога).
 *
 * Управляет состоянием списка сообщений, отправкой сообщений
 * и отметкой диалога как прочитанного.
 *
 * @property swApi API клиент для работы с сервером
 * @property swRepository Репозиторий для работы с данными (включая обновление локальной БД)
 * @property userNotifier Нотификатор для отображения ошибок в UI
 */
class ChatViewModel(
    private val swApi: SWApi,
    private val swRepository: SWRepository,
    private val userNotifier: UserNotifier,
    private val logger: Logger,
    private val crashReporter: CrashReporter,
    private val analyticsService: AnalyticsService
) : ViewModel(),
    IChatViewModel {
    private companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    override val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 1)
    override val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private val _messageText = MutableStateFlow("")
    override val messageText: StateFlow<String> = _messageText.asStateFlow()

    override fun onMessageTextChange(value: String) {
        _messageText.value = value
    }

    private var currentDialogId: Long? = null

    override fun loadMessages(dialogId: Long) {
        currentDialogId = dialogId
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            try {
                refreshMessagesInternal(dialogId)
            } catch (e: IOException) {
                logger.e(TAG, "Ошибка загрузки сообщений: ${e.message}", e)
                crashReporter.logException(e, "Ошибка загрузки сообщений")
                _uiState.value = ChatUiState.Error(e.message ?: "Неизвестная ошибка")
                userNotifier.handleError(
                    AppError.Generic(
                        e.message ?: "Ошибка загрузки сообщений",
                        e
                    )
                )
            } catch (e: HttpException) {
                logger.e(TAG, "HTTP ошибка загрузки сообщений: ${e.code()} ${e.message()}", e)
                crashReporter.logException(e, "HTTP ошибка загрузки сообщений: ${e.code()}")
                _uiState.value = ChatUiState.Error(e.message())
                userNotifier.handleError(
                    AppError.Generic(
                        e.message(),
                        e
                    )
                )
            }
        }
    }

    override fun refreshMessages() {
        val dialogId = currentDialogId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                refreshMessagesInternal(dialogId)
            } catch (e: IOException) {
                logger.e(TAG, "Ошибка обновления сообщений: ${e.message}", e)
                crashReporter.logException(e, "Ошибка обновления сообщений")
                userNotifier.handleError(
                    AppError.Generic(
                        e.message ?: "Ошибка обновления сообщений",
                        e
                    )
                )
            } catch (e: HttpException) {
                logger.e(TAG, "HTTP ошибка обновления сообщений: ${e.code()} ${e.message()}", e)
                crashReporter.logException(e, "HTTP ошибка обновления сообщений: ${e.code()}")
                userNotifier.handleError(
                    AppError.Generic(
                        e.message(),
                        e
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun refreshMessagesInternal(dialogId: Long) {
        val messages =
            swApi
                .getMessages(dialogId)
                .sortedBy { it.created }
        _uiState.value = ChatUiState.Success(messages)
    }

    override fun sendMessage(userId: Int) {
        val text = _messageText.value.trim()
        if (text.isEmpty()) return

        val dialogId = currentDialogId ?: return
        analyticsService.log(AnalyticsEvent.UserAction(UserActionType.SEND_MESSAGE))
        viewModelScope.launch {
            _isLoading.value = true
            try {
                swApi.sendMessageTo(userId.toLong(), text)
                _messageText.value = ""
                refreshMessagesInternal(dialogId)
                // Эмитим событие об успешной отправке сообщения
                _events.emit(ChatEvent.MessageSent(dialogId))
            } catch (e: IOException) {
                logger.e(TAG, "Ошибка отправки сообщения: ${e.message}", e)
                analyticsService.log(
                    AnalyticsEvent.AppError(AppErrorOperation.SEND_MESSAGE_FAILED, e)
                )
                crashReporter.logException(e, "Ошибка отправки сообщения")
                userNotifier.handleError(
                    AppError.Generic(
                        e.message ?: "Ошибка отправки сообщения",
                        e
                    )
                )
            } catch (e: HttpException) {
                logger.e(TAG, "HTTP ошибка отправки сообщения: ${e.code()} ${e.message()}", e)
                analyticsService.log(
                    AnalyticsEvent.AppError(AppErrorOperation.SEND_MESSAGE_FAILED, e)
                )
                crashReporter.logException(e, "HTTP ошибка отправки сообщения: ${e.code()}")
                userNotifier.handleError(
                    AppError.Generic(
                        e.message(),
                        e
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun markAsRead(userId: Int) {
        val dialogId = currentDialogId ?: return
        viewModelScope.launch {
            val result = swRepository.markDialogAsRead(dialogId, userId)
            if (result.isSuccess) {
                logger.i(TAG, "Диалог помечен как прочитанный: dialogId=$dialogId, userId=$userId")
            } else {
                // Ошибки markAsRead логируем, но не беспокоим пользователя
                val error = result.exceptionOrNull()
                logger.e(TAG, "Ошибка markAsRead: ${error?.message}", error)
                if (error != null) {
                    crashReporter.logException(error, "Ошибка markAsRead")
                }
            }
        }
    }
}
