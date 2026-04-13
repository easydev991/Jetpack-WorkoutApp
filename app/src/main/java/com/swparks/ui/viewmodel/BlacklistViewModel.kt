package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.analytics.AnalyticsEvent
import com.swparks.analytics.AnalyticsService
import com.swparks.analytics.AppErrorOperation
import com.swparks.analytics.UserActionType
import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.toApiOption
import com.swparks.ui.state.BlacklistAction
import com.swparks.ui.state.BlacklistUiState
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.swparks.ui.model.BlacklistAction as ApiBlacklistAction

/**
 * ViewModel для экрана черного списка
 *
 * Управляет отображением черного списка пользователей
 *
 * @param swRepository Репозиторий для работы с API и загрузки данных
 * @param logger Логгер для записи сообщений
 * @param userNotifier Обработчик ошибок для отправки ошибок в UI
 */
@Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException", "UnusedPrivateProperty")
class BlacklistViewModel(
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val userNotifier: UserNotifier,
    private val analyticsService: AnalyticsService
) : ViewModel(),
    IBlacklistViewModel {
    private companion object {
        private const val TAG = "BlacklistViewModel"
    }

    private val _uiState = MutableStateFlow<BlacklistUiState>(BlacklistUiState.Loading)
    override val uiState: StateFlow<BlacklistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            swRepository
                .getBlacklistFlow()
                .catch { error ->
                    val message = "Ошибка при загрузке черного списка: ${error.message}"
                    logger.e(TAG, message)
                    _uiState.value = BlacklistUiState.Error("Ошибка загрузки черного списка")
                    userNotifier.handleError(AppError.Generic(message, error))
                }.collect { blacklist ->
                    val currentState = _uiState.value
                    _uiState.value =
                        if (currentState is BlacklistUiState.Success) {
                            currentState.copy(blacklist = blacklist)
                        } else {
                            BlacklistUiState.Success(blacklist = blacklist)
                        }
                }
        }
    }

    override fun onAction(action: BlacklistAction) {
        when (action) {
            is BlacklistAction.ShowRemoveDialog -> showRemoveDialog(action.user)
            is BlacklistAction.Remove -> removeFromBlacklist(action.user)
            BlacklistAction.CancelRemove -> cancelRemove()
            BlacklistAction.DismissSuccessAlert -> dismissSuccessAlert()
            BlacklistAction.Back -> { // Обрабатывается в UI
            }
        }
    }

    private fun showRemoveDialog(user: com.swparks.data.model.User) {
        val currentState = _uiState.value
        if (currentState is BlacklistUiState.Success) {
            _uiState.value =
                currentState.copy(
                    itemToRemove = user,
                    showRemoveDialog = true
                )
        }
    }

    private fun removeFromBlacklist(user: com.swparks.data.model.User) {
        analyticsService.log(AnalyticsEvent.UserAction(UserActionType.UNBLOCK_USER))

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState !is BlacklistUiState.Success) return@launch

                _uiState.value =
                    currentState.copy(
                        showRemoveDialog = false,
                        isRemoving = true,
                        isLoading = true
                    )

                val result =
                    swRepository.blacklistAction(user, ApiBlacklistAction.UNBLOCK.toApiOption())
                result.fold(
                    onSuccess = {
                        logger.i(TAG, "Пользователь удален из черного списка: userId=${user.id}")
                        _uiState.value =
                            currentState.copy(
                                showRemoveDialog = false,
                                isLoading = false,
                                itemToRemove = null,
                                isRemoving = false,
                                showSuccessAlert = true,
                                unblockedUserName = user.name
                            )
                    },
                    onFailure = { error ->
                        val message = "Ошибка при удалении из черного списка: ${error.message}"
                        logger.e(TAG, message)
                        analyticsService.log(
                            AnalyticsEvent.AppError(AppErrorOperation.UNBLOCK_FAILED, error)
                        )
                        userNotifier.handleError(AppError.Generic(message, error))
                        _uiState.value =
                            currentState.copy(
                                showRemoveDialog = false,
                                isLoading = false,
                                itemToRemove = null,
                                isRemoving = false
                            )
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val message = "Ошибка при удалении из черного списка: ${e.message}"
                logger.e(TAG, message)
                analyticsService.log(
                    AnalyticsEvent.AppError(AppErrorOperation.UNBLOCK_FAILED, e)
                )
                userNotifier.handleError(AppError.Generic(message, e))
                val currentState = _uiState.value
                if (currentState is BlacklistUiState.Success) {
                    _uiState.value =
                        currentState.copy(
                            showRemoveDialog = false,
                            isLoading = false,
                            itemToRemove = null,
                            isRemoving = false
                        )
                }
            }
        }
    }

    private fun cancelRemove() {
        val currentState = _uiState.value
        if (currentState is BlacklistUiState.Success) {
            _uiState.value =
                currentState.copy(
                    showRemoveDialog = false,
                    itemToRemove = null
                )
        }
    }

    private fun dismissSuccessAlert() {
        val currentState = _uiState.value
        if (currentState is BlacklistUiState.Success) {
            _uiState.value =
                currentState.copy(
                    showSuccessAlert = false,
                    unblockedUserName = null
                )
        }
    }
}
