package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.R
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.usecase.IChangePasswordUseCase
import com.swparks.ui.state.ChangePasswordEvent
import com.swparks.ui.state.ChangePasswordUiState
import com.swparks.util.AppError
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooGenericExceptionCaught")
class ChangePasswordViewModel(
    private val changePasswordUseCase: IChangePasswordUseCase,
    private val logger: Logger,
    private val userNotifier: UserNotifier,
    private val resources: ResourcesProvider
) : ViewModel(), IChangePasswordViewModel {

    private companion object {
        private const val TAG = "ChangePasswordViewModel"
    }

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    override val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChangePasswordEvent>(Channel.BUFFERED)
    override val events = _events.receiveAsFlow()

    override fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPassword = value) }
    }

    override fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value) }
    }

    override fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }

    override fun onSaveClick() {
        val state = _uiState.value
        if (!state.canSave) {
            logger.w(TAG, "Попытка сохранить с невалидными данными")
            return
        }

        logger.i(TAG, "Начало смены пароля")
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val result = changePasswordUseCase(
                current = state.currentPassword,
                new = state.newPassword
            )

            result.fold(
                onSuccess = {
                    logger.i(TAG, "Пароль успешно изменён")
                    userNotifier.showInfo(resources.getString(R.string.password_changed_successfully))
                    _events.send(ChangePasswordEvent.NavigateBack)
                },
                onFailure = { error ->
                    logger.e(TAG, "Ошибка смены пароля: ${error.message}", error)
                    _uiState.update { it.copy(isSaving = false) }
                    userNotifier.handleError(
                        AppError.Generic(
                            error.message ?: "Ошибка смены пароля",
                            error
                        )
                    )
                }
            )
        }
    }
}
