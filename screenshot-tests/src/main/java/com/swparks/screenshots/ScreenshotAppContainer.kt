package com.swparks.screenshots

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import com.swparks.data.AppContainer
import com.swparks.data.DefaultAppContainer
import com.swparks.data.model.Country
import com.swparks.data.model.Event
import com.swparks.data.model.LoginSuccess
import com.swparks.data.model.Park
import com.swparks.data.model.SocialUpdates
import com.swparks.data.model.User
import com.swparks.data.provider.ResourcesProviderImpl
import com.swparks.data.repository.SWRepository
import com.swparks.domain.exception.NotFoundException
import com.swparks.domain.model.LocationCoordinates
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.repository.MessagesRepository
import com.swparks.domain.provider.LocationService
import com.swparks.domain.provider.LocationSettingsCheckResult
import com.swparks.domain.usecase.IGetFutureEventsFlowUseCase
import com.swparks.domain.usecase.IGetPastEventsFlowUseCase
import com.swparks.domain.usecase.IInitializeParksUseCase
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.ISyncFutureEventsUseCase
import com.swparks.domain.usecase.ISyncPastEventsUseCase
import com.swparks.domain.usecase.SyncCountriesUseCase
import com.swparks.domain.usecase.SyncParksUseCase
import com.swparks.ui.model.EventType
import com.swparks.ui.model.LoginCredentials
import com.swparks.ui.viewmodel.DialogsViewModel
import com.swparks.ui.viewmodel.OtherUserProfileViewModel
import com.swparks.ui.viewmodel.ProfileViewModel
import com.swparks.ui.viewmodel.SearchUserViewModel

/**
 * Screenshot container with deterministic data.
 *
 * Разделение ответственностей:
 * 1) Этот контейнер подменяет зависимости и отдает demo-данные.
 * 2) Стартовые nav-state константы для detail-экранов вынесены в [ScreenshotScenarioState].
 */
class ScreenshotAppContainer(
    context: Context,
    private val delegate: AppContainer = DefaultAppContainer(context)
) : AppContainer by delegate {
    private companion object {
        private const val MOSCOW_LATITUDE = 55.7558
        private const val MOSCOW_LONGITUDE = 37.6173
    }

    private val appContext = context.applicationContext
    private val demoParks = DemoData.loadDemoParks(appContext)
    private val demoCountries = DemoData.loadDemoCountries(appContext)
    private val screenshotSwRepository = ScreenshotSwRepository(
        delegate = delegate.swRepository,
        demoParks = demoParks
    )
    private val screenshotCountriesRepository = ScreenshotCountriesRepository(
        countries = demoCountries
    )
    private val screenshotMessagesRepository = ScreenshotMessagesRepository()
    private val resourcesProvider = ResourcesProviderImpl(appContext)

    override val swRepository: SWRepository = screenshotSwRepository
    override val countriesRepository: CountriesRepository = screenshotCountriesRepository
    override val messagesRepository: MessagesRepository = screenshotMessagesRepository
    override val locationService: LocationService = object : LocationService {
        override suspend fun getCurrentLocation(): Result<LocationCoordinates> =
            Result.success(
                LocationCoordinates(
                    latitude = MOSCOW_LATITUDE,
                    longitude = MOSCOW_LONGITUDE
                )
            )

        override suspend fun checkLocationSettings(): Result<LocationSettingsCheckResult> =
            Result.success(LocationSettingsCheckResult.SettingsOk)
    }
    override val syncParksUseCase: SyncParksUseCase = SyncParksUseCase(
        clock = clock,
        userPreferencesRepository = userPreferencesRepository,
        swRepository = swRepository,
        logger = logger
    )
    override val syncCountriesUseCase: SyncCountriesUseCase = SyncCountriesUseCase(
        clock = clock,
        userPreferencesRepository = userPreferencesRepository,
        countriesRepository = countriesRepository,
        logger = logger,
        analyticsService = analyticsService
    )

    override val initializeParksUseCase: IInitializeParksUseCase = object : IInitializeParksUseCase {
        override suspend fun invoke(): Result<Unit> = Result.success(Unit)
    }
    override val getFutureEventsFlowUseCase: IGetFutureEventsFlowUseCase =
        object : IGetFutureEventsFlowUseCase {
            override fun invoke(): Flow<List<Event>> = screenshotSwRepository.getFutureEventsFlow()
        }
    override val getPastEventsFlowUseCase: IGetPastEventsFlowUseCase =
        object : IGetPastEventsFlowUseCase {
            override fun invoke(): Flow<List<Event>> = screenshotSwRepository.getPastEventsFlow()
        }
    override val syncFutureEventsUseCase: ISyncFutureEventsUseCase =
        object : ISyncFutureEventsUseCase {
            override suspend fun invoke(): Result<Unit> = Result.success(Unit)
        }
    override val syncPastEventsUseCase: ISyncPastEventsUseCase =
        object : ISyncPastEventsUseCase {
            override suspend fun invoke(): Result<Unit> = Result.success(Unit)
        }
    override val loginUseCase: ILoginUseCase = object : ILoginUseCase {
        override suspend fun invoke(credentials: LoginCredentials): Result<LoginSuccess> {
            if (credentials.login.isBlank() || credentials.password.isBlank()) {
                return Result.failure(IllegalArgumentException("Логин и пароль обязательны"))
            }
            val loginResult = screenshotSwRepository.login(token = null)
            loginResult.onSuccess { success ->
                userPreferencesRepository.saveCurrentUserId(success.userId)
            }
            return loginResult
        }
    }

    override fun profileViewModelFactory(): ProfileViewModel = ProfileViewModel(
        countriesRepository = countriesRepository,
        swRepository = swRepository,
        logger = logger,
        userNotifier = userNotifier,
        analyticsService = analyticsService
    )

    override fun dialogsViewModelFactory(): DialogsViewModel = DialogsViewModel(
        messagesRepository = messagesRepository,
        swRepository = swRepository,
        logger = logger,
        resources = resourcesProvider,
        messageSentNotifier = messageSentNotifier,
        analyticsService = analyticsService
    )

    override fun searchUserViewModelFactory(): SearchUserViewModel = SearchUserViewModel(
        swRepository = swRepository,
        logger = logger,
        analyticsService = analyticsService
    )

    override fun otherUserProfileViewModelFactory(userId: Long): OtherUserProfileViewModel =
        OtherUserProfileViewModel(
            userId = userId,
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            logger = logger,
            userNotifier = userNotifier,
            resources = resourcesProvider,
            analyticsService = analyticsService
        )
}

