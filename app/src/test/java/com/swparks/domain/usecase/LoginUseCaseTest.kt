package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.SecureTokenRepository
import com.swparks.data.TokenEncoder
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.LoginSuccess
import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.LoginCredentials
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

/** Unit тесты для LoginUseCase */
class LoginUseCaseTest {

    private lateinit var tokenEncoder: TokenEncoder
    private lateinit var secureTokenRepository: SecureTokenRepository
    private lateinit var swRepository: SWRepository
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var loginUseCase: LoginUseCase

    private val testCredentials =
        LoginCredentials(login = "user@test.com", password = "password123")
    private val testUserId = 12345L
    private val testToken =
        "dXNlckB0ZXN0LmNvbTpwYXNzd29yZDEyMw==" // Base64 "user@test.com:password123"

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        tokenEncoder = mockk(relaxed = true)
        secureTokenRepository = mockk(relaxed = true)
        swRepository = mockk()
        preferencesRepository = mockk(relaxed = true)
        loginUseCase =
            LoginUseCase(tokenEncoder, secureTokenRepository, swRepository, preferencesRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenValidCredentials_thenSavesTokenAndCallsLogin() = runTest {
        // Given
        coEvery {
            swRepository.login(any())
        } returns Result.success(LoginSuccess(testUserId))

        coEvery { tokenEncoder.encode(testCredentials) } returns testToken

        // When
        val result = loginUseCase(testCredentials)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUserId, result.getOrNull()?.userId)
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(testToken) }
        coVerify(exactly = 1) { swRepository.login(testToken) }
        coVerify(exactly = 1) { tokenEncoder.encode(testCredentials) }
        coVerify(exactly = 1) { preferencesRepository.saveCurrentUserId(testUserId) }
    }

    @Test
    fun invoke_whenEmptyCredentials_thenSavesEmptyToken() = runTest {
        // Given
        val emptyCredentials = LoginCredentials(login = "", password = "")
        coEvery {
            swRepository.login(any())
        } returns Result.success(LoginSuccess(testUserId))

        coEvery { tokenEncoder.encode(emptyCredentials) } returns null

        // When
        val result = loginUseCase(emptyCredentials)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(null) }
        coVerify(exactly = 1) { swRepository.login(null) }
        coVerify(exactly = 1) { tokenEncoder.encode(emptyCredentials) }
        coVerify(exactly = 1) { preferencesRepository.saveCurrentUserId(testUserId) }
    }

    @Test
    fun invoke_whenCredentialsIsNullToken_thenSavesNullToken() = runTest {
        // Given
        val nullTokenCredentials = LoginCredentials(login = "test", password = " ")
        coEvery {
            swRepository.login(any())
        } returns Result.success(LoginSuccess(testUserId))

        coEvery { tokenEncoder.encode(nullTokenCredentials) } returns null

        // When
        val result = loginUseCase(nullTokenCredentials)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(null) }
        coVerify(exactly = 1) { swRepository.login(null) }
        coVerify(exactly = 1) { tokenEncoder.encode(nullTokenCredentials) }
        coVerify(exactly = 1) { preferencesRepository.saveCurrentUserId(testUserId) }
    }
}
