package com.swparks.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.network.SWApi
import com.swparks.ui.state.ChatUiState
import com.swparks.util.AppError
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана чата (диалога).
 *
 * Управляет состоянием списка сообщений, отправкой сообщений
 * и отметкой диалога как прочитанного.
 *
 * @property swApi API клиент для работы с сервером
 * @property userNotifier Нотификатор для отображения ошибок в UI
 */
class ChatViewModel(
    private val swApi: SWApi,
    private val userNotifier: UserNotifier
) : ViewModel(), IChatViewModel {

    private companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    override val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    override val messageText: MutableState<String> = mutableStateOf("")

    private var currentDialogId: Long? = null

    override fun loadMessages(dialogId: Long) {
        currentDialogId = dialogId
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            try {
                val messages = swApi.getMessages(dialogId)
                _uiState.value = ChatUiState.Success(messages)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки сообщений: ${e.message}", e)
                _uiState.value = ChatUiState.Error(e.message ?: "Неизвестная ошибка")
                userNotifier.handleError(AppError.Generic(e.message ?: "Ошибка загрузки сообщений", e))
            }
        }
    }

    override fun refreshMessages() {
        val dialogId = currentDialogId ?: return
        viewModelScope.launch {
            try {
                val messages = swApi.getMessages(dialogId)
                _uiState.value = ChatUiState.Success(messages)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обновления сообщений: ${e.message}", e)
                userNotifier.handleError(AppError.Generic(e.message ?: "Ошибка обновления сообщений", e))
            }
        }
    }

    override fun sendMessage(userId: Int) {
        val text = messageText.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                swApi.sendMessageTo(userId.toLong(), text)
                messageText.value = ""
                refreshMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка отправки сообщения: ${e.message}", e)
                userNotifier.handleError(AppError.Generic(e.message ?: "Ошибка отправки сообщения", e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun markAsRead(userId: Int) {
        viewModelScope.launch {
            try {
                swApi.markAsRead(userId.toLong())
                Log.i(TAG, "Диалог помечен как прочитанный: userId=$userId")
            } catch (e: Exception) {
                // Ошибки markAsRead логируем, но не беспокоим пользователя
                Log.e(TAG, "Ошибка markAsRead: ${e.message}", e)
            }
        }
    }
}
