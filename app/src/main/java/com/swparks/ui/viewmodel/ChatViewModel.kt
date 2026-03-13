package com.swparks.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.repository.SWRepository
import com.swparks.network.SWApi
import com.swparks.ui.state.ChatEvent
import com.swparks.ui.state.ChatUiState
import com.swparks.util.AppError
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val userNotifier: UserNotifier
) : ViewModel(), IChatViewModel {

    private companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    override val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 1)
    override val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    override val messageText: MutableState<String> = mutableStateOf("")

    private var currentDialogId: Long? = null

    override fun loadMessages(dialogId: Long) {
        currentDialogId = dialogId
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            try {
                refreshMessagesInternal(dialogId)
            } catch (e: IOException) {
                Log.e(TAG, "Ошибка загрузки сообщений: ${e.message}", e)
                _uiState.value = ChatUiState.Error(e.message ?: "Неизвестная ошибка")
                userNotifier.handleError(
                    AppError.Generic(
                        e.message ?: "Ошибка загрузки сообщений",
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
                Log.e(TAG, "Ошибка обновления сообщений: ${e.message}", e)
                userNotifier.handleError(
                    AppError.Generic(
                        e.message ?: "Ошибка обновления сообщений",
                        e
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun refreshMessagesInternal(dialogId: Long) {
        val messages = swApi.getMessages(dialogId)
            .sortedBy { it.created }
        _uiState.value = ChatUiState.Success(messages)
    }

    override fun sendMessage(userId: Int) {
        val text = messageText.value.trim()
        if (text.isEmpty()) return

        val dialogId = currentDialogId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                swApi.sendMessageTo(userId.toLong(), text)
                messageText.value = ""
                refreshMessagesInternal(dialogId)
                // Эмитим событие об успешной отправке сообщения
                _events.emit(ChatEvent.MessageSent(dialogId))
            } catch (e: IOException) {
                Log.e(TAG, "Ошибка отправки сообщения: ${e.message}", e)
                userNotifier.handleError(
                    AppError.Generic(
                        e.message ?: "Ошибка отправки сообщения",
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
                Log.i(TAG, "Диалог помечен как прочитанный: dialogId=$dialogId, userId=$userId")
            } else {
                // Ошибки markAsRead логируем, но не беспокоим пользователя
                Log.e(
                    TAG,
                    "Ошибка markAsRead: ${result.exceptionOrNull()?.message}",
                    result.exceptionOrNull()
                )
            }
        }
    }
}
