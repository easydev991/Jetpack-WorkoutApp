package com.swparks.model

import com.swparks.ui.model.LoginCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit тесты для LoginCredentials.
 * Тестируются все методы валидации учетных данных.
 */
class LoginCredentialsTest {
    private val defaultMinPasswordSize = 6

    @Test
    fun initializationWithDefaultValues() {
        // When
        val credentials = LoginCredentials()

        // Then
        assertEquals("", credentials.login)
        assertEquals("", credentials.password)
        assertEquals(defaultMinPasswordSize, credentials.minPasswordSize)
    }

    @Test
    fun initializationWithCustomParameters() {
        // When
        val credentials =
            LoginCredentials(
                login = "test@mail.com",
                password = "qwerty",
                minPasswordSize = 5
            )

        // Then
        assertEquals("test@mail.com", credentials.login)
        assertEquals("qwerty", credentials.password)
        assertEquals(5, credentials.minPasswordSize)
    }

    // MARK: - isReady

    @Test
    fun isReady_AllFieldsEmpty() {
        // Given
        val credentials = LoginCredentials()

        // Then
        assertFalse(credentials.isReady)
    }

    @Test
    fun isReady_LoginNotEmptyPasswordTooShort() {
        // Given
        val credentials = LoginCredentials(login = "user", password = "12345")

        // Then
        assertFalse(credentials.isReady)
    }

    @Test
    fun isReady_ValidLoginAndExactMinPassword() {
        // Given
        val credentials = LoginCredentials(login = "user", password = "123456")

        // Then
        assertTrue(credentials.isReady)
    }

    @Test
    fun isReady_PasswordWithSpacesMeetingMinLength() {
        // Given
        val credentials = LoginCredentials(login = "user", password = "12 345 6")

        // Then
        assertTrue(credentials.isReady)
    }

    @Test
    fun isReady_PasswordWithSpacesBelowMinLength() {
        // Given
        val credentials = LoginCredentials(login = "user", password = "123 45")

        // Then
        assertFalse(credentials.isReady)
    }

    @Test
    fun isReady_CustomMinPasswordSizeValidation() {
        // Given
        val credentials1 =
            LoginCredentials(
                login = "user",
                password = "1234",
                minPasswordSize = 4
            )

        // Then
        assertTrue(credentials1.isReady)

        // Given
        val credentials2 =
            LoginCredentials(
                login = "user",
                password = "123",
                minPasswordSize = 4
            )

        // Then
        assertFalse(credentials2.isReady)
    }

    @Test
    fun isReady_PasswordWithOnlyWhitespace() {
        // Given
        val credentials = LoginCredentials(login = "user", password = "      ")

        // Then
        assertFalse(credentials.isReady)
    }

    // MARK: - canRestorePassword

    @Test
    fun canRestorePassword_EmptyLogin() {
        // Given
        val credentials = LoginCredentials(login = "")

        // Then
        assertFalse(credentials.canRestorePassword)
    }

    @Test
    fun canRestorePassword_NonEmptyLogin() {
        // Given - логин с пробелом
        val credentials = LoginCredentials(login = " ")

        // Then
        assertTrue(credentials.canRestorePassword)

        // Given - логин с email
        val credentials2 = LoginCredentials(login = "user@mail.com")

        // Then
        assertTrue(credentials2.canRestorePassword)
    }

    // MARK: - canLogIn

    @Test
    fun canLogIn_AllConditionsMet() {
        // Given
        val credentials = LoginCredentials(login = "user", password = "123456")

        // Then
        assertTrue(credentials.canLogIn(isError = false))
    }

    @Test
    fun canLogIn_WhenNotReady() {
        // Given
        val credentials = LoginCredentials(login = "user", password = "123")

        // Then
        assertFalse(credentials.canLogIn(isError = false))
    }

    @Test
    fun canLogIn_WithError() {
        // Given
        val credentials = LoginCredentials(login = "user", password = "123456")

        // Then
        assertFalse(credentials.canLogIn(isError = true))
    }
}
