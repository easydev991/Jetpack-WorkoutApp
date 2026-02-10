package com.swparks.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.R
import com.swparks.domain.usecase.ITextEntryUseCase
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.state.TextEntryEvent
import com.swparks.ui.state.TextEntryUiState
import com.swparks.util.AppError
import com.swparks.util.ErrorReporter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для управления экраном ввода текста.
 *
 * Управляет состоянием UI экрана ввода текста для создания/редактирования
 * комментариев и записей в дневнике.
 *
 * @param textEntryUseCase Use case для добавления/редактирования комментариев и записей
 * @param errorReporter Интерфейс для обработки и отправки ошибок
 * @param mode Режим работы экрана (определяет тип операции и заголовок)
 * @param context Контекст приложения для проверки сетевого подключения
 */
class TextEntryViewModel(
    private val textEntryUseCase: ITextEntryUseCase,
    private val errorReporter: ErrorReporter,
    private val mode: TextEntryMode,
    private val context: Context
) : ViewModel(), ITextEntryViewModel {

    private val _uiState = MutableStateFlow(TextEntryUiState(mode))
    override val uiState: StateFlow<TextEntryUiState> = _uiState.asStateFlow()

    private val _events = Channel<TextEntryEvent>(Channel.BUFFERED)
    override val events = _events.receiveAsFlow()

    /**
     * Обновляет текст в текстовом поле.
     * Вычисляет флаг isSendEnabled на основе режима и текста.
     */
    override fun onTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(
            text = text,
            isSendEnabled = isSendEnabled(text),
            error = null
        )
    }

    /**
     * Отправляет текст на сервер.
     * Проверяет валидацию, сетевое подключение и вызывает соответствующий Use Case.
     */
    override fun onSend() {
        val trimmedText = _uiState.value.text.trim()

        // Валидация пустого текста
        if (trimmedText.isEmpty()) {
            _uiState.value =
                _uiState.value.copy(error = context.getString(R.string.text_entry_empty_error))
            return
        }

        // Проверка изменения текста при редактировании
        val isTextChanged = when (mode) {
            is TextEntryMode.EditPark,
            is TextEntryMode.EditEvent,
            is TextEntryMode.EditJournalEntry -> {
                val editInfo = when (mode) {
                    is TextEntryMode.EditPark -> mode.editInfo
                    is TextEntryMode.EditEvent -> mode.editInfo
                    is TextEntryMode.EditJournalEntry -> mode.editInfo
                    else -> return
                }
                trimmedText != editInfo.oldEntry.trim()
            }

            else -> true
        }

        if (!isTextChanged) {
            _uiState.value =
                _uiState.value.copy(error = context.getString(R.string.text_entry_empty_error))
            return
        }

        // Проверка сетевого подключения
        if (!isNetworkAvailable()) {
            _uiState.value =
                _uiState.value.copy(error = context.getString(R.string.error_network_io))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = when (mode) {
                is TextEntryMode.NewForPark -> textEntryUseCase.addParkComment(
                    mode.parkId,
                    trimmedText
                )

                is TextEntryMode.NewForEvent -> textEntryUseCase.addEventComment(
                    mode.eventId,
                    trimmedText
                )

                is TextEntryMode.NewForJournal -> textEntryUseCase.addJournalEntry(
                    mode.ownerId,
                    mode.journalId,
                    trimmedText
                )

                is TextEntryMode.EditPark -> textEntryUseCase.editParkComment(
                    mode.editInfo.parentObjectId,
                    mode.editInfo.entryId,
                    trimmedText
                )

                is TextEntryMode.EditEvent -> textEntryUseCase.editEventComment(
                    mode.editInfo.parentObjectId,
                    mode.editInfo.entryId,
                    trimmedText
                )

                is TextEntryMode.EditJournalEntry -> textEntryUseCase.editJournalEntry(
                    mode.ownerId,
                    mode.editInfo.parentObjectId,
                    mode.editInfo.entryId,
                    trimmedText
                )
            }

            _uiState.value = _uiState.value.copy(isLoading = false)

            result.fold(
                onSuccess = {
                    _events.trySend(TextEntryEvent.Success)
                },
                onFailure = { exception ->
                    val errorMessage = context.getString(
                        R.string.text_entry_error,
                        exception.message ?: ""
                    )
                    _events.trySend(TextEntryEvent.Error(errorMessage))
                    val appError = AppError.Generic(
                        message = errorMessage,
                        throwable = exception
                    )
                    errorReporter.handleError(appError)
                }
            )
        }
    }

    /**
     * Сбрасывает состояние ошибки.
     */
    override fun onDismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Сбрасывает состояние ViewModel для новой сессии (при открытии sheet).
     */
    override fun resetState() {
        _uiState.value = TextEntryUiState(mode)
    }

    /**
     * Проверяет, можно ли отправить текст на основе режима и текущего текста.
     */
    private fun isSendEnabled(text: String): Boolean {
        val trimmedText = text.trim()

        return when (mode) {
            is TextEntryMode.NewForPark,
            is TextEntryMode.NewForEvent,
            is TextEntryMode.NewForJournal -> trimmedText.isNotEmpty()

            is TextEntryMode.EditPark,
            is TextEntryMode.EditEvent,
            is TextEntryMode.EditJournalEntry -> {
                val editInfo = when (mode) {
                    is TextEntryMode.EditPark -> mode.editInfo
                    is TextEntryMode.EditEvent -> mode.editInfo
                    is TextEntryMode.EditJournalEntry -> mode.editInfo
                    else -> return false
                }
                trimmedText.isNotEmpty() && trimmedText != editInfo.oldEntry.trim()
            }
        }
    }

    /**
     * Проверяет доступность сетевого подключения.
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
