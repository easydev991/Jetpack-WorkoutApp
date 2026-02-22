package com.swparks.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.model.EditProfileLocations
import com.swparks.domain.provider.AvatarHelper
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.IDeleteUserUseCase
import com.swparks.ui.model.Gender
import com.swparks.ui.model.MainUserForm
import com.swparks.ui.state.EditProfileEvent
import com.swparks.ui.state.EditProfileUiState
import com.swparks.util.AppError
import com.swparks.util.ImageUtils
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * ViewModel для экрана редактирования профиля.
 *
 * Управляет данными формы пользователя и справочником стран/городов.
 *
 * @param swRepository Репозиторий для работы с данными пользователя и API
 * @param countriesRepository Репозиторий для работы с данными стран и городов
 * @param deleteUserUseCase Use case для удаления аккаунта пользователя
 * @param avatarHelper Хелпер для работы с аватарами (Uri → ByteArray)
 * @param logger Логгер для записи сообщений
 * @param userNotifier Обработчик ошибок для отправки ошибок в UI
 * @param resources Провайдер строковых ресурсов
 */
@Suppress("TooGenericExceptionCaught")
class EditProfileViewModel(
    private val swRepository: SWRepository,
    private val countriesRepository: CountriesRepository,
    private val deleteUserUseCase: IDeleteUserUseCase,
    private val avatarHelper: AvatarHelper,
    private val logger: Logger,
    private val userNotifier: UserNotifier,
    private val resources: ResourcesProvider
) : ViewModel(), IEditProfileViewModel {

    private companion object {
        private const val TAG = "EditProfileViewModel"
        private const val STATE_TIMEOUT_MS = 5000L
    }

    // Текущий пользователь из репозитория
    private val currentUser: StateFlow<User?> = swRepository.getCurrentUserFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = null
        )

    // UI State
    private val _uiState = MutableStateFlow(EditProfileUiState())
    override val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    // Event-канал для side-effects
    private val _events = MutableSharedFlow<EditProfileEvent>()
    override val events: SharedFlow<EditProfileEvent> = _events.asSharedFlow()

    init {
        // Подписываемся на изменения пользователя
        viewModelScope.launch {
            currentUser.collect { user ->
                user?.let { loadUserData(it) }
            }
        }
    }

    /**
     * Загружает данные пользователя в форму.
     */
    private suspend fun loadUserData(user: User) {
        logger.i(TAG, "Загрузка данных пользователя: ${user.id}")

        // Загружаем список стран
        val countries = countriesRepository.getCountriesFlow().first()

        // Определяем выбранную страну и город
        val selectedCountry = user.countryID?.let { countryId ->
            countries.find { it.id == countryId.toString() }
        }

        val cities = if (selectedCountry != null) {
            countriesRepository.getCitiesByCountry(selectedCountry.id)
        } else {
            emptyList()
        }

        val selectedCity = user.cityID?.let { cityId ->
            cities.find { it.id == cityId.toString() }
        }

        // Создаем форму из данных пользователя
        val userForm = MainUserForm(
            name = user.name,
            fullname = user.fullName ?: "",
            email = user.email ?: "",
            password = "",
            birthDate = user.birthDate ?: "",
            genderCode = user.genderCode ?: 0,
            countryId = user.countryID,
            cityId = user.cityID
        )

        _uiState.update {
            it.copy(
                userForm = userForm,
                initialForm = userForm,
                countries = countries,
                cities = cities,
                selectedCountry = selectedCountry,
                selectedCity = selectedCity,
                initialCountry = selectedCountry,
                initialCity = selectedCity,
                isLoading = false
            )
        }

        logger.i(TAG, "Данные пользователя загружены")
    }

    override fun onLoginChange(value: String) {
        _uiState.update { it.copy(userForm = it.userForm.copy(name = value)) }
    }

    override fun onEmailChange(value: String) {
        val emailError = if (value.isNotEmpty() && !isValidEmail(value)) {
            resources.getString(R.string.email_invalid)
        } else {
            null
        }
        _uiState.update {
            it.copy(
                userForm = it.userForm.copy(email = value),
                emailError = emailError
            )
        }
    }

    override fun onFullNameChange(value: String) {
        _uiState.update { it.copy(userForm = it.userForm.copy(fullname = value)) }
    }

    override fun onGenderChange(gender: Gender) {
        _uiState.update { it.copy(userForm = it.userForm.copy(genderCode = gender.rawValue)) }
        logger.d(TAG, "Пол изменен: ${gender.name}")
    }

    override fun onBirthDateChange(timestamp: Long) {
        val localDate = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val formatted = localDate.format(DateTimeFormatter.ISO_DATE)

        // Валидация: дата не должна быть в будущем
        val birthDateError = if (localDate.isInFuture()) {
            resources.getString(R.string.birth_date_in_future)
        } else {
            null
        }

        _uiState.update {
            it.copy(
                userForm = it.userForm.copy(birthDate = formatted),
                birthDateError = birthDateError
            )
        }
        logger.d(TAG, "Дата рождения изменена: $formatted")
    }

    override fun onCountryClick() {
        val countryId = _uiState.value.userForm.countryId
        viewModelScope.launch {
            _events.emit(EditProfileEvent.NavigateToSelectCountry(countryId))
        }
        logger.d(TAG, "Клик по стране: $countryId")
    }

    override fun onCityClick() {
        val form = _uiState.value.userForm
        val countryId = form.countryId
        if (countryId != null) {
            viewModelScope.launch {
                _events.emit(EditProfileEvent.NavigateToSelectCity(form.cityId, countryId))
            }
        }
        logger.d(TAG, "Клик по городу: ${form.cityId}, страна: $countryId")
    }

    override fun onChangePasswordClick() {
        val userId = currentUser.value?.id ?: return
        viewModelScope.launch {
            _events.emit(EditProfileEvent.NavigateToChangePassword(userId))
        }
        logger.d(TAG, "Клик по смене пароля")
    }

    override fun onChangeAvatarClick() {
        logger.d(TAG, "Клик по изменению фото")
    }

    override fun onAvatarSelected(uri: Uri?) {
        // Если uri == null — пользователь отменил выбор
        if (uri == null) {
            logger.d(TAG, "Avatar selection cancelled by user")
            return
        }

        // Проверяем MIME-тип
        if (!avatarHelper.isSupportedMimeType(uri)) {
            val errorMessage = resources.getString(R.string.avatar_error_unsupported_type)
            logger.w(TAG, "Unsupported MIME type for uri: $uri")
            _uiState.update { it.copy(avatarError = errorMessage) }
            return
        }

        // Сбрасываем ошибку и сохраняем URI
        logger.d(TAG, "Avatar selected: $uri")
        _uiState.update {
            it.copy(
                selectedAvatarUri = uri,
                avatarError = null
            )
        }
    }

    override fun onSaveClick() {
        val currentState = _uiState.value

        // Критическая ошибка - пользователь не авторизован
        val userId = currentUser.value?.id
        if (userId == null) {
            logger.e(TAG, "Пользователь не авторизован")
            userNotifier.handleError(AppError.Generic("Пользователь не авторизован", null))
            return
        }

        // Проверяем валидность состояния для сохранения
        if (!currentState.hasChanges || currentState.isSaving) {
            when {
                !currentState.hasChanges -> logger.w(TAG, "Попытка сохранить без изменений")
                currentState.isSaving -> logger.w(TAG, "Сохранение уже в процессе")
            }
            return
        }

        logger.i(TAG, "Начало сохранения профиля")

        // Устанавливаем состояние сохранения
        _uiState.update {
            it.copy(
                isSaving = true,
                isUploadingAvatar = currentState.selectedAvatarUri != null
            )
        }

        viewModelScope.launch {
            // Подготавливаем изображение если выбрано
            val imageBytes = prepareImageBytes(currentState.selectedAvatarUri)
            if (currentState.selectedAvatarUri != null && imageBytes == null) {
                // Ошибка уже обработана в prepareImageBytes
                return@launch
            }

            // Сохраняем профиль
            saveProfile(userId, currentState, imageBytes)
        }
    }

    /**
     * Подготавливает изображение для отправки на сервер.
     *
     * @param uri URI выбранного изображения или null
     * @return ByteArray с данными изображения или null если URI null или произошла ошибка
     */
    private fun prepareImageBytes(uri: Uri?): ByteArray? {
        if (uri == null) return null

        val uriResult = avatarHelper.uriToByteArray(uri)
        return uriResult.fold(
            onSuccess = { bytes ->
                val compressed = ImageUtils.compressIfNeeded(bytes)
                logger.d(TAG, "Image prepared: ${bytes.size} -> ${compressed.size} bytes")
                compressed
            },
            onFailure = { error ->
                logger.e(TAG, "Ошибка чтения изображения: ${error.message}", error)
                val errorMessage = resources.getString(R.string.avatar_error_read_failed)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isUploadingAvatar = false,
                        avatarError = errorMessage
                    )
                }
                userNotifier.handleError(AppError.Generic(errorMessage, error))
                null
            }
        )
    }

    /**
     * Сохраняет профиль пользователя.
     */
    private suspend fun saveProfile(
        userId: Long,
        currentState: EditProfileUiState,
        imageBytes: ByteArray?
    ) {
        val result = swRepository.editUser(
            userId = userId,
            form = currentState.userForm,
            image = imageBytes
        )

        result.fold(
            onSuccess = { updatedUser ->
                logger.i(TAG, "Профиль успешно обновлен: ${updatedUser.id}")
                _uiState.update {
                    it.copy(
                        initialForm = it.userForm,
                        initialCountry = it.selectedCountry,
                        initialCity = it.selectedCity,
                        selectedAvatarUri = null,
                        avatarError = null,
                        isSaving = false,
                        isUploadingAvatar = false
                    )
                }
                _events.emit(EditProfileEvent.NavigateBack)
            },
            onFailure = { error ->
                logger.e(TAG, "Ошибка сохранения профиля: ${error.message}", error)
                _uiState.update { it.copy(isSaving = false, isUploadingAvatar = false) }
                userNotifier.handleError(
                    AppError.Generic(
                        error.message ?: "Ошибка сохранения профиля",
                        error
                    )
                )
            }
        )
    }

    /**
     * Обрабатывает выбор страны.
     *
     * @param countryName Имя выбранной страны
     */
    override fun onCountrySelected(countryName: String) {
        val locations = EditProfileLocations.fromCountries(_uiState.value.countries)
        val result = locations.selectCountry(countryName, _uiState.value.selectedCity)

        _uiState.update {
            it.copy(
                selectedCountry = result.newCountry,
                selectedCity = result.newCity,
                cities = result.newCities,
                userForm = it.userForm.copy(
                    countryId = result.newCountry?.id?.toIntOrNull(),
                    cityId = result.newCity?.id?.toIntOrNull()
                )
            )
        }

        logger.d(TAG, "Выбрана страна: $countryName, город: ${result.newCity?.name ?: "сброшен"}")
    }

    /**
     * Обрабатывает выбор города.
     *
     * @param cityName Имя выбранного города
     */
    override fun onCitySelected(cityName: String) {
        val locations = EditProfileLocations.fromCountries(_uiState.value.countries)
        val result = locations.selectCity(cityName, _uiState.value.selectedCountry)

        // Если город из другой страны - сначала обновляем страну
        if (result.countryName != null) {
            val countryResult = locations.selectCountry(result.countryName, null)
            _uiState.update {
                it.copy(
                    selectedCountry = countryResult.newCountry,
                    selectedCity = result.newCity,
                    cities = countryResult.newCities,
                    userForm = it.userForm.copy(
                        countryId = countryResult.newCountry?.id?.toIntOrNull(),
                        cityId = result.newCity?.id?.toIntOrNull()
                    )
                )
            }
            logger.d(
                TAG,
                "Выбран город: $cityName из другой страны: ${result.countryName}"
            )
        } else {
            _uiState.update {
                it.copy(
                    selectedCity = result.newCity,
                    userForm = it.userForm.copy(
                        cityId = result.newCity?.id?.toIntOrNull()
                    )
                )
            }
            logger.d(TAG, "Выбран город: $cityName")
        }
    }

    /**
     * Сбрасывает несохранённые изменения к исходным значениям.
     *
     * Вызывается при закрытии экрана без сохранения для восстановления
     * исходных значений страны и города.
     */
    override fun resetChanges() {
        val currentState = _uiState.value

        // Если изменений нет - нечего сбрасывать
        if (!currentState.hasChanges) {
            return
        }

        logger.i(TAG, "Сброс несохранённых изменений")

        val initialCountry = currentState.initialCountry
        val initialCity = currentState.initialCity

        // Запускаем корутину для получения списка городов
        viewModelScope.launch {
            val cities = if (initialCountry != null) {
                countriesRepository.getCitiesByCountry(initialCountry.id)
            } else {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    userForm = it.initialForm,
                    selectedCountry = initialCountry,
                    selectedCity = initialCity,
                    cities = cities,
                    selectedAvatarUri = null,
                    avatarError = null,
                    emailError = null,
                    birthDateError = null
                )
            }
        }
    }

    /**
     * Удаляет профиль пользователя.
     *
     * Показывает состояние загрузки, вызывает API для удаления профиля на сервере,
     * очищает все локальные данные пользователя и перенаправляет на экран логина.
     */
    override fun onDeleteProfileClick() {
        val currentState = _uiState.value

        // Предотвращаем повторный клик во время удаления
        if (currentState.isDeleting) {
            logger.w(TAG, "Удаление уже в процессе")
            return
        }

        logger.i(TAG, "Начало удаления профиля")

        // Устанавливаем состояние удаления
        _uiState.update { it.copy(isDeleting = true) }

        viewModelScope.launch {
            val result = deleteUserUseCase()

            result.fold(
                onSuccess = {
                    logger.i(TAG, "Профиль успешно удален")
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.emit(EditProfileEvent.NavigateToLogin)
                },
                onFailure = { error ->
                    logger.e(TAG, "Ошибка удаления профиля: ${error.message}", error)
                    _uiState.update { it.copy(isDeleting = false) }
                    userNotifier.handleError(
                        AppError.Generic(
                            error.message ?: "Ошибка удаления профиля",
                            error
                        )
                    )
                }
            )
        }
    }

    /**
     * Проверяет валидность email адреса.
     *
     * @param email Email для проверки
     * @return true если email валиден
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email)
    }
}

/**
 * Проверяет, находится ли дата в будущем.
 */
private fun LocalDate.isInFuture(): Boolean = this > LocalDate.now()
