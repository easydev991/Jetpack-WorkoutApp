package com.swparks.data

import com.swparks.model.LoginCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Модульные тесты для TokenEncoder.
 */
class TokenEncoderTest {

    private val tokenEncoder = TokenEncoder()

    @Test
    fun encode_whenValidCredentials_thenReturnsBase64Token() {
        // Given
        val credentials = LoginCredentials(login = "test@example.com", password = "password123")

        // When
        val result = tokenEncoder.encode(credentials)

        // Then
        assertNotNull(result)
        // Base64-encoded "test@example.com:password123" should be "dGVzdEBleGFtcGxlLmNvbTpwYXNzd29yZDEyMw=="
        assertEquals("dGVzdEBleGFtcGxlLmNvbTpwYXNzd29yZDEyMw==", result)
    }

    @Test
    fun encode_whenEmptyLogin_thenReturnsNull() {
        // Given
        val credentials = LoginCredentials(login = "", password = "password123")

        // When
        val result = tokenEncoder.encode(credentials)

        // Then
        assertNull(result)
    }

    @Test
    fun encode_whenEmptyPassword_thenReturnsNull() {
        // Given
        val credentials = LoginCredentials(login = "test@example.com", password = "")

        // When
        val result = tokenEncoder.encode(credentials)

        // Then
        assertNull(result)
    }

    @Test
    fun encode_whenWhitespaceOnlyCredentials_thenReturnsNull() {
        // Given
        val credentials = LoginCredentials(login = "   ", password = "   ")

        // When
        val result = tokenEncoder.encode(credentials)

        // Then
        assertNull(result)
    }

    @Test
    fun encode_whenTrimmedCredentials_thenReturnsCorrectToken() {
        // Given
        val credentials =
            LoginCredentials(login = "  test@example.com  ", password = "  password123  ")

        // When
        val result = tokenEncoder.encode(credentials)

        // Then
        assertNotNull(result)
        // Base64-encoded "test@example.com:password123" (after trimming)
        assertEquals("dGVzdEBleGFtcGxlLmNvbTpwYXNzd29yZDEyMw==", result)
    }

    @Test
    fun encode_whenLoginWithSpacesOnly_thenReturnsNull() {
        // Given
        val credentials = LoginCredentials(login = "  ", password = "password123")

        // When
        val result = tokenEncoder.encode(credentials)

        // Then
        assertNull(result)
    }

    @Test
    fun encode_whenPasswordWithSpacesOnly_thenReturnsNull() {
        // Given
        val credentials = LoginCredentials(login = "test@example.com", password = "  ")

        // When
        val result = tokenEncoder.encode(credentials)

        // Then
        assertNull(result)
    }
}
