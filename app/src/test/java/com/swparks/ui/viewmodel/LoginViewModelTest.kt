package com.swparks.ui.viewmodel


import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.SWRepository
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.IResetPasswordUseCase
import com.swparks.model.LoginCredentials
import com.swparks.model.LoginSuccess
import com.swparks.model.SocialUpdates
import com.swparks.model.User
import com.swparks.ui.state.LoginUiState
import com.swparks.util.Logger
import com.swparks.util.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var secureTokenRepository: SecureTokenRepository
    private lateinit var swRepository: SWRepository
    private lateinit var loginViewModel: LoginViewModel

    private val testLoginSuccess = LoginSuccess(userId = 123L)
    private val testUser = User(
        id = 123L,
        name = "Test User",
        image = "",
        cityID = null,
        countryID = null,
        birthDate = null,
        email = null,
        fullName = null,
        genderCode = null,
        friendRequestCount = null,
        friendsCount = null,
        parksCount = null,
        addedParks = null,
        journalCount = null,
        lang = "ru"
    )
    private val testLogger: Logger = NoOpLogger()

    @Before
    fun setup() {
        loginUseCase = mockk(relaxed = true)
        resetPasswordUseCase = mockk(relaxed = true)
        secureTokenRepository = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        loginViewModel = LoginViewModel(
            testLogger,
            loginUseCase,
            resetPasswordUseCase,
            secureTokenRepository,
            swRepository
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
        assertTrue(state is LoginUiState.LoginSuccess)
        assertNull(loginViewModel.loginErrorState.value)
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
        assertTrue(state is LoginUiState.ResetSuccess)
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
    fun loginAndLoadUserData_whenSuccessful_thenReturnsSocialUpdates() = runTest {
        // Given
        val credentials = LoginCredentials(login = "user@test.com", password = "password123")
        loginViewModel.onLoginChange("user@test.com")
        loginViewModel.onPasswordChange("password123")

        val testSocialUpdates = SocialUpdates(
            user = testUser,
            friends = emptyList(),
            friendRequests = emptyList(),
            blacklist = emptyList()
        )

        coEvery { loginUseCase(credentials) } returns Result.success(testLoginSuccess)
        coEvery { swRepository.getSocialUpdates(123L) } returns Result.success(testSocialUpdates)

        // When
        val result = loginViewModel.loginAndLoadUserData()

        // Then
        assertTrue(result.isSuccess)
        val socialUpdates = result.getOrThrow()
        assertTrue(socialUpdates.user.id == 123L)
        assertTrue(socialUpdates.user.name == "Test User")
        coVerify(exactly = 1) { loginUseCase(credentials) }
        coVerify(exactly = 1) { swRepository.getSocialUpdates(123L) }
    }

    @Test
    fun loginAndLoadUserData_whenLoginFails_thenReturnsError() = runTest {
        // Given
        val credentials = LoginCredentials(login = "user@test.com", password = "password123")
        loginViewModel.onLoginChange("user@test.com")
        loginViewModel.onPasswordChange("password123")
        val errorMessage = "Неверные учетные данные"
        coEvery { loginUseCase(credentials) } returns Result.failure(Exception(errorMessage))

        // When
        val result = loginViewModel.loginAndLoadUserData()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message == errorMessage)
        coVerify(exactly = 1) { loginUseCase(credentials) }
        coVerify(exactly = 0) { swRepository.getSocialUpdates(any()) }
    }

    @Test
    fun loginAndLoadUserData_whenLoadSocialUpdatesFails_thenReturnsError() = runTest {
        // Given
        val credentials = LoginCredentials(login = "user@test.com", password = "password123")
        loginViewModel.onLoginChange("user@test.com")
        loginViewModel.onPasswordChange("password123")
        val errorMessage = "Ошибка сети"
        coEvery { loginUseCase(credentials) } returns Result.success(testLoginSuccess)
        coEvery { swRepository.getSocialUpdates(123L) } returns Result.failure(
            Exception(errorMessage)
        )

        // When
        val result = loginViewModel.loginAndLoadUserData()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message == errorMessage)
        coVerify(exactly = 1) { loginUseCase(credentials) }
        coVerify(exactly = 1) { swRepository.getSocialUpdates(123L) }
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
