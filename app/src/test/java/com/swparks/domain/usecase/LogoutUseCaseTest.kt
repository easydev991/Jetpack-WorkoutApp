package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.AuthRepository
import com.swparks.util.CrashReporter
import com.swparks.util.NoOpCrashReporter
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit тесты для LogoutUseCase */
class LogoutUseCaseTest {
    private lateinit var secureTokenRepository: SecureTokenRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var crashReporter: CrashReporter
    private lateinit var logoutUseCase: LogoutUseCase

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        secureTokenRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        crashReporter = NoOpCrashReporter()
        logoutUseCase = LogoutUseCase(secureTokenRepository, authRepository, crashReporter)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenLogout_thenClearsTokenAndCallsForceLogout() =
        runTest {
            // When
            logoutUseCase()

            // Then
            coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(null) }
            coVerify(exactly = 1) { authRepository.clearUserData() }
            coVerify(exactly = 1) { authRepository.forceLogout() }
        }

    @Test
    fun invoke_whenLogoutThenCompletesSuccessfully() =
        runTest {
            // When
            logoutUseCase()

            // Then - проверка, что метод завершается без исключений
            assertTrue(true)
        }
}
