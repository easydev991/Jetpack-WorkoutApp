package com.swparks.ui.viewmodel

import com.swparks.ui.state.ChangePasswordEvent
import com.swparks.ui.state.ChangePasswordUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IChangePasswordViewModel {
    val uiState: StateFlow<ChangePasswordUiState>
    val events: Flow<ChangePasswordEvent>

    fun onCurrentPasswordChange(value: String)
    fun onNewPasswordChange(value: String)
    fun onConfirmPasswordChange(value: String)
    fun onSaveClick()
}
