package com.swparks.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.model.City
import com.swparks.model.Country
import com.swparks.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State для экрана профиля */
sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(
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
    private val swRepository: SWRepository,
) : ViewModel() {

    // Флаг для предотвращения дубликатной загрузки профиля
    private var isExplicitlyLoadingProfile = false

    /**
     * Загружает профиль пользователя с сервера по userId.
     * Используется после успешной авторизации для загрузки свежих данных.
     *
     * @param userId ID пользователя для загрузки профиля
     */
    fun loadUserProfileFromServer(userId: Long) {
        // Устанавливаем флаг, чтобы init не пытался загружать параллельно
        isExplicitlyLoadingProfile = true

        viewModelScope.launch {
            try {
                // Загружаем пользователя с сервера
                swRepository.getUser(userId)
                    .onSuccess { user ->
                        // Пользователь уже сохранен в кэше через SWRepository.getUser()
                        // Теперь загружаем страну и город
                        loadProfile(user)
                        Log.i("ProfileViewModel", "Профиль загружен с сервера: ${user.id}")

                        // Сбрасываем флаг после успешной загрузки
                        isExplicitlyLoadingProfile = false
                    }
                    .onFailure { error ->
                        _uiState.update { ProfileUiState.Error("Ошибка загрузки профиля: ${error.message}") }
                        Log.e("ProfileViewModel", "Ошибка загрузки профиля с сервера: ${error.message}")
                        isExplicitlyLoadingProfile = false
                    }
            } catch (e: Exception) {
                _uiState.update { ProfileUiState.Error("Ошибка загрузки профиля: ${e.message}") }
                Log.e("ProfileViewModel", "Ошибка загрузки профиля: ${e.message}")
                isExplicitlyLoadingProfile = false
            }
        }
    }

    // Подписываемся на текущего пользователя из кэша
    val currentUser: StateFlow<User?> = swRepository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // UI State
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Автоматически загружаем данные при изменении currentUser
        viewModelScope.launch {
            currentUser.collect { user ->
                // Если профиль загружается явно через loadUserProfileFromServer, не загружаем повторно
                if (isExplicitlyLoadingProfile) {
                    Log.d("ProfileViewModel", "Пропускаем загрузку профиля из-за явной загрузки")
                    return@collect
                }
                // Передаем user как параметр, чтобы избежать race condition
                loadProfile(user)
            }
        }
    }

    /**
     * Загружает данные профиля с информацией о стране и городе
     * @param user Пользователь для которого загружаем профиль
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

                _uiState.update { ProfileUiState.Success(country, city) }
                Log.i("ProfileViewModel", "Профиль загружен: ${user.id}")
            } catch (e: Exception) {
                _uiState.update { ProfileUiState.Error("Ошибка загрузки профиля: ${e.message}") }
            }
        }
    }

}
