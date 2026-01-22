package com.swparks.domain.usecase

import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.SWRepository
import com.swparks.model.LoginSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit тесты для LoginUseCase */
class LoginUseCaseTest {

    private lateinit var secureTokenRepository: SecureTokenRepository
    private lateinit var swRepository: SWRepository
    private lateinit var loginUseCase: LoginUseCase

    private val testToken = "test_auth_token_12345"
    private val testUserId = 12345L

    @Before
    fun setup() {
        secureTokenRepository = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        loginUseCase = LoginUseCase(secureTokenRepository, swRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenValidToken_thenSavesTokenAndCallsLogin() = runTest {
        // Given
        coEvery { swRepository.login(any()) } returns Result.success(LoginSuccess(testUserId))

        // When
        val result = loginUseCase(testToken)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUserId, result.getOrNull()?.userId)
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(testToken) }
        coVerify(exactly = 1) { swRepository.login(null) }
    }

    @Test
    fun invoke_whenEmptyToken_thenSavesEmptyToken() = runTest {
        // Given
        coEvery { swRepository.login(any()) } returns Result.success(LoginSuccess(testUserId))

        // When
        val result = loginUseCase("")

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken("") }
    }

    @Test
    fun invoke_whenTokenIsNull_thenSavesNullToken() = runTest {
        // Given
        coEvery { swRepository.login(any()) } returns Result.success(LoginSuccess(testUserId))

        // When
        val result = loginUseCase(null)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(null) }
    }
}
