package com.swparks.ui.viewmodel

import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.ILogoutUseCase
import com.swparks.model.LoginSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var authViewModel: AuthViewModel

    private val testToken = "test_auth_token_12345"
    private val testLoginSuccess = LoginSuccess(userId = 1L)

    @Before
    fun setup() {
        loginUseCase = mockk(relaxed = true)
        logoutUseCase = mockk(relaxed = true)

        // Настраиваем возвращаемое значение для loginUseCase
        coEvery { loginUseCase(any()) } returns Result.success(testLoginSuccess)

        authViewModel = AuthViewModel(loginUseCase, logoutUseCase)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun login_whenCalled_thenInvokesLoginUseCase() = runTest {
        // When
        authViewModel.login(testToken)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { loginUseCase(testToken) }
    }

    @Test
    fun login_whenCalledTwice_thenInvokesLoginUseCaseTwice() = runTest {
        // When
        authViewModel.login(testToken)
        advanceUntilIdle()

        authViewModel.login(testToken)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { loginUseCase(testToken) }
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
}
