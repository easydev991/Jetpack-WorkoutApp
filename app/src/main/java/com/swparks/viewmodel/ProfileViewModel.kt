package com.swparks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.domain.repository.CountriesRepository
import com.swparks.model.City
import com.swparks.model.Country
import com.swparks.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State для экрана профиля */
sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(
        val user: User,
        val country: Country? = null,
        val city: City? = null,
    ) : ProfileUiState()

    data class Error(val message: String) : ProfileUiState()
}

/**
 * ViewModel для экрана профиля
 *
 * Управляет данными пользователя и справочником стран/городов
 */
class ProfileViewModel(
    private val countriesRepository: CountriesRepository,
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Загружает данные профиля с информацией о стране и городе
     *
     * @param user Данные пользователя для отображения
     */
    fun loadProfile(user: User?) {
        if (user == null) {
            _uiState.update { ProfileUiState.Error("Пользователь не авторизован") }
            return
        }

        viewModelScope.launch {
            try {
                // Получаем страну пользователя
                val country =
                    user.countryID?.let { countryId ->
                        countriesRepository.getCountryById(countryId.toString())
                    }

                // Получаем город пользователя
                val city =
                    user.cityID?.let { cityId ->
                        countriesRepository.getCityById(cityId.toString())
                    }

                _uiState.update { ProfileUiState.Success(user, country, city) }
            } catch (e: Exception) {
                _uiState.update { ProfileUiState.Error("Ошибка загрузки профиля: ${e.message}") }
            }
        }
    }
}
