package com.swparks.ui.viewmodel

import com.swparks.R
import com.swparks.data.SecureTokenRepository
import com.swparks.data.TokenEncoder
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.LoginSuccess
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.CountriesRepository
import com.swparks.ui.model.LoginCredentials
import com.swparks.ui.model.RegistrationRequest
import com.swparks.ui.state.RegisterEvent
import com.swparks.ui.state.RegisterUiState
import com.swparks.util.Logger
import com.swparks.util.NoOpLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Unit тесты для RegisterViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var swRepository: SWRepository
    private lateinit var secureTokenRepository: SecureTokenRepository
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var tokenEncoder: TokenEncoder
    private lateinit var countriesRepository: CountriesRepository
    private lateinit var resourcesProvider: ResourcesProvider
    private lateinit var registerViewModel: RegisterViewModel

    private val testLoginSuccess = LoginSuccess(userId = 123L)
    private val testLogger: Logger = NoOpLogger()
    private val testCountry = Country(id = "1", name = "Россия", cities = emptyList())
    private val testCity = City(id = "1", name = "Москва", lat = "55.7558", lon = "37.6173")

    @Before
    fun setup() {
        swRepository = mockk(relaxed = true)
        secureTokenRepository = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        tokenEncoder = mockk(relaxed = true)
        countriesRepository = mockk(relaxed = true)
        resourcesProvider = mockk(relaxed = true)

        every { tokenEncoder.encode(any<LoginCredentials>()) } returns "test-token"
        coEvery { countriesRepository.getCountryById(any()) } returns testCountry
        coEvery { countriesRepository.getCitiesByCountry(any()) } returns listOf(testCity)
        coEvery { countriesRepository.getAllCities() } returns listOf(testCity)
        coEvery { countriesRepository.getCountryForCity(any()) } returns testCountry
        every { countriesRepository.getCountriesFlow() } returns flowOf(listOf(testCountry))

        // Mock resources
        every { resourcesProvider.getString(R.string.email_invalid) } returns "Введите корректный email"
        every { resourcesProvider.getString(R.string.password_short) } returns "Пароль слишком короткий"
        every { resourcesProvider.getString(R.string.login_empty) } returns "Введите логин"

        registerViewModel = RegisterViewModel(
            logger = testLogger,
            swRepository = swRepository,
            secureTokenRepository = secureTokenRepository,
            preferencesRepository = preferencesRepository,
            tokenEncoder = tokenEncoder,
            countriesRepository = countriesRepository,
            resources = resourcesProvider
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onLoginChange_WhenCalled_thenUpdatesLogin() {
        // Given
        val newLogin = "testuser"

        // When
        registerViewModel.onLoginChange(newLogin)

        // Then
        assertEquals(newLogin, registerViewModel.form.value.login)
        assertNull(registerViewModel.loginError.value)
    }

    @Test
    fun onEmailChange_WhenCalled_thenUpdatesEmail() {
        // Given
        val newEmail = "test@example.com"

        // When
        registerViewModel.onEmailChange(newEmail)

        // Then
        assertEquals(newEmail, registerViewModel.form.value.email)
        assertNull(registerViewModel.emailFormatError.value)
    }

    @Test
    fun onEmailChange_WhenInvalidEmail_thenShowsError() {
        // Given
        val invalidEmail = "invalid-email"

        // When
        registerViewModel.onEmailChange(invalidEmail)

        // Then
        assertEquals(invalidEmail, registerViewModel.form.value.email)
        assertEquals("Введите корректный email", registerViewModel.emailFormatError.value)
    }

    @Test
    fun onEmailChange_WhenEmpty_thenClearsError() {
        // Given - first set invalid email to get error
        registerViewModel.onEmailChange("invalid")
        assertEquals("Введите корректный email", registerViewModel.emailFormatError.value)

        // When - clear email
        registerViewModel.onEmailChange("")

        // Then
        assertEquals("", registerViewModel.form.value.email)
        assertNull(registerViewModel.emailFormatError.value)
    }

    @Test
    fun onPasswordChange_WhenCalled_thenUpdatesPassword() {
        // Given
        val newPassword = "password123"

        // When
        registerViewModel.onPasswordChange(newPassword)

        // Then
        assertEquals(newPassword, registerViewModel.form.value.password)
        assertNull(registerViewModel.passwordLengthError.value)
    }

    @Test
    fun onPasswordChange_WhenShortPassword_thenShowsError() {
        // Given
        val shortPassword = "123"

        // When
        registerViewModel.onPasswordChange(shortPassword)

        // Then
        assertEquals(shortPassword, registerViewModel.form.value.password)
        assertEquals("Пароль слишком короткий", registerViewModel.passwordLengthError.value)
    }

    @Test
    fun onPasswordChange_WhenEmpty_thenClearsError() {
        // Given - first set short password to get error
        registerViewModel.onPasswordChange("123")
        assertEquals("Пароль слишком короткий", registerViewModel.passwordLengthError.value)

        // When - clear password
        registerViewModel.onPasswordChange("")

        // Then
        assertEquals("", registerViewModel.form.value.password)
        assertNull(registerViewModel.passwordLengthError.value)
    }

    @Test
    fun onPasswordChange_WhenValidLength_thenClearsError() {
        // Given - first set short password to get error
        registerViewModel.onPasswordChange("123")
        assertEquals("Пароль слишком короткий", registerViewModel.passwordLengthError.value)

        // When - set valid password
        registerViewModel.onPasswordChange("123456")

        // Then
        assertEquals("123456", registerViewModel.form.value.password)
        assertNull(registerViewModel.passwordLengthError.value)
    }

    @Test
    fun onFullNameChange_WhenCalled_thenUpdatesFullName() {
        // Given
        val newFullName = "Ivan Ivanov"

        // When
        registerViewModel.onFullNameChange(newFullName)

        // Then
        assertEquals(newFullName, registerViewModel.form.value.fullName)
    }

    @Test
    fun onGenderChange_WhenCalled_thenUpdatesGenderCode() {
        // Given
        val genderCode = 0 // MALE

        // When
        registerViewModel.onGenderChange(genderCode)

        // Then
        assertEquals(genderCode, registerViewModel.form.value.genderCode)
    }

    @Test
    fun onBirthDateChange_WhenCalled_thenUpdatesBirthDate() {
        // Given
        val birthDate = LocalDate.of(2000, 1, 15)

        // When
        registerViewModel.onBirthDateChange(birthDate)

        // Then
        assertEquals(birthDate, registerViewModel.form.value.birthDate)
        assertNull(registerViewModel.birthDateError.value)
    }

    @Test
    fun onCountrySelectedByName_WhenCalled_thenUpdatesCountryAndClearsCity() = runTest {
        // Given - first select city
        registerViewModel.onCountrySelectedByName("Россия")
        advanceUntilIdle()
        registerViewModel.onCitySelectedByName("Москва")
        advanceUntilIdle()

        // When - select different country
        registerViewModel.onCountrySelectedByName("Россия")
        advanceUntilIdle()

        // Then
        assertEquals("1", registerViewModel.form.value.countryId)
        assertNull(registerViewModel.form.value.cityId)
        assertEquals(testCountry, registerViewModel.selectedCountry.value)
    }

    @Test
    fun onCitySelectedByName_WhenCalled_thenUpdatesCity() = runTest {
        // Given
        registerViewModel.onCountrySelectedByName("Россия")
        advanceUntilIdle()

        // When
        registerViewModel.onCitySelectedByName("Москва")
        advanceUntilIdle()

        // Then
        assertEquals("1", registerViewModel.form.value.cityId)
        assertEquals(testCity, registerViewModel.selectedCity.value)
    }

    @Test
    fun onPolicyAcceptedChange_WhenCalled_thenUpdatesPolicyAccepted() {
        // Given
        val accepted = true

        // When
        registerViewModel.onPolicyAcceptedChange(accepted)

        // Then
        assertTrue(registerViewModel.form.value.isPolicyAccepted)
    }

    @Test
    fun clearErrors_WhenCalled_thenClearsAllErrors() {
        // Given - set some errors via validation
        registerViewModel.onLoginChange("")
        registerViewModel.register() // triggers validation
        registerViewModel.onEmailChange("invalid-email")
        registerViewModel.onPasswordChange("123")

        // When
        registerViewModel.clearErrors()

        // Then
        assertNull(registerViewModel.loginError.value)
        assertNull(registerViewModel.emailFormatError.value)
        assertNull(registerViewModel.passwordLengthError.value)
        assertNull(registerViewModel.birthDateError.value)
    }

    @Test
    fun formIsValid_WhenAllFieldsValid_thenReturnsTrue() = runTest {
        // Given - fill all required fields
        registerViewModel.onLoginChange("testuser")
        registerViewModel.onEmailChange("test@example.com")
        registerViewModel.onPasswordChange("password123")
        registerViewModel.onFullNameChange("Ivan Ivanov")
        registerViewModel.onGenderChange(0)
        registerViewModel.onBirthDateChange(LocalDate.of(2000, 1, 15))
        registerViewModel.onCountrySelectedByName("Россия")
        advanceUntilIdle()
        registerViewModel.onCitySelectedByName("Москва")
        advanceUntilIdle()
        registerViewModel.onPolicyAcceptedChange(true)

        // When
        val isValid = registerViewModel.form.value.isValid

        // Then
        assertTrue(isValid)
    }

    @Test
    fun formIsValid_WhenMissingLogin_thenReturnsFalse() = runTest {
        // Given - don't fill login
        registerViewModel.onEmailChange("test@example.com")
        registerViewModel.onPasswordChange("password123")
        registerViewModel.onFullNameChange("Ivan Ivanov")
        registerViewModel.onGenderChange(0)
        registerViewModel.onBirthDateChange(LocalDate.of(2000, 1, 15))
        registerViewModel.onCountrySelectedByName("Россия")
        advanceUntilIdle()
        registerViewModel.onCitySelectedByName("Москва")
        advanceUntilIdle()
        registerViewModel.onPolicyAcceptedChange(true)

        // When
        val isValid = registerViewModel.form.value.isValid

        // Then
        assertFalse(isValid)
    }

    @Test
    fun formIsValid_WhenInvalidEmail_thenReturnsFalse() = runTest {
        // Given - invalid email
        registerViewModel.onLoginChange("testuser")
        registerViewModel.onEmailChange("invalid-email")
        registerViewModel.onPasswordChange("password123")
        registerViewModel.onFullNameChange("Ivan Ivanov")
        registerViewModel.onGenderChange(0)
        registerViewModel.onBirthDateChange(LocalDate.of(2000, 1, 15))
        registerViewModel.onCountrySelectedByName("Россия")
        advanceUntilIdle()
        registerViewModel.onCitySelectedByName("Москва")
        advanceUntilIdle()
        registerViewModel.onPolicyAcceptedChange(true)

        // When
        val isValid = registerViewModel.form.value.isValid

        // Then
        assertFalse(isValid)
    }

    @Test
    fun formIsValid_WhenShortPassword_thenReturnsFalse() = runTest {
        // Given - short password
        registerViewModel.onLoginChange("testuser")
        registerViewModel.onEmailChange("test@example.com")
        registerViewModel.onPasswordChange("123")
        registerViewModel.onFullNameChange("Ivan Ivanov")
        registerViewModel.onGenderChange(0)
        registerViewModel.onBirthDateChange(LocalDate.of(2000, 1, 15))
        registerViewModel.onCountrySelectedByName("Россия")
        advanceUntilIdle()
        registerViewModel.onCitySelectedByName("Москва")
        advanceUntilIdle()
        registerViewModel.onPolicyAcceptedChange(true)

        // When
        val isValid = registerViewModel.form.value.isValid

        // Then
        assertFalse(isValid)
    }

    @Test
    fun formIsValid_WhenUnder13YearsOld_thenReturnsFalse() = runTest {
        // Given - birth date less than 13 years ago
        val youngDate = LocalDate.now().minusYears(10)
        registerViewModel.onLoginChange("testuser")
        registerViewModel.onEmailChange("test@example.com")
        registerViewModel.onPasswordChange("password123")
        registerViewModel.onFullNameChange("Ivan Ivanov")
        registerViewModel.onGenderChange(0)
        registerViewModel.onBirthDateChange(youngDate)
        registerViewModel.onCountrySelectedByName("Россия")
        advanceUntilIdle()
        registerViewModel.onCitySelectedByName("Москва")
        advanceUntilIdle()
        registerViewModel.onPolicyAcceptedChange(true)

        // When
        val isValid = registerViewModel.form.value.isValid

        // Then
        assertFalse(isValid)
    }

    @Test
    fun formIsValid_WhenPolicyNotAccepted_thenReturnsFalse() = runTest {
        // Given - policy not accepted
        registerViewModel.onLoginChange("testuser")
        registerViewModel.onEmailChange("test@example.com")
        registerViewModel.onPasswordChange("password123")
        registerViewModel.onFullNameChange("Ivan Ivanov")
        registerViewModel.onGenderChange(0)
        registerViewModel.onBirthDateChange(LocalDate.of(2000, 1, 15))
        registerViewModel.onCountrySelectedByName("Россия")
        advanceUntilIdle()
        registerViewModel.onCitySelectedByName("Москва")
        advanceUntilIdle()
        registerViewModel.onPolicyAcceptedChange(false)

        // When
        val isValid = registerViewModel.form.value.isValid

        // Then
        assertFalse(isValid)
    }

    @Test
    fun register_WhenValidForm_thenReturnsSuccess() = runTest {
        // Given
        fillValidForm()
        coEvery { swRepository.register(any<RegistrationRequest>()) } returns Result.success(
            testLoginSuccess
        )

        // When
        registerViewModel.register()
        advanceUntilIdle()

        // Then
        val state = registerViewModel.uiState.value
        assertTrue(state is RegisterUiState.Idle)

        val event = registerViewModel.registerEvents.first()
        assertTrue(event is RegisterEvent.Success)
        assertEquals(testLoginSuccess.userId, (event as RegisterEvent.Success).userId)

        coVerify(exactly = 1) { swRepository.register(any<RegistrationRequest>()) }
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken("test-token") }
        coVerify(exactly = 1) { preferencesRepository.saveCurrentUserId(testLoginSuccess.userId) }
    }

    @Test
    fun register_WhenApiError_thenReturnsError() = runTest {
        // Given
        fillValidForm()
        val errorMessage = "Пользователь с таким email уже существует"
        coEvery { swRepository.register(any<RegistrationRequest>()) } returns Result.failure(
            Exception(errorMessage)
        )

        // When
        registerViewModel.register()
        advanceUntilIdle()

        // Then
        val state = registerViewModel.uiState.value
        assertTrue(state is RegisterUiState.Error)
        val errorState = state as RegisterUiState.Error
        assertEquals(errorMessage, errorState.message)
    }

    @Test
    fun register_WhenInvalidForm_thenDoesNotCallApi() = runTest {
        // Given - form is not valid (empty fields)
        // When
        registerViewModel.register()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { swRepository.register(any<RegistrationRequest>()) }
    }

    @Test
    fun resetForNewSession_WhenCalled_thenClearsAllData() = runTest {
        // Given - fill some data
        fillValidForm()

        // When
        registerViewModel.resetForNewSession()

        // Then
        val form = registerViewModel.form.value
        assertEquals("", form.login)
        assertEquals("", form.email)
        assertEquals("", form.password)
        assertEquals("", form.fullName)
        assertNull(form.genderCode)
        assertNull(form.birthDate)
        assertNull(form.countryId)
        assertNull(form.cityId)
        assertFalse(form.isPolicyAccepted)
        assertTrue(registerViewModel.uiState.value is RegisterUiState.Idle)
    }

    @Test
    fun onCitySelectedByName_WhenCountryNotSelected_thenSelectsCountryAutomatically() = runTest {
        // Given - country not selected
        assertNull(registerViewModel.selectedCountry.value)

        // When - select city without selecting country first
        registerViewModel.onCitySelectedByName("Москва")
        advanceUntilIdle()

        // Then - city and country are selected
        assertEquals("1", registerViewModel.form.value.cityId)
        assertEquals(testCity, registerViewModel.selectedCity.value)
        assertEquals(testCountry, registerViewModel.selectedCountry.value)
        assertEquals("1", registerViewModel.form.value.countryId)
    }

    @Test
    fun allCities_WhenViewModelCreated_thenLoadsAllCities() = runTest {
        // When - ViewModel created (happens in setup)

        // Then - allCities is loaded
        advanceUntilIdle()
        assertEquals(listOf(testCity), registerViewModel.allCities.value)
    }

    private fun fillValidForm() {
        registerViewModel.onLoginChange("testuser")
        registerViewModel.onEmailChange("test@example.com")
        registerViewModel.onPasswordChange("password123")
        registerViewModel.onFullNameChange("Ivan Ivanov")
        registerViewModel.onGenderChange(0)
        registerViewModel.onBirthDateChange(LocalDate.of(2000, 1, 15))
        registerViewModel.onCountrySelectedByName("Россия")
        registerViewModel.onCitySelectedByName("Москва")
        registerViewModel.onPolicyAcceptedChange(true)
    }
}
