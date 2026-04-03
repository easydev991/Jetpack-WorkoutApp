package com.swparks.ui.state

/**
 * UI State для экрана регистрации.
 */
sealed class RegisterUiState {
    /**
     * Признак занятости UI: идет загрузка.
     */
    val isBusy: Boolean
        get() = this is Loading

    /**
     * Начальное состояние.
     */
    data object Idle : RegisterUiState()

    /**
     * Состояние загрузки (регистрация в процессе).
     */
    data object Loading : RegisterUiState()

    /**
     * Успешная регистрация.
     *
     * @param userId ID зарегистрированного пользователя
     */
    data class Success(
        val userId: Long
    ) : RegisterUiState()

    /**
     * Ошибка регистрации.
     *
     * @param message Сообщение об ошибке для отображения пользователю
     * @param exception Исключение для определения типа ошибки
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : RegisterUiState()
}
