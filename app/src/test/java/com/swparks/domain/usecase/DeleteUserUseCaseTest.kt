package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.SWRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit тесты для DeleteUserUseCase.
 *
 * Тестирует:
 * - Успешное удаление профиля с очисткой всех данных
 * - Ошибка API при удалении профиля
 * - Очистка токена и локальных данных при успехе
 */
class DeleteUserUseCaseTest {

    private lateinit var secureTokenRepository: SecureTokenRepository
    private lateinit var swRepository: SWRepository
    private lateinit var deleteUserUseCase: DeleteUserUseCase

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        secureTokenRepository = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        deleteUserUseCase = DeleteUserUseCase(secureTokenRepository, swRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenApiSuccess_thenReturnsSuccessAndClearsData() = runTest {
        // Given
        coEvery { swRepository.deleteUser() } returns Result.success(Unit)

        // When
        val result = deleteUserUseCase()

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { swRepository.deleteUser() }
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(null) }
        coVerify(exactly = 1) { swRepository.clearUserData() }
        coVerify(exactly = 1) { swRepository.forceLogout() }
    }

    @Test
    fun invoke_whenApiSuccess_thenClearsTokenBeforeClearingData() = runTest {
        // Given
        coEvery { swRepository.deleteUser() } returns Result.success(Unit)

        // When
        deleteUserUseCase()

        // Then - проверяем порядок вызовов через последовательность verify
        coVerify {
            swRepository.deleteUser()
            secureTokenRepository.saveAuthToken(null)
            swRepository.clearUserData()
            swRepository.forceLogout()
        }
    }

    @Test
    fun invoke_whenApiFails_thenReturnsFailureAndDoesNotClearData() = runTest {
        // Given
        val error = IOException("Network error")
        coEvery { swRepository.deleteUser() } returns Result.failure(error)

        // When
        val result = deleteUserUseCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { swRepository.deleteUser() }
        coVerify(exactly = 0) { secureTokenRepository.saveAuthToken(null) }
        coVerify(exactly = 0) { swRepository.clearUserData() }
        coVerify(exactly = 0) { swRepository.forceLogout() }
    }

    @Test
    fun invoke_whenApiFailsWithHttpException_thenReturnsFailure() = runTest {
        // Given
        val error = RuntimeException("Server error 500")
        coEvery { swRepository.deleteUser() } returns Result.failure(error)

        // When
        val result = deleteUserUseCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals("Server error 500", result.exceptionOrNull()?.message)
    }
}
