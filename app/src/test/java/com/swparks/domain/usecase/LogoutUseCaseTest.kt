package com.swparks.domain.usecase

import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.SWRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit тесты для LogoutUseCase */
class LogoutUseCaseTest {

    private lateinit var secureTokenRepository: SecureTokenRepository
    private lateinit var swRepository: SWRepository
    private lateinit var logoutUseCase: LogoutUseCase

    @Before
    fun setup() {
        secureTokenRepository = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        logoutUseCase = LogoutUseCase(secureTokenRepository, swRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenLogout_thenClearsTokenAndCallsForceLogout() = runTest {
        // When
        logoutUseCase()

        // Then
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(null) }
        coVerify(exactly = 1) { swRepository.forceLogout() }
    }

    @Test
    fun invoke_whenLogoutThenCompletesSuccessfully() = runTest {
        // When
        logoutUseCase()

        // Then - проверка, что метод завершается без исключений
        assertTrue(true)
    }
}
