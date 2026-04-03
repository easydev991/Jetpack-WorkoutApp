package com.swparks.ui.state

/**
 * Одноразовые события для экрана авторизации.
 */
sealed class LoginEvent {
    data class Success(
        val userId: Long
    ) : LoginEvent()

    data class ResetSuccess(
        val email: String
    ) : LoginEvent()
}
