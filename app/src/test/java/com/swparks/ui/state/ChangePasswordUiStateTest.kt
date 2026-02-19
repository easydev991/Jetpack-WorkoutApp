package com.swparks.ui.state

import com.swparks.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChangePasswordUiStateTest {

    // Tests for newPasswordError

    @Test
    fun newPasswordError_whenEmpty_thenIsNull() {
        // Given
        val state = ChangePasswordUiState(newPassword = "")

        // When
        val error = state.newPasswordError

        // Then
        assertNull(error)
    }

    @Test
    fun newPasswordError_whenShortPassword_thenIsPasswordShort() {
        // Given
        val state = ChangePasswordUiState(newPassword = "12345") // 5 chars < 6

        // When
        val error = state.newPasswordError

        // Then
        assertEquals(R.string.password_short, error)
    }

    @Test
    fun newPasswordError_whenValidPassword_thenIsNull() {
        // Given
        val state = ChangePasswordUiState(newPassword = "123456") // 6 chars = min

        // When
        val error = state.newPasswordError

        // Then
        assertNull(error)
    }

    @Test
    fun newPasswordError_whenLongEnoughPassword_thenIsNull() {
        // Given
        val state = ChangePasswordUiState(newPassword = "validPassword123")

        // When
        val error = state.newPasswordError

        // Then
        assertNull(error)
    }

    // Tests for confirmPasswordError

    @Test
    fun confirmPasswordError_whenEmpty_thenIsNull() {
        // Given
        val state = ChangePasswordUiState(
            newPassword = "123456",
            confirmPassword = ""
        )

        // When
        val error = state.confirmPasswordError

        // Then
        assertNull(error)
    }

    @Test
    fun confirmPasswordError_whenNotMatch_thenIsPasswordNotMatch() {
        // Given
        val state = ChangePasswordUiState(
            newPassword = "123456",
            confirmPassword = "654321"
        )

        // When
        val error = state.confirmPasswordError

        // Then
        assertEquals(R.string.password_not_match, error)
    }

    @Test
    fun confirmPasswordError_whenMatch_thenIsNull() {
        // Given
        val state = ChangePasswordUiState(
            newPassword = "123456",
            confirmPassword = "123456"
        )

        // When
        val error = state.confirmPasswordError

        // Then
        assertNull(error)
    }

    @Test
    fun confirmPasswordError_whenBothEmpty_thenIsNull() {
        // Given
        val state = ChangePasswordUiState(
            newPassword = "",
            confirmPassword = ""
        )

        // When
        val error = state.confirmPasswordError

        // Then
        assertNull(error)
    }
}
