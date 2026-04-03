package com.swparks.ui.state

/**
 * Одноразовые события для экрана регистрации.
 */
sealed class RegisterEvent {
    /**
     * Успешная регистрация.
     *
     * @param userId ID зарегистрированного пользователя
     */
    data class Success(
        val userId: Long
    ) : RegisterEvent()
}
