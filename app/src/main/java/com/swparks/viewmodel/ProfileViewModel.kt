package com.swparks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.data.repository.SWRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.model.City
import com.swparks.model.Country
import com.swparks.model.User
import com.swparks.util.ErrorReporter
import com.swparks.util.Logger
import com.swparks.util.setValueIfChanged
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
 *
 * @param countriesRepository Репозиторий для работы с данными стран и городов
 * @param swRepository Репозиторий для работы с данными пользователя и API
 * @param logger Логгер для записи сообщений
 * @param errorReporter Обработчик ошибок для отправки ошибок в UI
 */
@Suppress("UnusedPrivateProperty", "TooGenericExceptionCaught", "MaxLineLength")
class ProfileViewModel(
    private val countriesRepository: CountriesRepository,
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val errorReporter: ErrorReporter,
) : ViewModel() {

    private companion object {
        private const val STATE_TIMEOUT_MS = 5000L
        private const val TAG = "ProfileViewModel"
    }

    // Подписываемся на текущего пользователя из кэша
    val currentUser: StateFlow<User?> = swRepository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = null
        )

    // UI State
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Состояние обновления данных (pull-to-refresh)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser
                .collect { user ->
                    loadProfileAddress(user)
                }
        }
    }

    /**
     * Загружает профиль пользователя и социальные данные с сервера по userId.
     * Используется после успешной авторизации для загрузки свежих данных.
     *
     * @param userId ID пользователя для загрузки профиля и социальных данных
     */
    fun loadProfileFromServer(userId: Long) {
        viewModelScope.launch {
            try {
                logger.i(TAG, "Начало загрузки профиля с сервера: $userId")
                loadSocialUpdates(userId, updateUiState = true)
            } catch (e: Exception) {
                val errorMessage = "Ошибка загрузки профиля и социальных данных: ${e.message}"
                _uiState.update { ProfileUiState.Error(errorMessage) }
                logger.e(TAG, errorMessage)
            }
        }
    }

    /**
     * Обновляет данные профиля пользователя с сервера (для pull-to-refresh).
     * Использует текущего пользователя из репозитория.
     */
    fun refreshProfile() {
        val user = currentUser.value ?: run {
            logger.w(TAG, "Пропускаем обновление профиля: пользователь не авторизован")
            return
        }

        viewModelScope.launch {
            try {
                _isRefreshing.update { true }
                logger.i(TAG, "Начало обновления профиля: ${user.id}")

                loadSocialUpdates(user.id, updateUiState = false)
            } catch (e: Exception) {
                val errorMessage = "Ошибка обновления профиля: ${e.message}"
                logger.e(TAG, errorMessage)
            } finally {
                _isRefreshing.update { false }
            }
        }
    }

    /**
     * Загружает социальные данные пользователя с сервера.
     *
     * @param userId ID пользователя
     * @param updateUiState Если true, обновляет UI State при ошибке (для loadProfileFromServer)
     */
    private suspend fun loadSocialUpdates(userId: Long, updateUiState: Boolean) {
        swRepository.getSocialUpdates(userId)
            .onSuccess { socialUpdates ->
                // Данные сохранены в кэше через SWRepository.getSocialUpdates()
                // Теперь загружаем страну и город
                loadProfileAddress(socialUpdates.user)
                logger.i(TAG, "Профиль успешно загружен: ${socialUpdates.user.id}")
            }
            .onFailure { error ->
                val errorMessage = "Ошибка загрузки профиля и социальных данных: ${error.message}"
                if (updateUiState) {
                    _uiState.update { ProfileUiState.Error(errorMessage) }
                }
                logger.e(TAG, errorMessage)
            }
    }

    /**
     * Загружает данные профиля с информацией о стране и городе.
     * Используется когда пользователь уже загружен (например, при открытии экрана).
     *
     * @param user Пользователь для которого загружаем профиль
     */
    private fun loadProfileAddress(user: User?) {
        if (user == null) {
            _uiState.update { ProfileUiState.Error("Пользователь не авторизован") }
            logger.d(TAG, "Пропускаем загрузку адреса: пользователь null")
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
                _uiState.setValueIfChanged(ProfileUiState.Success(country, city)) {
                    logger.i(TAG, "Адрес для профиля обновлен из countriesRepository")
                }
            } catch (e: Exception) {
                val message = "Ошибка загрузки адреса для профиля: ${e.message}"
                _uiState.update { ProfileUiState.Error(message) }
                logger.e(TAG, message)
            }
        }
    }
}
