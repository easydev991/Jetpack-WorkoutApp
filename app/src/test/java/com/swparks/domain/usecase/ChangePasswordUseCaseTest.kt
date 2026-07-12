package com.swparks.domain.usecase

import com.swparks.data.SecureTokenRepository
import com.swparks.data.TokenEncoder
import com.swparks.data.model.User
import com.swparks.data.repository.AuthRepository
import com.swparks.domain.exception.ServerException
import com.swparks.ui.model.LoginCredentials
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ChangePasswordUseCaseTest {
    private lateinit var authRepository: AuthRepository
    private lateinit var secureTokenRepository: SecureTokenRepository
    private lateinit var tokenEncoder: TokenEncoder
    private lateinit var changePasswordUseCase: ChangePasswordUseCase

    @Before
    fun setup() {
        authRepository = mockk(relaxed = true)
        secureTokenRepository = mockk(relaxed = true)
        tokenEncoder = mockk(relaxed = true)
        changePasswordUseCase =
            ChangePasswordUseCase(
                authRepository,
                secureTokenRepository,
                tokenEncoder
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenApiReturnsSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            val currentPassword = "oldPassword123"
            val newPassword = "newPassword456"
            val user = User(id = 1, name = "testuser", image = null)
            val newToken = "newEncodedToken"

            coEvery {
                authRepository.changePassword(
                    currentPassword,
                    newPassword
                )
            } returns Result.success(Unit)
            every { authRepository.getCurrentUserFlow() } returns flowOf(user)
            every {
                tokenEncoder.encode(
                    LoginCredentials(
                        "testuser",
                        newPassword
                    )
                )
            } returns newToken

            // When
            val result = changePasswordUseCase(currentPassword, newPassword)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { authRepository.changePassword(currentPassword, newPassword) }
        }

    @Test
    fun invoke_whenApiThrowsIOException_thenReturnsFailure() =
        runTest {
            // Given
            val currentPassword = "oldPassword123"
            val newPassword = "newPassword456"
            val ioException = IOException("Нет соединения с интернетом")
            coEvery {
                authRepository.changePassword(
                    currentPassword,
                    newPassword
                )
            } returns Result.failure(ioException)

            // When
            val result = changePasswordUseCase(currentPassword, newPassword)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 1) { authRepository.changePassword(currentPassword, newPassword) }
        }

    @Test
    fun invoke_whenApiReturnsServerException_thenReturnsFailure() =
        runTest {
            // Given
            val currentPassword = "oldPassword123"
            val newPassword = "newPassword456"
            val serverException = ServerException("Неверный текущий пароль", null)
            coEvery {
                authRepository.changePassword(
                    currentPassword,
                    newPassword
                )
            } returns Result.failure(serverException)

            // When
            val result = changePasswordUseCase(currentPassword, newPassword)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 1) { authRepository.changePassword(currentPassword, newPassword) }
        }

    @Test
    fun invoke_whenApiReturns401Error_thenReturnsFailure() =
        runTest {
            // Given
            val currentPassword = "oldPassword123"
            val newPassword = "newPassword456"
            val serverException = ServerException("Ошибка авторизации", null)
            coEvery {
                authRepository.changePassword(
                    currentPassword,
                    newPassword
                )
            } returns Result.failure(serverException)

            // When
            val result = changePasswordUseCase(currentPassword, newPassword)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 1) { authRepository.changePassword(currentPassword, newPassword) }
        }

    // ==================== Тесты обновления токена ====================

    @Test
    fun invoke_whenSuccess_thenUpdatesTokenWithNewPassword() =
        runTest {
            // Given
            val currentPassword = "oldPassword123"
            val newPassword = "newPassword456"
            val user = User(id = 1, name = "testuser", image = null)
            val newToken = "newEncodedToken"

            coEvery {
                authRepository.changePassword(
                    currentPassword,
                    newPassword
                )
            } returns Result.success(Unit)
            every { authRepository.getCurrentUserFlow() } returns flowOf(user)
            every {
                tokenEncoder.encode(
                    LoginCredentials(
                        "testuser",
                        newPassword
                    )
                )
            } returns newToken

            // When
            val result = changePasswordUseCase(currentPassword, newPassword)

            // Then
            assertTrue(result.isSuccess)
            verify(exactly = 1) { tokenEncoder.encode(LoginCredentials("testuser", newPassword)) }
            coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(newToken) }
        }

    @Test
    fun invoke_whenSuccessButUserNotFound_thenDoesNotUpdateToken() =
        runTest {
            // Given
            val currentPassword = "oldPassword123"
            val newPassword = "newPassword456"

            coEvery {
                authRepository.changePassword(
                    currentPassword,
                    newPassword
                )
            } returns Result.success(Unit)
            every { authRepository.getCurrentUserFlow() } returns flowOf(null)

            // When
            val result = changePasswordUseCase(currentPassword, newPassword)

            // Then
            assertTrue(result.isSuccess)
            verify(exactly = 0) { tokenEncoder.encode(any()) }
            coVerify(exactly = 0) { secureTokenRepository.saveAuthToken(any()) }
        }

    @Test
    fun invoke_whenSuccessButTokenEncoderReturnsNull_thenDoesNotSaveToken() =
        runTest {
            // Given
            val currentPassword = "oldPassword123"
            val newPassword = "newPassword456"
            val user = User(id = 1, name = "testuser", image = null)

            coEvery {
                authRepository.changePassword(
                    currentPassword,
                    newPassword
                )
            } returns Result.success(Unit)
            every { authRepository.getCurrentUserFlow() } returns flowOf(user)
            every { tokenEncoder.encode(any<LoginCredentials>()) } returns null

            // When
            val result = changePasswordUseCase(currentPassword, newPassword)

            // Then
            assertTrue(result.isSuccess)
            verify(exactly = 1) { tokenEncoder.encode(LoginCredentials("testuser", newPassword)) }
            coVerify(exactly = 0) { secureTokenRepository.saveAuthToken(any()) }
        }

    @Test
    fun invoke_whenFailure_thenDoesNotUpdateToken() =
        runTest {
            // Given
            val currentPassword = "oldPassword123"
            val newPassword = "newPassword456"
            val error = IOException("Network error")

            coEvery {
                authRepository.changePassword(
                    currentPassword,
                    newPassword
                )
            } returns Result.failure(error)

            // When
            val result = changePasswordUseCase(currentPassword, newPassword)

            // Then
            assertTrue(result.isFailure)
            assertEquals(error, result.exceptionOrNull())
            verify(exactly = 0) { tokenEncoder.encode(any()) }
            coVerify(exactly = 0) { secureTokenRepository.saveAuthToken(any()) }
        }
}
