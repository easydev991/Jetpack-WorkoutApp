package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.R
import com.swparks.data.SecureTokenRepository
import com.swparks.data.TokenEncoder
import com.swparks.data.UserPreferencesRepository
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.CountriesRepository
import com.swparks.ui.model.RegisterForm
import com.swparks.ui.model.RegistrationRequest
import com.swparks.ui.state.RegisterEvent
import com.swparks.ui.state.RegisterUiState
import com.swparks.util.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel для управления экраном регистрации.
 *
 * Управляет состоянием UI экрана регистрации, включая форму,
 * валидацию полей и отправку запроса на сервер.
 *
 * @param logger Логгер для записи сообщений
 * @param swRepository Репозиторий для работы с API
 * @param secureTokenRepository Репозиторий для безопасного хранения токена
 * @param preferencesRepository Репозиторий для хранения настроек
 * @param tokenEncoder Кодировщик токена
 * @param countriesRepository Репозиторий для работы со странами и городами
 * @param resources Провайдер строковых ресурсов
 */
@Suppress("LongParameterList")
class RegisterViewModel(
    private val logger: Logger,
    private val swRepository: SWRepository,
    private val secureTokenRepository: SecureTokenRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val tokenEncoder: TokenEncoder,
    private val countriesRepository: CountriesRepository,
    private val resources: ResourcesProvider
) : ViewModel(), IRegisterViewModel {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    override val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _registerEvents = Channel<RegisterEvent>(Channel.BUFFERED)
    override val registerEvents = _registerEvents.receiveAsFlow()

    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    override val countries: StateFlow<List<Country>> = _countries.asStateFlow()

    private val _cities = MutableStateFlow<List<City>>(emptyList())
    override val cities: StateFlow<List<City>> = _cities.asStateFlow()

    private val _allCities = MutableStateFlow<List<City>>(emptyList())
    override val allCities: StateFlow<List<City>> = _allCities.asStateFlow()

    private val _selectedCountry = MutableStateFlow<Country?>(null)
    override val selectedCountry: StateFlow<Country?> = _selectedCountry.asStateFlow()

    private val _selectedCity = MutableStateFlow<City?>(null)
    override val selectedCity: StateFlow<City?> = _selectedCity.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    override val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _emailFormatError = MutableStateFlow<String?>(null)
    override val emailFormatError: StateFlow<String?> = _emailFormatError.asStateFlow()

    private val _passwordLengthError = MutableStateFlow<String?>(null)
    override val passwordLengthError: StateFlow<String?> = _passwordLengthError.asStateFlow()

    private val _birthDateError = MutableStateFlow<String?>(null)
    override val birthDateError: StateFlow<String?> = _birthDateError.asStateFlow()

    private val _form = MutableStateFlow(RegisterForm())
    override val form: StateFlow<RegisterForm> = _form.asStateFlow()

    init {
        // Сначала загружаем данные стран из assets, потом подписываемся на Flow
        countriesRepository.ensureCountriesLoaded()
        loadCountries()
        loadAllCities()
    }

    private fun loadCountries() {
        viewModelScope.launch {
            countriesRepository.getCountriesFlow().collect { countriesList ->
                _countries.value = countriesList
            }
        }
    }

    private fun loadCitiesForCountry(countryId: String) {
        viewModelScope.launch {
            val citiesList = countriesRepository.getCitiesByCountry(countryId)
            _cities.value = citiesList
        }
    }

    private fun loadAllCities() {
        viewModelScope.launch {
            val citiesList = countriesRepository.getAllCities()
            _allCities.value = citiesList
        }
    }

    override fun onLoginChange(value: String) {
        _form.value = _form.value.copy(login = value)
        _loginError.value = null
    }

    override fun onEmailChange(value: String) {
        _form.value = _form.value.copy(email = value)
        // Валидация email "на лету"
        _emailFormatError.value = if (value.isNotEmpty() && !isValidEmail(value)) {
            resources.getString(R.string.email_invalid)
        } else {
            null
        }
    }

    override fun onPasswordChange(value: String) {
        _form.value = _form.value.copy(password = value)
        // Валидация длины пароля "на лету"
        val trueCount = value.count { !it.isWhitespace() }
        _passwordLengthError.value =
            if (value.isNotEmpty() && trueCount < RegisterForm.MIN_PASSWORD_LENGTH) {
                resources.getString(R.string.password_short)
            } else {
                null
            }
    }

    override fun onFullNameChange(value: String) {
        _form.value = _form.value.copy(fullName = value)
    }

    override fun onGenderChange(genderCode: Int) {
        _form.value = _form.value.copy(genderCode = genderCode)
    }

    override fun onBirthDateChange(date: LocalDate?) {
        _form.value = _form.value.copy(birthDate = date)
        _birthDateError.value = null
    }

    override fun onCountrySelectedByName(countryName: String) {
        val country = _countries.value.find { it.name == countryName }
        if (country != null) {
            _form.value = _form.value.copy(countryId = country.id, cityId = null)
            _selectedCountry.value = country
            _selectedCity.value = null
            _cities.value = emptyList()

            // Загружаем города для выбранной страны
            loadCitiesForCountry(country.id)
        }
    }

    override fun onCitySelectedByName(cityName: String) {
        // Сначала ищем в городах для выбранной страны
        var city = _cities.value.find { it.name == cityName }

        // Если не найден и страна не выбрана - ищем во всех городах
        if (city == null && _selectedCountry.value == null) {
            city = _allCities.value.find { it.name == cityName }
        }

        if (city != null) {
            val selectedCity = city

            // Если страна еще не выбрана - автоматически выбираем страну для этого города
            if (_selectedCountry.value == null) {
                viewModelScope.launch {
                    val country = countriesRepository.getCountryForCity(selectedCity.id)
                    if (country != null) {
                        _form.value = _form.value.copy(countryId = country.id)
                        _selectedCountry.value = country
                        loadCitiesForCountry(country.id)
                    }
                }
            }

            _form.value = _form.value.copy(cityId = selectedCity.id)
            _selectedCity.value = selectedCity
        }
    }

    override fun onPolicyAcceptedChange(accepted: Boolean) {
        _form.value = _form.value.copy(isPolicyAccepted = accepted)
    }

    override fun register() {
        if (!validateForm()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading

            val currentForm = _form.value
            val request = RegistrationRequest(
                name = currentForm.login,
                fullName = currentForm.fullName,
                email = currentForm.email,
                password = currentForm.password,
                birthDate = currentForm.birthDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE),
                genderCode = currentForm.genderCode!!,
                countryId = currentForm.countryId?.toIntOrNull(),
                cityId = currentForm.cityId?.toIntOrNull()
            )

            swRepository.register(request)
                .onSuccess { loginSuccess ->
                    // Генерируем токен из логина и пароля (как при обычной авторизации)
                    val token = tokenEncoder.encode(
                        com.swparks.ui.model.LoginCredentials(
                            login = currentForm.login,
                            password = currentForm.password
                        )
                    )
                    secureTokenRepository.saveAuthToken(token)

                    // Сохраняем userId
                    preferencesRepository.saveCurrentUserId(loginSuccess.userId)

                    // Отправляем событие успеха
                    _registerEvents.send(RegisterEvent.Success(loginSuccess.userId))
                    _uiState.value = RegisterUiState.Idle
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Ошибка регистрации"
                    _uiState.value = RegisterUiState.Error(errorMessage, exception)
                    logger.e("RegisterViewModel", "Ошибка регистрации: $errorMessage", exception)
                }
        }
    }

    override fun clearErrors() {
        _loginError.value = null
        _emailFormatError.value = null
        _passwordLengthError.value = null
        _birthDateError.value = null
    }

    override fun resetForNewSession() {
        _form.value = RegisterForm()
        _uiState.value = RegisterUiState.Idle
        _selectedCountry.value = null
        _selectedCity.value = null
        _cities.value = emptyList()
        clearErrors()
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (_form.value.login.isBlank()) {
            _loginError.value = resources.getString(R.string.login_empty)
            isValid = false
        }

        if (_form.value.email.isNotEmpty() && !isValidEmail(_form.value.email)) {
            // Ошибка уже показана через onEmailChange
            isValid = false
        }

        if (_form.value.password.isNotEmpty() &&
            _form.value.password.count { !it.isWhitespace() } < RegisterForm.MIN_PASSWORD_LENGTH
        ) {
            // Ошибка уже показана через onPasswordChange
            isValid = false
        }

        val birthDate = _form.value.birthDate
        if (birthDate != null) {
            val age = java.time.Period.between(birthDate, LocalDate.now()).years
            if (age < MINIMUM_AGE) {
                _birthDateError.value = "Вам должно быть не менее 13 лет"
                isValid = false
            }
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email)
    }

    companion object {
        private const val MINIMUM_AGE = 13
    }
}
