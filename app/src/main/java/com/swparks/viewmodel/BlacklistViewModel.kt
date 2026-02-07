package com.swparks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.repository.SWRepository
import com.swparks.model.AppError
import com.swparks.model.BlacklistAction
import com.swparks.model.User
import com.swparks.model.toApiOption
import com.swparks.util.ErrorReporter
import com.swparks.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана черного списка
 *
 * Управляет отображением черного списка пользователей
 *
 * @param swRepository Репозиторий для работы с API и загрузки данных
 * @param logger Логгер для записи сообщений
 * @param errorReporter Обработчик ошибок для отправки ошибок в UI
 */
@Suppress("TooGenericExceptionCaught")
class BlacklistViewModel(
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val errorReporter: ErrorReporter
) : ViewModel() {

    private companion object {
        private const val TAG = "BlacklistViewModel"
    }

    // UI State
    private val _uiState = MutableStateFlow<BlacklistUiState>(BlacklistUiState.Loading)
    val uiState: StateFlow<BlacklistUiState> = _uiState.asStateFlow()

    // Элемент для удаления из черного списка
    private val _itemToRemove = MutableStateFlow<User?>(null)
    val itemToRemove: StateFlow<User?> = _itemToRemove.asStateFlow()

    // Состояние диалога подтверждения удаления
    private val _showRemoveDialog = MutableStateFlow(false)
    val showRemoveDialog: StateFlow<Boolean> = _showRemoveDialog.asStateFlow()

    // Индикатор загрузки при удалении из черного списка
    private val _isRemoving = MutableStateFlow(false)
    val isRemoving: StateFlow<Boolean> = _isRemoving.asStateFlow()

    // Состояние алерта об успешной разблокировке
    private val _showSuccessAlert = MutableStateFlow(false)
    val showSuccessAlert: StateFlow<Boolean> = _showSuccessAlert.asStateFlow()

    // Имя разблокированного пользователя для алерта
    private val _unblockedUserName = MutableStateFlow<String?>(null)
    val unblockedUserName: StateFlow<String?> = _unblockedUserName.asStateFlow()

    init {
        viewModelScope.launch {
            swRepository.getBlacklistFlow()
                .catch { error ->
                    val message = "Ошибка при загрузке черного списка: ${error.message}"
                    logger.e(TAG, message)
                    _uiState.value = BlacklistUiState.Error("Ошибка загрузки черного списка")
                    errorReporter.handleError(AppError.Generic(message, error))
                }
                .collect { blacklist ->
                    _uiState.value = BlacklistUiState.Success(blacklist = blacklist)
                }
        }
    }

    /**
     * Показать диалог подтверждения удаления пользователя из черного списка
     *
     * @param user Пользователь для удаления
     */
    fun showRemoveDialog(user: User) {
        _itemToRemove.value = user
        _showRemoveDialog.value = true
    }

    /**
     * Удаление пользователя из черного списка
     *
     * @param user Пользователь для удаления
     */
    fun removeFromBlacklist(user: User) {
        viewModelScope.launch {
            try {
                // Закрываем диалог перед началом операции, чтобы индикатор загрузки был виден
                _showRemoveDialog.value = false
                _isRemoving.value = true
                // Обновляем состояние Success с isLoading = true
                val currentState = _uiState.value
                if (currentState is BlacklistUiState.Success) {
                    _uiState.value = currentState.copy(isLoading = true)
                }
                val result =
                    swRepository.blacklistAction(user, BlacklistAction.UNBLOCK.toApiOption())
                result.fold(
                    onSuccess = {
                        logger.i(TAG, "Пользователь удален из черного списка: userId=${user.id}")
                        _itemToRemove.value = null
                        _isRemoving.value = false
                        // Сохраняем имя пользователя и показываем алерт
                        _unblockedUserName.value = user.name
                        _showSuccessAlert.value = true
                        // Возвращаем состояние Success с isLoading = false
                        if (_uiState.value is BlacklistUiState.Success) {
                            _uiState.value =
                                (_uiState.value as BlacklistUiState.Success).copy(isLoading = false)
                        }
                    },
                    onFailure = { error ->
                        val message = "Ошибка при удалении из черного списка: ${error.message}"
                        logger.e(TAG, message)
                        errorReporter.handleError(AppError.Generic(message, error))
                        _itemToRemove.value = null
                        _isRemoving.value = false
                        // Возвращаем состояние Success с isLoading = false
                        if (_uiState.value is BlacklistUiState.Success) {
                            _uiState.value =
                                (_uiState.value as BlacklistUiState.Success).copy(isLoading = false)
                        }
                    }
                )
            } catch (e: Exception) {
                val message = "Ошибка при удалении из черного списка: ${e.message}"
                logger.e(TAG, message)
                errorReporter.handleError(AppError.Generic(message, e))
                _itemToRemove.value = null
                _isRemoving.value = false
                // Возвращаем состояние Success с isLoading = false
                if (_uiState.value is BlacklistUiState.Success) {
                    _uiState.value =
                        (_uiState.value as BlacklistUiState.Success).copy(isLoading = false)
                }
            }
        }
    }

    /**
     * Отмена удаления пользователя из черного списка
     */
    fun cancelRemove() {
        _showRemoveDialog.value = false
        _itemToRemove.value = null
    }

    /**
     * Закрытие алерта об успешной разблокировке
     */
    fun dismissSuccessAlert() {
        _showSuccessAlert.value = false
        _unblockedUserName.value = null
    }
}
