package com.swparks.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginSuccessTest {
    @Test
    fun userId_whenValueIs123_thenReturns123() {
        // Given
        val loginSuccess = LoginSuccess(userId = 123L)

        // When & Then
        assertEquals(123L, loginSuccess.userId)
    }

    @Test
    fun userId_whenValueIs0_thenReturns0() {
        // Given
        val loginSuccess = LoginSuccess(userId = 0L)

        // When & Then
        assertEquals(0L, loginSuccess.userId)
    }

    @Test
    fun userId_whenValueIsNegative_thenReturnsNegative() {
        // Given
        val loginSuccess = LoginSuccess(userId = -1L)

        // When & Then
        assertEquals(-1L, loginSuccess.userId)
    }
}
