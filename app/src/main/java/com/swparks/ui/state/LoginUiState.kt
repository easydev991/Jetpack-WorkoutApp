package com.swparks.ui.state

/**
 * UI State для экрана авторизации.
 */
sealed class LoginUiState {
    /**
     * Признак занятости UI: идет загрузка.
     * true для Loading или LoginSuccess
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
     */
    data class LoginSuccess(
        val userId: Long
    ) : LoginUiState()

    /**
     * Успешное восстановление пароля.
     */
    data object ResetSuccess : LoginUiState()

    /**
     * Ошибка авторизации.
     *
     * @param message Сообщение об ошибке для отображения пользователю
     * @param exception Исключение для определения типа ошибки (NetworkException и т.д.)
     */
    data class LoginError(
        val message: String,
        val exception: Throwable? = null
    ) : LoginUiState()

    /**
     * Ошибка восстановления пароля.
     *
     * @param message Сообщение об ошибке для отображения пользователю
     * @param exception Исключение для определения типа ошибки (NetworkException и т.д.)
     */
    data class ResetError(
        val message: String,
        val exception: Throwable? = null
    ) : LoginUiState()
}
