package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository
import com.swparks.domain.exception.ServerException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit тесты для ResetPasswordUseCase.
 */
class ResetPasswordUseCaseTest {
    private lateinit var swRepository: SWRepository
    private lateinit var resetPasswordUseCase: ResetPasswordUseCase

    @Before
    fun setup() {
        swRepository = mockk(relaxed = true)
        resetPasswordUseCase = ResetPasswordUseCase(swRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenApiReturnsSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            val login = "user@mail.com"
            coEvery { swRepository.resetPassword(login) } returns Result.success(Unit)

            // When
            val result = resetPasswordUseCase(login)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { swRepository.resetPassword(login) }
        }

    @Test
    fun invoke_whenApiThrowsIOException_thenReturnsFailure() =
        runTest {
            // Given
            val login = "user@mail.com"
            val ioException = IOException("Нет соединения с интернетом")
            coEvery { swRepository.resetPassword(login) } returns Result.failure(ioException)

            // When
            val result = resetPasswordUseCase(login)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 1) { swRepository.resetPassword(login) }
        }

    @Test
    fun invoke_whenApiReturnsServerException_thenReturnsFailure() =
        runTest {
            // Given
            val login = "user@mail.com"
            val serverException = ServerException("Пользователь не найден", null)
            coEvery { swRepository.resetPassword(login) } returns Result.failure(serverException)

            // When
            val result = resetPasswordUseCase(login)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 1) { swRepository.resetPassword(login) }
        }
}
