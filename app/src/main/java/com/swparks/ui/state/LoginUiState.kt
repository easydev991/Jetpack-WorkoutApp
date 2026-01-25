package com.swparks.ui.state

import com.swparks.model.SocialUpdates

/**
 * UI State для экрана авторизации.
 */
sealed class LoginUiState {
    /**
     * Начальное состояние.
     */
    data object Idle : LoginUiState()

    /**
     * Состояние загрузки.
     */
    data object Loading : LoginUiState()

    /**
     * Успешная авторизация с загруженными данными пользователя.
     *
     * @param socialUpdates Данные пользователя или null, если данные еще загружаются
     */
    data class LoginSuccess(val socialUpdates: SocialUpdates?) : LoginUiState()

    /**
     * Успешное восстановление пароля.
     */
    data object ResetSuccess : LoginUiState()

    /**
     * Ошибка авторизации.
     */
    data class LoginError(val message: String) : LoginUiState()

    /**
     * Ошибка восстановления пароля.
     */
    data class ResetError(val message: String) : LoginUiState()
}