private class ScreenshotSwRepository(
    private val delegate: SWRepository,
    demoParks: List<Park>
) : SWRepository by delegate {
    private val currentUserFlow = MutableStateFlow<User?>(null)
    private val parksFlow = MutableStateFlow(demoParks)
    private val futureEventsFlow = MutableStateFlow(DemoData.demoFutureEvents)
    private val pastEventsFlow = MutableStateFlow(DemoData.demoPastEvents)
    private val friendsFlow = MutableStateFlow<List<User>>(emptyList())
    private val friendRequestsFlow = MutableStateFlow<List<User>>(emptyList())
    private val blacklistFlow = MutableStateFlow<List<User>>(emptyList())
    private val isAuthorizedFlow = MutableStateFlow(false)

    override val isAuthorized: Flow<Boolean> = isAuthorizedFlow

    override fun getCurrentUserFlow(): Flow<User?> = currentUserFlow
    override fun getParksFlow(): Flow<List<com.swparks.data.model.Park>> = parksFlow
    override fun getFutureEventsFlow(): Flow<List<Event>> = futureEventsFlow
    override fun getPastEventsFlow(): Flow<List<Event>> = pastEventsFlow
    override fun getFriendsFlow(): Flow<List<User>> = friendsFlow
    override fun getFriendRequestsFlow(): Flow<List<User>> = friendRequestsFlow
    override fun getBlacklistFlow(): Flow<List<User>> = blacklistFlow
    override fun getFriendsCountFlow(): Flow<Int> = flowOf(DemoData.demoAuthorizedUser.friendsCount ?: 0)

    override suspend fun clearUserData() {
        currentUserFlow.value = null
        isAuthorizedFlow.value = false
    }

    override suspend fun login(token: String?): Result<LoginSuccess> {
        currentUserFlow.value = DemoData.demoAuthorizedUser
        isAuthorizedFlow.value = true
        return Result.success(LoginSuccess(userId = DemoData.demoAuthorizedUser.id))
    }

    override suspend fun forceLogout() {
        currentUserFlow.value = null
        isAuthorizedFlow.value = false
    }

    override suspend fun getUser(userId: Long): Result<User> {
        val user = when (userId) {
            DemoData.demoAuthorizedUser.id -> DemoData.demoAuthorizedUser
            DemoData.demoUser.id -> DemoData.demoUser
            DemoData.demoSearchUser.id -> DemoData.demoSearchUser
            else -> null
        } ?: return Result.failure(IllegalArgumentException("User not found: $userId"))
        return Result.success(user)
    }

    override suspend fun syncFutureEvents(): Result<Unit> = Result.success(Unit)
    override suspend fun syncPastEvents(): Result<Unit> = Result.success(Unit)

    override suspend fun getAllParks(): Result<List<com.swparks.data.model.Park>> =
        Result.success(parksFlow.value)

    override suspend fun getPark(id: Long): Result<com.swparks.data.model.Park> {
        val fallback = parksFlow.value.firstOrNull { it.id == id }
        val park = DemoData.parkDetailsById(id, fallback)
        if (fallback == null && park.id != id) {
            return Result.failure(NotFoundException.ParkNotFound(id))
        }
        return Result.success(park)
    }

    override suspend fun getParkFromCache(parkId: Long): com.swparks.data.model.Park? {
        val fallback = parksFlow.value.firstOrNull { it.id == parkId }
        return if (fallback != null) DemoData.parkDetailsById(parkId, fallback) else null
    }

    override suspend fun getParksForUser(userId: Long): Result<List<com.swparks.data.model.Park>> {
        return Result.success(DemoData.demoParksForUser(parksFlow.value))
    }

    override suspend fun importSeedParks(context: Context) = Unit

    override suspend fun upsertParks(parks: List<com.swparks.data.model.Park>) {
        parksFlow.value = if (parks.isEmpty()) parksFlow.value else parks
    }

    override suspend fun cachePark(park: com.swparks.data.model.Park) {
        parksFlow.value = parksFlow.value
            .filterNot { it.id == park.id }
            .plus(park)
            .sortedBy { it.id }
    }

    override suspend fun getCachedParksForUser(userId: Long): List<com.swparks.data.model.Park>? {
        return DemoData.demoParksForUser(parksFlow.value)
    }

    override suspend fun hasCachedParksForUser(userId: Long): Boolean = true

    override suspend fun getUpdatedParks(date: String): Result<List<com.swparks.data.model.Park>> {
        return Result.success(parksFlow.value)
    }

    override suspend fun getEvents(type: EventType): Result<List<Event>> {
        return when (type) {
            EventType.FUTURE -> Result.success(futureEventsFlow.value)
            EventType.PAST -> Result.success(pastEventsFlow.value)
        }
    }

    override suspend fun getEvent(id: Long): Result<Event> {
        val event = allEvents().firstOrNull { it.id == id } ?: return Result.failure(
            NotFoundException.EventNotFound(id)
        )
        return Result.success(event)
    }

    override suspend fun deleteEvent(eventId: Long): Result<Unit> {
        futureEventsFlow.value = futureEventsFlow.value.filterNot { it.id == eventId }
        pastEventsFlow.value = pastEventsFlow.value.filterNot { it.id == eventId }
        return Result.success(Unit)
    }

    override suspend fun changeIsGoingToEvent(go: Boolean, eventId: Long): Result<Unit> {
        updateEvent(eventId) { event ->
            event.copy(trainHere = go)
        }
        return Result.success(Unit)
    }

    override suspend fun getSocialUpdates(userId: Long): Result<SocialUpdates> {
        val user = currentUserFlow.value ?: DemoData.demoAuthorizedUser
        return Result.success(
            SocialUpdates(
                user = user,
                friends = emptyList(),
                friendRequests = emptyList(),
                blacklist = emptyList()
            )
        )
    }

    override suspend fun findUsers(name: String): Result<List<User>> {
        return Result.success(DemoData.searchUsers(name))
    }

    private fun allEvents(): List<Event> = futureEventsFlow.value + pastEventsFlow.value

    private fun updateEvent(eventId: Long, transform: (Event) -> Event) {
        futureEventsFlow.value = futureEventsFlow.value.map { event ->
            if (event.id == eventId) transform(event) else event
        }
        pastEventsFlow.value = pastEventsFlow.value.map { event ->
            if (event.id == eventId) transform(event) else event
        }
    }
}

