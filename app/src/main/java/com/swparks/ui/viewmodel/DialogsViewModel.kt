package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.R
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.MessagesRepository
import com.swparks.ui.state.DialogsUiState
import com.swparks.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * ViewModel для экрана списка диалогов.
 *
 * Управляет состоянием списка диалогов, загружает данные из репозитория
 * и обрабатывает действия пользователя.
 *
 * @property messagesRepository Репозиторий для работы с диалогами
 * @property swRepository Репозиторий для работы с сервером (удаление диалога)
 * @property logger Логгер для записи ошибок и отладочной информации
 * @property resources Провайдер строковых ресурсов для локализации
 */
class DialogsViewModel(
    private val messagesRepository: MessagesRepository,
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val resources: ResourcesProvider
) : ViewModel(), IDialogsViewModel {

    private companion object {
        private const val TAG = "DialogsViewModel"
        private const val FLOW_SUBSCRIPTION_TIMEOUT_MS = 5000L
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
                    logger.d(
                        TAG,
                        "Collect получил ${dialogs.size} диалогов, hasSuccessfullyLoaded=$hasSuccessfullyLoaded"
                    )
                    // Если был успешный ответ от сервера или кэш не пустой - показываем Success
                    if (hasSuccessfullyLoaded || dialogs.isNotEmpty()) {
                        logger.i(TAG, "Устанавливаем Success с ${dialogs.size} диалогами")
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
            // После успешной загрузки с сервера, данные сохранены в БД.
            // Всегда читаем текущее состояние из БД и обновляем uiState,
            // независимо от текущего состояния (Loading или Success).
            // Это гарантирует корректное отображение EmptyStateView после авторизации.
            try {
                val dialogs = messagesRepository.dialogs.first()
                _uiState.value = DialogsUiState.Success(dialogs = dialogs)
            } catch (e: IOException) {
                logger.e(TAG, "Ошибка при чтении диалогов: ${e.message}")
                _uiState.value = DialogsUiState.Success(dialogs = emptyList())
            }
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

    // Индикатор удаления диалога
    private val _isDeleting = MutableStateFlow(false)
    override val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    // Индикатор отметки диалога как прочитанного
    private val _isMarkingAsRead = MutableStateFlow(false)
    override val isMarkingAsRead: StateFlow<Boolean> = _isMarkingAsRead.asStateFlow()

    // Общий индикатор любой операции обновления
    override val isUpdating: StateFlow<Boolean> = combine(
        _isDeleting,
        _isMarkingAsRead
    ) { deleting, marking -> deleting || marking }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(FLOW_SUBSCRIPTION_TIMEOUT_MS),
            false
        )

    override fun deleteDialog(dialogId: Long) {
        viewModelScope.launch {
            _isDeleting.value = true
            val result = swRepository.deleteDialog(dialogId)
            _isDeleting.value = false

            if (result.isFailure) {
                _syncError.value = resources.getString(R.string.dialog_delete_error)
                logger.e(TAG, "Ошибка удаления диалога: ${result.exceptionOrNull()?.message}")
            }
            // При успехе Flow из Room обновит список автоматически
        }
    }

    override fun markDialogAsRead(dialogId: Long, userId: Int) {
        viewModelScope.launch {
            _isMarkingAsRead.value = true
            _syncError.value = null

            val result = swRepository.markDialogAsRead(dialogId, userId)
            _isMarkingAsRead.value = false

            if (result.isFailure) {
                logger.e(TAG, "markDialogAsRead failed: ${result.exceptionOrNull()?.message}")
                _syncError.value = resources.getString(R.string.sync_error_message)
            }
            // При успехе Flow из Room обновит unreadCount автоматически
        }
    }
}
