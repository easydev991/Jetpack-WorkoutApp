package com.swparks.ui.viewmodel

import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.ui.model.RegisterForm
import com.swparks.ui.state.RegisterEvent
import com.swparks.ui.state.RegisterUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Интерфейс для RegisterViewModel.
 *
 * Создан для возможности тестирования UI компонентов Compose без бизнес-логики.
 * Позволяет создавать mock/fake реализации для UI тестов.
 */
interface IRegisterViewModel {
    /**
     * Состояние UI экрана.
     */
    val uiState: StateFlow<RegisterUiState>

    /**
     * Одноразовые события регистрации (Success).
     */
    val registerEvents: Flow<RegisterEvent>

    /**
     * Текущая форма регистрации.
     */
    val form: StateFlow<RegisterForm>

    /**
     * Список стран для выбора.
     */
    val countries: StateFlow<List<Country>>

    /**
     * Список городов для выбранной страны.
     */
    val cities: StateFlow<List<City>>

    /**
     * Список всех городов из всех стран.
     * Используется когда страна еще не выбрана.
     */
    val allCities: StateFlow<List<City>>

    /**
     * Выбранная страна.
     */
    val selectedCountry: StateFlow<Country?>

    /**
     * Выбранный город.
     */
    val selectedCity: StateFlow<City?>

    /**
     * Ошибка валидации логина.
     */
    val loginError: StateFlow<String?>

    /**
     * Ошибка валидации email (формат email на лету).
     */
    val emailFormatError: StateFlow<String?>

    /**
     * Ошибка валидации пароля (длина пароля на лету).
     */
    val passwordLengthError: StateFlow<String?>

    /**
     * Ошибка валидации возраста.
     */
    val birthDateError: StateFlow<String?>

    /**
     * Обновляет логин.
     */
    fun onLoginChange(value: String)

    /**
     * Обновляет email.
     */
    fun onEmailChange(value: String)

    /**
     * Обновляет пароль.
     */
    fun onPasswordChange(value: String)

    /**
     * Обновляет полное имя.
     */
    fun onFullNameChange(value: String)

    /**
     * Обновляет пол.
     */
    fun onGenderChange(genderCode: Int)

    /**
     * Обновляет дату рождения.
     */
    fun onBirthDateChange(date: LocalDate?)

    /**
     * Выбирает страну по имени.
     */
    fun onCountrySelectedByName(countryName: String)

    /**
     * Выбирает город по имени.
     */
    fun onCitySelectedByName(cityName: String)

    /**
     * Обновляет флаг принятия соглашения.
     */
    fun onPolicyAcceptedChange(accepted: Boolean)

    /**
     * Выполняет регистрацию.
     */
    fun register()

    /**
     * Очищает все ошибки валидации.
     */
    fun clearErrors()

    /**
     * Сбрасывает состояние ViewModel для новой сессии регистрации.
     */
    fun resetForNewSession()
}
