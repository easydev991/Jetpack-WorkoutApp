package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.domain.repository.MessagesRepository
import com.swparks.ui.state.DialogsUiState
import com.swparks.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана списка диалогов.
 *
 * Управляет состоянием списка диалогов, загружает данные из репозитория
 * и обрабатывает действия пользователя.
 *
 * @property messagesRepository Репозиторий для работы с диалогами
 * @property logger Логгер для записи ошибок и отладочной информации
 */
class DialogsViewModel(
    private val messagesRepository: MessagesRepository,
    private val logger: Logger
) : ViewModel(), IDialogsViewModel {

    private companion object {
        private const val TAG = "DialogsViewModel"
    }

    private val _uiState = MutableStateFlow<DialogsUiState>(DialogsUiState.Loading)
    override val uiState: StateFlow<DialogsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Индикатор загрузки диалогов после авторизации
    private val _isLoadingDialogs = MutableStateFlow(false)
    override val isLoadingDialogs: StateFlow<Boolean> = _isLoadingDialogs.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    override val syncError: StateFlow<String?> = _syncError.asStateFlow()

    // Флаг для отслеживания первой загрузки (показываем Error если кэш пустой)
    private var hasSuccessfullyLoaded = false

    init {
        // Подписываемся на Flow из Room
        viewModelScope.launch {
            messagesRepository.dialogs
                .catch { error ->
                    logger.e(TAG, "Ошибка при загрузке диалогов: ${error.message}")
                    _uiState.value = DialogsUiState.Error("Ошибка загрузки диалогов")
                }
                .collect { dialogs ->
                    // Если был успешный ответ от сервера или кэш не пустой - показываем Success
                    if (hasSuccessfullyLoaded || dialogs.isNotEmpty()) {
                        _uiState.value = DialogsUiState.Success(dialogs = dialogs)
                    }
                    // Иначе оставляем Loading до завершения refresh()
                }
        }
        // Первоначальная загрузка данных
        refresh()
    }

    override fun refresh() {
        // Защита от множественных вызовов (debounce)
        if (_isRefreshing.value) return

        viewModelScope.launch {
            _isRefreshing.value = true
            _syncError.value = null  // Сбрасываем предыдущую ошибку

            // Если это первая загрузка после сброса - устанавливаем isLoadingDialogs
            if (_uiState.value is DialogsUiState.Loading) {
                _isLoadingDialogs.value = true
            }

            loadDialogsInternal()
        }
    }

    override fun loadDialogsAfterAuth() {
        // Защита от множественных вызовов (debounce)
        if (_isRefreshing.value) return

        viewModelScope.launch {
            _isRefreshing.value = true
            _syncError.value = null

            // ВСЕГДА показываем LoadingOverlayView после авторизации
            _isLoadingDialogs.value = true

            loadDialogsInternal()
        }
    }

    /**
     * Внутренний метод загрузки диалогов.
     * Вызывается из refresh() и loadDialogsAfterAuth().
     */
    private suspend fun loadDialogsInternal() {
        val result = messagesRepository.refreshDialogs()
        _isRefreshing.value = false
        _isLoadingDialogs.value = false

        if (result.isSuccess) {
            hasSuccessfullyLoaded = true
            // Если сервер вернул 0 диалогов, Flow из Room может не эмитить повторно
            // (значение не изменилось: было 0, осталось 0).
            // Явно устанавливаем Success с текущим состоянием (пустой или заполненный список).
            val currentState = _uiState.value
            if (currentState is DialogsUiState.Loading) {
                // При первой загрузке с пустым результатом - показываем EmptyState
                _uiState.value = DialogsUiState.Success(dialogs = emptyList())
            }
            // Если уже Success, collect обновит состояние из Room
        } else {
            val currentState = _uiState.value
            // Если кэш пустой и это первая загрузка - показываем Error
            // Проверяем Loading (первичная загрузка) или Success с пустым списком
            if (currentState is DialogsUiState.Loading ||
                (currentState is DialogsUiState.Success && currentState.dialogs.isEmpty())
            ) {
                _uiState.value = DialogsUiState.Error("Ошибка загрузки диалогов")
            } else {
                // Иначе показываем ошибку синхронизации в Snackbar
                _syncError.value = "Ошибка синхронизации"
            }
        }
    }

    override fun onDialogClick(dialogId: Long, userId: Int?) {
        if (userId == null) {
            logger.w(TAG, "Нажат диалог без userId: dialogId=$dialogId")
            return
        }
        logger.i(TAG, "Нажат диалог: dialogId=$dialogId, userId=$userId")
    }

    override fun dismissSyncError() {
        _syncError.value = null
    }
}
