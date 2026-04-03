package com.swparks.ui.state

import com.swparks.R

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSaving: Boolean = false
) {
    val canSave: Boolean
        get() =
            currentPassword.isNotEmpty() &&
                newPassword.isNotEmpty() &&
                confirmPassword.isNotEmpty() &&
                newPassword == confirmPassword &&
                newPassword.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
                !isSaving

    /**
     * Ошибка для поля "Новый пароль".
     * Показывается если поле не пустое и длина меньше MIN_PASSWORD_LENGTH.
     */
    val newPasswordError: Int?
        get() =
            if (newPassword.isNotEmpty() && newPassword.length < MIN_PASSWORD_LENGTH) {
                R.string.password_short
            } else {
                null
            }

    /**
     * Ошибка для поля "Подтверждение пароля".
     * Показывается если поле не пустое и пароли не совпадают.
     */
    val confirmPasswordError: Int?
        get() =
            if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                R.string.password_not_match
            } else {
                null
            }

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
        const val MAX_PASSWORD_LENGTH = 32
    }
}

sealed interface ChangePasswordEvent {
    data object NavigateBack : ChangePasswordEvent
}
