package com.swparks.ui.viewmodel

import com.swparks.data.model.LoginSuccess
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.ILogoutUseCase
import com.swparks.ui.model.LoginCredentials
import com.swparks.util.AppError
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit тесты для AuthViewModel */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var loginUseCase: ILoginUseCase
    private lateinit var logoutUseCase: ILogoutUseCase
    private lateinit var userNotifier: UserNotifier
    private lateinit var authViewModel: AuthViewModel

    private val testCredentials =
        LoginCredentials(login = "user@test.com", password = "password123")
    private val testLoginSuccess = LoginSuccess(userId = 1L)

    @Before
    fun setup() {
        loginUseCase = mockk(relaxed = true)
        logoutUseCase = mockk(relaxed = true)
        val mockErrorFlow = MutableSharedFlow<AppError>()
        userNotifier = mockk(relaxed = true) {
            every { errorFlow } returns mockErrorFlow
        }

        // Настраиваем возвращаемое значение для loginUseCase
        coEvery { loginUseCase(any()) } returns Result.success(testLoginSuccess)

        authViewModel = AuthViewModel(loginUseCase, logoutUseCase, userNotifier)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun login_whenCalled_thenInvokesLoginUseCase() = runTest {
        // When
        authViewModel.login(testCredentials)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { loginUseCase(testCredentials) }
    }

    @Test
    fun login_whenCalledTwice_thenInvokesLoginUseCaseTwice() = runTest {
        // When
        authViewModel.login(testCredentials)
        advanceUntilIdle()

        authViewModel.login(testCredentials)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { loginUseCase(testCredentials) }
    }

    @Test
    fun logout_whenCalled_thenInvokesLogoutUseCase() = runTest {
        // When
        authViewModel.logout()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { logoutUseCase() }
    }

    @Test
    fun clearError_whenCalled_thenSetsIdleState() {
        // When
        authViewModel.clearError()

        // Then
        val state = authViewModel.uiState.value
        assertTrue(state is AuthUiState.Idle)
    }

    @Test
    fun login_whenUseCaseFails_thenCallsUserNotifier() = runTest {
        // Given
        val testException = RuntimeException("Login failed")
        coEvery { loginUseCase(any()) } returns Result.failure(testException)

        // When
        authViewModel.login(testCredentials)
        advanceUntilIdle()

        // Then - проверяем, что userNotifier.handleError был вызван
        verify {
            userNotifier.handleError(
                match<AppError> { error ->
                    error is AppError.Network &&
                        error.message.contains("Не удалось войти")
                }
            )
        }
    }
}
