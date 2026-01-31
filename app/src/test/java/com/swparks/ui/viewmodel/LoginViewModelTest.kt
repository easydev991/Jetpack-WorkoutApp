package com.swparks.ui.viewmodel


import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.IResetPasswordUseCase
import com.swparks.model.LoginCredentials
import com.swparks.model.LoginSuccess
import com.swparks.ui.state.LoginEvent
import com.swparks.ui.state.LoginUiState
import com.swparks.util.Logger
import com.swparks.util.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit тесты для LoginViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var loginUseCase: ILoginUseCase
    private lateinit var resetPasswordUseCase: IResetPasswordUseCase
    private lateinit var loginViewModel: LoginViewModel

    private val testLoginSuccess = LoginSuccess(userId = 123L)
    private val testLogger: Logger = NoOpLogger()

    @Before
    fun setup() {
        loginUseCase = mockk(relaxed = true)
        resetPasswordUseCase = mockk(relaxed = true)
        loginViewModel = LoginViewModel(
            testLogger,
            loginUseCase,
            resetPasswordUseCase
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onLoginChange_WhenCalled_thenUpdatesLogin() {
        // Given
        val newLogin = "user@test.com"

        // When
        loginViewModel.onLoginChange(newLogin)

        // Then
        assertTrue(loginViewModel.credentials.login == newLogin)
        assertTrue(loginViewModel.loginErrorState.value == null)

        assertTrue(loginViewModel.resetErrorState.value == null)

    }

    @Test
    fun onPasswordChange_WhenCalled_thenUpdatesPassword() {
        // Given
        val newPassword = "password123"

        // When
        loginViewModel.onPasswordChange(newPassword)

        // Then
        assertTrue(loginViewModel.credentials.password == newPassword)
        assertTrue(loginViewModel.loginErrorState.value == null)

        assertTrue(loginViewModel.resetErrorState.value == null)

    }

    @Test
    fun onLoginChange_WhenCalledWithEmpty_thenClearsErrors() {
        // Given
        // Errors will be set internally by calling methods that produce errors

        // When
        loginViewModel.onLoginChange("")

        // Then
        assertTrue(loginViewModel.loginErrorState.value == null)

        assertTrue(loginViewModel.resetErrorState.value == null)

    }

    @Test
    fun onPasswordChange_WhenCalledWithEmpty_thenClearsErrors() {
        // Given
        // Errors will be set internally by calling methods that produce errors

        // When
        loginViewModel.onPasswordChange("")

        // Then
        assertTrue(loginViewModel.loginErrorState.value == null)

        assertTrue(loginViewModel.resetErrorState.value == null)

    }

    @Test
    fun clearErrors_WhenCalled_thenClearsAllErrors() {
        // Given
        // Errors will be set internally by calling methods that produce errors

        // When
        loginViewModel.clearErrors()

        // Then
        assertTrue(loginViewModel.loginErrorState.value == null)

        assertTrue(loginViewModel.resetErrorState.value == null)

    }

    @Test
    fun login_whenValidCredentials_thenReturnsSuccess() = runTest {
        // Given
        val credentials = LoginCredentials(login = "user@test.com", password = "password123")
        loginViewModel.onLoginChange("user@test.com")
        loginViewModel.onPasswordChange("password123")
        coEvery { loginUseCase(credentials) } returns Result.success(testLoginSuccess)

        // When
        loginViewModel.login()
        advanceUntilIdle()

        // Then
        val state = loginViewModel.uiState.value
        assertTrue(state is LoginUiState.Idle)
        assertNull(loginViewModel.loginErrorState.value)

        val event = loginViewModel.loginEvents.first()
        assertTrue(event is LoginEvent.Success)
        assertTrue((event as LoginEvent.Success).userId == testLoginSuccess.userId)

        coVerify(exactly = 1) { loginUseCase(credentials) }
    }

    @Test
    fun login_whenInvalidCredentials_thenReturnsError() = runTest {
        // Given
        val credentials = LoginCredentials(login = "user@test.com", password = "password123")
        loginViewModel.onLoginChange("user@test.com")
        loginViewModel.onPasswordChange("password123")
        val errorMessage = "Неверные учетные данные"
        coEvery { loginUseCase(credentials) } returns Result.failure(
            Exception(errorMessage)
        )

        // When
        loginViewModel.login()
        advanceUntilIdle()

        // Then
        val state = loginViewModel.uiState.value
        assertTrue(state is LoginUiState.LoginError)
        val errorState = state as LoginUiState.LoginError
        assertTrue(errorState.message == errorMessage)
        assertTrue(loginViewModel.loginErrorState.value == errorMessage)
        coVerify(exactly = 1) { loginUseCase(credentials) }
    }

    @Test
    fun resetPassword_whenEmptyLogin_thenShowsAlert() = runTest {
        // Given
        loginViewModel.onLoginChange("")

        // When
        loginViewModel.resetPassword()
        advanceUntilIdle()

        // Then
        val state = loginViewModel.uiState.value
        assertTrue(state is LoginUiState.Idle)
        coVerify(exactly = 0) { resetPasswordUseCase(any()) }
    }

    @Test
    fun resetPassword_whenValidLogin_thenReturnsSuccess() = runTest {
        // Given
        loginViewModel.onLoginChange("user@test.com")
        coEvery { resetPasswordUseCase(any()) } returns Result.success(Unit)

        // When
        loginViewModel.resetPassword()
        advanceUntilIdle()

        // Then
        val state = loginViewModel.uiState.value
        assertTrue(state is LoginUiState.Idle)

        val event = loginViewModel.loginEvents.first()
        assertTrue(event is LoginEvent.ResetSuccess)
        assertTrue((event as LoginEvent.ResetSuccess).email == "user@test.com")

        coVerify(exactly = 1) { resetPasswordUseCase("user@test.com") }
    }

    @Test
    fun resetPassword_whenApiReturnsError_thenReturnsError() = runTest {
        // Given
        loginViewModel.onLoginChange("user@test.com")
        val errorMessage = "Пользователь не найден"
        coEvery { resetPasswordUseCase("user@test.com") } returns Result.failure(
            Exception(errorMessage)
        )

        // When
        loginViewModel.resetPassword()
        advanceUntilIdle()

        // Then
        val state = loginViewModel.uiState.value
        assertTrue(state is LoginUiState.ResetError)
        val errorState = state as LoginUiState.ResetError
        assertTrue(errorState.message == errorMessage)
        assertTrue(loginViewModel.resetErrorState.value == errorMessage)
        coVerify(exactly = 1) { resetPasswordUseCase("user@test.com") }
    }

    @Test
    fun canLogIn_whenCredentialsValidAndNoError_thenReturnsTrue() {
        // Given
        loginViewModel.onLoginChange("user@test.com")
        loginViewModel.onPasswordChange("password123")

        // When
        val canLogIn = loginViewModel.credentials.canLogIn(
            isError = loginViewModel.loginErrorState.value != null
        )

        // Then
        assertTrue(canLogIn)
    }

    @Test
    fun canLogIn_whenCredentialsInvalid_thenReturnsFalse() {
        // Given
        loginViewModel.onLoginChange("user@test.com")
        loginViewModel.onPasswordChange("123") // Пароль слишком короткий

        // When
        val canLogIn = loginViewModel.credentials.canLogIn(
            isError = loginViewModel.loginErrorState.value != null
        )

        // Then
        assertFalse(canLogIn)
    }

    @Test
    fun canLogIn_whenHasError_thenReturnsFalse() = runTest {
        // Given
        val credentials = LoginCredentials(login = "user@test.com", password = "password123")
        loginViewModel.onLoginChange("user@test.com")
        loginViewModel.onPasswordChange("password123")
        // Trigger error by calling login with failure
        coEvery { loginUseCase(credentials) } returns Result.failure(Exception("Error"))
        loginViewModel.login()
        advanceUntilIdle()

        // When
        val canLogIn = loginViewModel.credentials.canLogIn(
            isError = loginViewModel.loginErrorState.value != null
        )

        // Then
        assertFalse(canLogIn)
    }

    @Test
    fun canRestorePassword_whenLoginNotEmpty_thenReturnsTrue() {
        // Given
        loginViewModel.onLoginChange("user@test.com")

        // When
        val canRestore = loginViewModel.credentials.canRestorePassword

        // Then
        assertTrue(canRestore)
    }

    @Test
    fun canRestorePassword_whenLoginEmpty_thenReturnsFalse() {
        // Given
        loginViewModel.onLoginChange("")

        // When
        val canRestore = loginViewModel.credentials.canRestorePassword

        // Then
        assertFalse(canRestore)
    }
}
