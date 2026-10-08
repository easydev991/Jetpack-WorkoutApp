package com.swparks.ui.viewmodel

import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.ui.model.RegisterForm
import com.swparks.ui.state.RegisterEvent
import com.swparks.ui.state.RegisterUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDate

/**
 * Fake-реализация RegisterViewModel для UI тестов.
 *
 * Предоставляет простую реализацию интерфейса с возможностью установки состояния.
 * Используется в Compose UI тестах для проверки разных состояний экрана.
 */
class FakeRegisterViewModel(
    override val uiState: StateFlow<RegisterUiState> = MutableStateFlow(RegisterUiState.Idle),
    override val form: StateFlow<RegisterForm> = MutableStateFlow(RegisterForm()),
    override val countries: StateFlow<List<Country>> = MutableStateFlow(emptyList()),
    override val cities: StateFlow<List<City>> = MutableStateFlow(emptyList()),
    override val allCities: StateFlow<List<City>> = MutableStateFlow(emptyList()),
    override val selectedCountry: StateFlow<Country?> = MutableStateFlow(null),
    override val selectedCity: StateFlow<City?> = MutableStateFlow(null),
    override val loginError: StateFlow<String?> = MutableStateFlow(null),
    override val emailFormatError: StateFlow<String?> = MutableStateFlow(null),
    override val passwordLengthError: StateFlow<String?> = MutableStateFlow(null),
    override val birthDateError: StateFlow<String?> = MutableStateFlow(null)
) : IRegisterViewModel {
    // Поток событий для тестирования
    private val eventsFlow = MutableSharedFlow<RegisterEvent>()
    override val registerEvents: Flow<RegisterEvent> = eventsFlow.asSharedFlow()

    override fun onLoginChange(value: String) {
        // Заглушка - не делает ничего в тестах
    }

    override fun onEmailChange(value: String) {
        // Заглушка - не делает ничего в тестах
    }

    override fun onPasswordChange(value: String) {
        // Заглушка - не делает ничего в тестах
    }

    override fun onFullNameChange(value: String) {
        // Заглушка - не делает ничего в тестах
    }

    override fun onGenderChange(genderCode: Int) {
        // Заглушка - не делает ничего в тестах
    }

    override fun onBirthDateChange(date: LocalDate?) {
        // Заглушка - не делает ничего в тестах
    }

    override fun onCountrySelectedById(countryId: String) {
        // Заглушка - не делает ничего в тестах
    }

    override fun onCitySelectedById(cityId: String) {
        // Заглушка - не делает ничего в тестах
    }

    override fun onPolicyAcceptedChange(accepted: Boolean) {
        // Заглушка - не делает ничего в тестах
    }

    override fun register() {
        // Заглушка - не делает ничего в тестах
    }

    override fun clearErrors() {
        // Заглушка - не делает ничего в тестах
    }

    override fun onAction(action: RegisterContentAction) {
        // Заглушка - не делает ничего в тестах
    }

    override fun resetForNewSession() {
        // Заглушка - не делает ничего в тестах
    }
}