private class ScreenshotCountriesRepository(
    private val countries: List<Country>
) : CountriesRepository {
    private val countriesFlow = MutableStateFlow(countries)
    private val citiesById = countries.flatMap { it.cities }.associateBy { it.id }
    private val countriesById = countries.associateBy { it.id }

    override fun ensureCountriesLoaded() = Unit

    override fun getCountriesFlow(): Flow<List<com.swparks.data.model.Country>> = countriesFlow

    override suspend fun getCountryById(countryId: String): com.swparks.data.model.Country? {
        return countriesById[countryId]
    }

    override suspend fun getCityById(cityId: String): com.swparks.data.model.City? {
        return citiesById[cityId]
    }

    override suspend fun getCitiesByCountry(countryId: String): List<com.swparks.data.model.City> {
        return getCountryById(countryId)?.cities.orEmpty()
    }

    override suspend fun getAllCities(): List<com.swparks.data.model.City> = citiesById.values.toList()

    override suspend fun getCountryForCity(cityId: String): com.swparks.data.model.Country? {
        val city = getCityById(cityId) ?: return null
        return countries.firstOrNull { country ->
            country.cities.any { it.id == city.id }
        }
    }

    override suspend fun updateCountriesFromServer(): Result<Unit> = Result.success(Unit)
}

private class ScreenshotMessagesRepository : MessagesRepository {
    override val dialogs: Flow<List<com.swparks.data.database.entity.DialogEntity>> =
        MutableStateFlow(emptyList())

    override suspend fun refreshDialogs(): Result<Unit> = Result.success(Unit)
}
