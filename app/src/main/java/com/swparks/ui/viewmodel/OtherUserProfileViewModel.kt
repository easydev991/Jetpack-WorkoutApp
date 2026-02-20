package com.swparks.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swparks.R
import com.swparks.data.model.ApiFriendAction
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.CountriesRepository
import com.swparks.ui.model.BlacklistAction
import com.swparks.ui.model.toApiOption
import com.swparks.util.AppError
import com.swparks.util.HttpCodes
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException

@Suppress("TooGenericExceptionCaught")
class OtherUserProfileViewModel(
    private val userId: Long,
    private val countriesRepository: CountriesRepository,
    private val swRepository: SWRepository,
    private val logger: Logger,
    private val userNotifier: UserNotifier,
    private val resources: ResourcesProvider
) : ViewModel(), IOtherUserProfileViewModel {

    companion object {
        private const val TAG = "OtherUserProfileViewModel"
        const val CURRENT_USER_LOAD_TIMEOUT_MS = 10_000L // Доступна в тестах
        private const val SUBSCRIPTION_TIMEOUT_MS = 5000L
    }

    private val _viewedUser = MutableStateFlow<User?>(null)
    override val viewedUser: StateFlow<User?> = _viewedUser.asStateFlow()

    override val currentUser: StateFlow<User?> = swRepository.getCurrentUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

    override val friends: StateFlow<List<User>> = swRepository.getFriendsFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    override val blacklist: StateFlow<List<User>> = swRepository.getBlacklistFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    private val _uiState =
        MutableStateFlow<OtherUserProfileUiState>(OtherUserProfileUiState.Loading)
    override val uiState: StateFlow<OtherUserProfileUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingCurrentUser = MutableStateFlow(true)
    override val isLoadingCurrentUser: StateFlow<Boolean> = _isLoadingCurrentUser.asStateFlow()

    private val _isFriendActionLoading = MutableStateFlow(false)
    override val isFriendActionLoading: StateFlow<Boolean> = _isFriendActionLoading.asStateFlow()

    // Кэш адреса для оптимизации refreshUser
    private var lastCountryId: Int? = null
    private var lastCityId: Int? = null
    private var cachedCountry: Country? = null
    private var cachedCity: City? = null

    init {
        waitForCurrentUserAndLoad()
    }

    private fun waitForCurrentUserAndLoad() {
        viewModelScope.launch {
            try {
                // Ждем загрузки текущего пользователя с timeout
                withTimeout(CURRENT_USER_LOAD_TIMEOUT_MS) {
                    currentUser.first { it != null }
                }
                _isLoadingCurrentUser.update { false }
                loadUser()
            } catch (e: Exception) {
                logger.e(TAG, "Timeout ожидания currentUser: ${e.message}")
                _isLoadingCurrentUser.update { false }
                // При timeout retry бессмысленен — пользователь не авторизован
                _uiState.update {
                    OtherUserProfileUiState.Error(
                        "Ошибка авторизации",
                        canRetry = false
                    )
                }
            }
        }
    }

    override fun loadUser() {
        // Сброс кэша при новой загрузке (защита при переиспользовании ViewModel)
        cachedCountry = null
        cachedCity = null
        lastCountryId = null
        lastCityId = null

        viewModelScope.launch {
            _uiState.update { OtherUserProfileUiState.Loading }
            logger.i(TAG, "Загрузка профиля пользователя: $userId")

            swRepository.getUser(userId)
                .onSuccess { viewedUser ->
                    _viewedUser.update { viewedUser }
                    loadProfileAddress(viewedUser)
                    logger.i(TAG, "Профиль загружен: ${viewedUser.id}")
                }
                .onFailure { error ->
                    val (message, canRetry) = when {
                        error is HttpException && error.code() == HttpCodes.NOT_FOUND ->
                            "Пользователь не найден" to false
                        error is HttpException && error.code() == HttpCodes.FORBIDDEN ->
                            "Доступ запрещен" to false
                        else -> "Ошибка загрузки профиля: ${error.message}" to true
                    }
                    userNotifier.handleError(AppError.Generic(message, error))
                    if (!canRetry) {
                        _uiState.update { OtherUserProfileUiState.UserNotFound }
                    } else {
                        _uiState.update { OtherUserProfileUiState.Error(message, canRetry = true) }
                    }
                    logger.e(TAG, message)
                }
        }
    }

    override fun refreshUser() {
        viewModelScope.launch {
            _isRefreshing.update { true }
            logger.i(TAG, "Обновление профиля: $userId")

            swRepository.getUser(userId)
                .onSuccess { viewedUser ->
                    _viewedUser.update { viewedUser }
                    loadProfileAddress(viewedUser)
                }
                .onFailure { error ->
                    val message = "Ошибка обновления профиля: ${error.message}"
                    userNotifier.handleError(AppError.Generic(message, error))
                    logger.e(TAG, message)
                }

            _isRefreshing.update { false }
        }
    }

    private fun loadProfileAddress(viewedUser: User?) {
        if (viewedUser == null) return

        val countryId = viewedUser.countryID
        val cityId = viewedUser.cityID

        // Оптимизация: не загружаем адрес, если ID не изменились
        if (countryId == lastCountryId && cityId == lastCityId) {
            logger.i(TAG, "Адрес не изменился, используем кэш")
            updateUiStateWithAddress(cachedCountry, cachedCity)
            return
        }

        viewModelScope.launch {
            try {
                val country = countryId?.let { countriesRepository.getCountryById(it.toString()) }
                val city = cityId?.let { countriesRepository.getCityById(it.toString()) }

                // Обновляем кэш
                lastCountryId = countryId
                lastCityId = cityId
                cachedCountry = country
                cachedCity = city

                updateUiStateWithAddress(country, city)
                logger.i(TAG, "Адрес профиля обновлен")
            } catch (e: Exception) {
                val message = "Ошибка загрузки адреса: ${e.message}"
                userNotifier.handleError(AppError.Generic(message, e))
                logger.e(TAG, message)
            }
        }
    }

    private fun updateUiStateWithAddress(country: Country?, city: City?) {
        val currentState = _uiState.value
        _uiState.update {
            when (currentState) {
                is OtherUserProfileUiState.Loading -> OtherUserProfileUiState.Success(country, city)
                is OtherUserProfileUiState.Success -> OtherUserProfileUiState.Success(country, city)
                is OtherUserProfileUiState.Error -> OtherUserProfileUiState.Error(
                    currentState.message, currentState.canRetry, country, city
                )

                is OtherUserProfileUiState.UserNotFound -> OtherUserProfileUiState.UserNotFound
                is OtherUserProfileUiState.BlockedByUser -> OtherUserProfileUiState.BlockedByUser
            }
        }
    }

    override fun performFriendAction() {
        val viewedUserId = viewedUser.value?.id ?: return
        val isFriend = friends.value.any { it.id == viewedUserId }
        val action = if (isFriend) ApiFriendAction.REMOVE else ApiFriendAction.ADD

        viewModelScope.launch {
            _isFriendActionLoading.update { true }
            logger.i(TAG, "Действие с друзьями: $action для $viewedUserId")
            swRepository.friendAction(viewedUserId, action)
                .onSuccess {
                    logger.i(TAG, "Действие с друзьями выполнено успешно")
                    val message = if (action == ApiFriendAction.ADD) {
                        resources.getString(R.string.friend_request_sent)
                    } else {
                        resources.getString(R.string.friends_list_updated)
                    }
                    userNotifier.showInfo(message)
                }
                .onFailure { error ->
                    val message = "Ошибка действия с друзьями: ${error.message}"
                    userNotifier.handleError(AppError.Generic(message, error))
                    logger.e(TAG, message)
                }
            _isFriendActionLoading.update { false }
        }
    }

    override fun performBlacklistAction(onBlocked: () -> Unit) {
        val viewedUser = viewedUser.value ?: return
        val isInBlacklist = blacklist.value.any { it.id == viewedUser.id }
        val action = if (isInBlacklist) BlacklistAction.UNBLOCK else BlacklistAction.BLOCK

        viewModelScope.launch {
            logger.i(TAG, "Действие с черным списком: $action для ${viewedUser.id}")
            swRepository.blacklistAction(viewedUser, action.toApiOption())
                .onSuccess {
                    logger.i(TAG, "Действие с черным списком выполнено успешно: $action")
                    // После блокировки возвращаемся назад
                    if (action == BlacklistAction.BLOCK) {
                        onBlocked()
                    }
                }
                .onFailure { error ->
                    val message = "Ошибка действия с черным списком: ${error.message}"
                    userNotifier.handleError(AppError.Generic(message, error))
                    logger.e(TAG, message)
                }
        }
    }
}
