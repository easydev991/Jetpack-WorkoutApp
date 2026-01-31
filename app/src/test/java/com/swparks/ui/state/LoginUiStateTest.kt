package com.swparks.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты для LoginUiState.
 */
class LoginUiStateTest {

    @Test
    fun isBusy_whenIdle_returnsFalse() {
        val state = LoginUiState.Idle
        assertFalse("Idle состояние не должно быть занятым", state.isBusy)
    }

    @Test
    fun isBusy_whenLoading_returnsTrue() {
        val state = LoginUiState.Loading
        assertTrue("Loading состояние должно быть занятым", state.isBusy)
    }

    @Test
    fun isBusy_whenResetSuccess_returnsFalse() {
        val state = LoginUiState.ResetSuccess
        assertFalse("ResetSuccess состояние не должно быть занятым", state.isBusy)
    }

    @Test
    fun isBusy_whenLoginError_returnsFalse() {
        val state = LoginUiState.LoginError(message = "Error")
        assertFalse("LoginError состояние не должно быть занятым", state.isBusy)
    }

    @Test
    fun isBusy_whenResetError_returnsFalse() {
        val state = LoginUiState.ResetError(message = "Error")
        assertFalse("ResetError состояние не должно быть занятым", state.isBusy)
    }

    @Test
    fun loginError_whenCreatedWithMessage_containsCorrectMessage() {
        val message = "Неверный логин или пароль"
        val state = LoginUiState.LoginError(message)

        assertEquals(message, state.message)
    }

    @Test
    fun resetError_whenCreatedWithMessage_containsCorrectMessage() {
        val message = "Email не найден"
        val state = LoginUiState.ResetError(message)

        assertEquals(message, state.message)
    }

}
