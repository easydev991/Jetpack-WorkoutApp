package com.swparks.ui.state

/**
 * UI State для экрана авторизации.
 */
sealed class LoginUiState {
    /**
     * Признак занятости UI: идет загрузка или загружаются данные пользователя после авторизации.
     * true для Loading или LoginSuccess с null socialUpdates
     */
    val isBusy: Boolean
        get() =
            this is Loading || this is LoginSuccess

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
     * @param userId ID авторизованного пользователя
     * @param socialUpdates Данные пользователя или null, если данные еще загружаются
     */
    data class LoginSuccess(val userId: Long) : LoginUiState()

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
