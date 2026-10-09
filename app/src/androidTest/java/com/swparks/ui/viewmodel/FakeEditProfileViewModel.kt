package com.swparks.ui.viewmodel

import android.net.Uri
import com.swparks.ui.model.Gender
import com.swparks.ui.state.EditProfileEvent
import com.swparks.ui.state.EditProfileUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Ручной stateful fake [EditProfileViewModel] для UI-тестов экрана профиля:
 * обновляет форму в [uiState], события не эмитит.
 */
class FakeEditProfileViewModel(
    initialState: EditProfileUiState = EditProfileUiState(isLoading = false)
) : IEditProfileViewModel {
    private val _uiState = MutableStateFlow(initialState)
    override val uiState: StateFlow<EditProfileUiState> = _uiState

    private val _events = MutableSharedFlow<EditProfileEvent>()
    override val events: SharedFlow<EditProfileEvent> = _events.asSharedFlow()

    override fun onLoginChange(value: String) {
        _uiState.value =
            _uiState.value.copy(
                userForm = _uiState.value.userForm.copy(name = value)
            )
    }

    override fun onEmailChange(value: String) {
        _uiState.value =
            _uiState.value.copy(
                userForm = _uiState.value.userForm.copy(email = value)
            )
    }

    override fun onFullNameChange(value: String) {
        _uiState.value =
            _uiState.value.copy(
                userForm = _uiState.value.userForm.copy(fullname = value)
            )
    }

    override fun onGenderChange(gender: Gender) {
        _uiState.value =
            _uiState.value.copy(
                userForm = _uiState.value.userForm.copy(genderCode = gender.rawValue)
            )
    }

    override fun onBirthDateChange(timestamp: Long) {
    }

    override fun onCountryClick() {
    }

    override fun onCityClick() {
    }

    override fun onChangePasswordClick() {
    }

    override fun onChangeAvatarClick() {
    }

    override fun onAvatarSelected(uri: Uri?) {
    }

    override fun onSaveClick() {
    }

    override fun onCountrySelected(countryId: String) {
    }

    override fun onCitySelected(cityId: String) {
    }

    override fun resetChanges() {
    }

    override fun onDeleteProfileClick() {
    }
}
