package com.swparks.screenshots

import android.content.Context
import com.swparks.data.DefaultAppContainer
import com.swparks.data.TokenEncoder
import com.swparks.data.model.Country
import com.swparks.data.model.Event
import com.swparks.data.model.LoginSuccess
import com.swparks.data.model.Park
import com.swparks.data.model.SocialUpdates
import com.swparks.data.model.User
import com.swparks.data.provider.LocationServiceImpl
import com.swparks.data.provider.ResourcesProviderImpl
import com.swparks.data.repository.AuthRepository
import com.swparks.data.repository.CountriesRepositoryImpl
import com.swparks.data.repository.FriendsRepository
import com.swparks.data.repository.MessagesRepositoryImpl
import com.swparks.data.repository.ParksEventsRepository
import com.swparks.data.repository.UserProfileRepository
import com.swparks.domain.exception.NotFoundException
import com.swparks.domain.model.LocationCoordinates
import com.swparks.domain.provider.LocationSettingsCheckResult
import com.swparks.domain.usecase.GetFutureEventsFlowUseCase
import com.swparks.domain.usecase.GetPastEventsFlowUseCase
import com.swparks.domain.usecase.InitializeParksUseCase
import com.swparks.domain.usecase.LoginUseCase
import com.swparks.domain.usecase.SyncCountriesUseCase
import com.swparks.domain.usecase.SyncFutureEventsUseCase
import com.swparks.domain.usecase.SyncParksUseCase
import com.swparks.domain.usecase.SyncPastEventsUseCase
import com.swparks.network.SWApi
import com.swparks.ui.model.EventType
import com.swparks.ui.viewmodel.DialogsViewModel
import com.swparks.ui.viewmodel.OtherUserProfileViewModel
import com.swparks.ui.viewmodel.ProfileViewModel
import com.swparks.ui.viewmodel.SearchUserViewModel
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Screenshot container with deterministic data.
 *
 * Разделение ответственностей:
 * 1) Этот контейнер подменяет зависимости и отдает demo-данные.
 * 2) Стартовые nav-state константы для detail-экранов вынесены в [ScreenshotScenarioState].
 */
class ScreenshotAppContainer(
    context: Context
) : DefaultAppContainer(context) {
    private companion object {
        private const val MOSCOW_LATITUDE = 55.7558
        private const val MOSCOW_LONGITUDE = 37.6173
    }

    private val appContext = context.applicationContext
    private val demoParks = DemoData.loadDemoParks(appContext)
    private val demoCountries = DemoData.loadDemoCountries(appContext)
    private val screenshotCountriesRepository =
        ScreenshotCountriesRepository(
            appContext = appContext,
            countries = demoCountries,
            delegate = this
        )
    private val screenshotMessagesRepository =
        ScreenshotMessagesRepository(
            swApi = super.provideMessagesApi(),
            logger = super.logger,
            crashReporter = super.crashReporter
        )
    private val screenshotAuthRepository =
        ScreenshotAuthRepository(
            swApi = super.provideAuthApi(),
            logger = super.logger,
            crashReporter = super.crashReporter,
            preferencesRepository = super.userPreferencesRepository,
            userDao = super.userDao,
            dialogDao = super.dialogDao
        )
    private val screenshotUserProfileRepository =
        ScreenshotUserProfileRepository(
            swApi = super.provideProfileApi(),
            logger = super.logger,
            crashReporter = super.crashReporter,
            preferencesRepository = super.userPreferencesRepository,
            userDao = super.userDao
        )
    private val screenshotFriendsRepository =
        ScreenshotFriendsRepository(
            swApi = super.provideFriendsApi(),
            logger = super.logger,
            crashReporter = super.crashReporter,
            userDao = super.userDao
        )
    private val screenshotParksEventsRepository =
        ScreenshotParksEventsRepository(
            swApi = super.provideParksApi(),
            logger = super.logger,
            crashReporter = super.crashReporter,
            preferencesRepository = super.userPreferencesRepository,
            eventDao = super.eventDao,
            parkDao = super.parkDao,
            userDao = super.userDao,
            demoParks = demoParks
        )
    private val resourcesProvider = ResourcesProviderImpl(appContext)

    override val authRepository: AuthRepository = screenshotAuthRepository
    override val userProfileRepository: UserProfileRepository = screenshotUserProfileRepository
    override val friendsRepository: FriendsRepository = screenshotFriendsRepository
    override val parksEventsRepository: ParksEventsRepository = screenshotParksEventsRepository
    override val countriesRepository: CountriesRepositoryImpl = screenshotCountriesRepository
    override val messagesRepository: MessagesRepositoryImpl = screenshotMessagesRepository
    override val locationService: LocationServiceImpl =
        object : LocationServiceImpl(appContext) {
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
    override val syncParksUseCase: SyncParksUseCase =
        SyncParksUseCase(
            clock = clock,
            userPreferencesRepository = userPreferencesRepository,
            parksEventsRepository = parksEventsRepository,
            logger = logger
        )
    override val syncCountriesUseCase: SyncCountriesUseCase =
        SyncCountriesUseCase(
            clock = clock,
            userPreferencesRepository = userPreferencesRepository,
            countriesRepository = countriesRepository,
            logger = logger,
            analyticsService = analyticsService
        )

    override val getFutureEventsFlowUseCase: GetFutureEventsFlowUseCase =
        GetFutureEventsFlowUseCase(parksEventsRepository)

    override val getPastEventsFlowUseCase: GetPastEventsFlowUseCase =
        GetPastEventsFlowUseCase(parksEventsRepository)

    override val syncFutureEventsUseCase: SyncFutureEventsUseCase =
        SyncFutureEventsUseCase(parksEventsRepository)

    override val syncPastEventsUseCase: SyncPastEventsUseCase =
        SyncPastEventsUseCase(parksEventsRepository)

    override val initializeParksUseCase: InitializeParksUseCase =
        InitializeParksUseCase(appContext, parksEventsRepository, logger)

    override val loginUseCase: LoginUseCase =
        LoginUseCase(
            TokenEncoder(),
            secureTokenRepository,
            authRepository,
            userPreferencesRepository,
            crashReporter
        )

    override fun profileViewModelFactory(): ProfileViewModel =
        ProfileViewModel(
            countriesRepository = countriesRepository,
            authRepository = authRepository,
            userProfileRepository = userProfileRepository,
            friendsRepository = friendsRepository,
            logger = logger,
            userNotifier = userNotifier,
            analyticsService = analyticsService
        )

    override fun dialogsViewModelFactory(): DialogsViewModel =
        DialogsViewModel(
            messagesRepository = messagesRepository,
            logger = logger,
            resources = resourcesProvider,
            messageSentNotifier = messageSentNotifier,
            analyticsService = analyticsService
        )

    override fun searchUserViewModelFactory(): SearchUserViewModel =
        SearchUserViewModel(
            userProfileRepository = userProfileRepository,
            logger = logger,
            analyticsService = analyticsService
        )

    override fun otherUserProfileViewModelFactory(userId: Long): OtherUserProfileViewModel =
        OtherUserProfileViewModel(
            userId = userId,
            countriesRepository = countriesRepository,
            authRepository = authRepository,
            userProfileRepository = userProfileRepository,
            friendsRepository = friendsRepository,
            logger = logger,
            userNotifier = userNotifier,
            resources = resourcesProvider,
            analyticsService = analyticsService
        )
}

private class ScreenshotAuthRepository(
    swApi: SWApi,
    logger: Logger,
    crashReporter: CrashReporter,
    preferencesRepository: com.swparks.data.UserPreferencesRepository,
    userDao: com.swparks.data.database.dao.UserDao,
    dialogDao: com.swparks.data.database.dao.DialogDao
) : AuthRepository(
        swApi = swApi,
        preferencesRepository = preferencesRepository,
        userDao = userDao,
        dialogDao = dialogDao,
        logger = logger,
        crashReporter = crashReporter
    ) {
    private val currentUserFlow = kotlinx.coroutines.flow.MutableStateFlow<com.swparks.data.model.User?>(null)
    private val isAuthorizedFlow = kotlinx.coroutines.flow.MutableStateFlow(false)

    override val isAuthorized = isAuthorizedFlow as kotlinx.coroutines.flow.Flow<Boolean>

    override fun getCurrentUserFlow() = currentUserFlow

    override suspend fun login(token: String?): Result<com.swparks.data.model.LoginSuccess> {
        currentUserFlow.value = DemoData.demoAuthorizedUser
        isAuthorizedFlow.value = true
        return Result.success(
            com.swparks.data.model
                .LoginSuccess(userId = DemoData.demoAuthorizedUser.id)
        )
    }

    override suspend fun forceLogout() {
        currentUserFlow.value = null
        isAuthorizedFlow.value = false
    }

    override suspend fun clearUserData() {
        currentUserFlow.value = null
        isAuthorizedFlow.value = false
    }
}

private class ScreenshotUserProfileRepository(
    swApi: SWApi,
    logger: Logger,
    crashReporter: CrashReporter,
    preferencesRepository: com.swparks.data.UserPreferencesRepository,
    userDao: com.swparks.data.database.dao.UserDao
) : UserProfileRepository(
        swApi = swApi,
        preferencesRepository = preferencesRepository,
        userDao = userDao,
        logger = logger,
        crashReporter = crashReporter
    ) {
    override suspend fun getUser(userId: Long): Result<com.swparks.data.model.User> {
        val user =
            when (userId) {
                DemoData.demoAuthorizedUser.id -> DemoData.demoAuthorizedUser
                DemoData.demoUser.id -> DemoData.demoUser
                DemoData.demoSearchUser.id -> DemoData.demoSearchUser
                else -> null
            } ?: return Result.failure(IllegalArgumentException("User not found: $userId"))
        return Result.success(user)
    }

    override suspend fun getSocialUpdates(userId: Long): Result<com.swparks.data.model.SocialUpdates> {
        val user = DemoData.demoAuthorizedUser
        return Result.success(
            com.swparks.data.model.SocialUpdates(
                user = user,
                friends = emptyList(),
                friendRequests = emptyList(),
                blacklist = emptyList()
            )
        )
    }

    override suspend fun findUsers(name: String): Result<List<com.swparks.data.model.User>> = Result.success(DemoData.searchUsers(name))
}

private class ScreenshotFriendsRepository(
    swApi: SWApi,
    logger: Logger,
    crashReporter: CrashReporter,
    userDao: com.swparks.data.database.dao.UserDao
) : FriendsRepository(
        swApi = swApi,
        userDao = userDao,
        logger = logger,
        crashReporter = crashReporter
    ) {
    private val friendsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<com.swparks.data.model.User>>(emptyList())
    private val friendRequestsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<com.swparks.data.model.User>>(emptyList())
    private val blacklistFlow = kotlinx.coroutines.flow.MutableStateFlow<List<com.swparks.data.model.User>>(emptyList())

    override fun getFriendsFlow() = friendsFlow

    override fun getFriendRequestsFlow() = friendRequestsFlow

    override fun getBlacklistFlow() = blacklistFlow

    override fun getFriendsCountFlow() = kotlinx.coroutines.flow.flowOf(DemoData.demoAuthorizedUser.friendsCount ?: 0)
}

private class ScreenshotParksEventsRepository(
    swApi: SWApi,
    logger: Logger,
    crashReporter: CrashReporter,
    preferencesRepository: com.swparks.data.UserPreferencesRepository,
    eventDao: com.swparks.data.database.dao.EventDao,
    parkDao: com.swparks.data.database.dao.ParkDao,
    userDao: com.swparks.data.database.dao.UserDao,
    demoParks: List<com.swparks.data.model.Park>
) : ParksEventsRepository(
        swApi = swApi,
        preferencesRepository = preferencesRepository,
        eventDao = eventDao,
        parkDao = parkDao,
        userDao = userDao,
        logger = logger,
        crashReporter = crashReporter
    ) {
    private val parksFlow = kotlinx.coroutines.flow.MutableStateFlow(demoParks)
    private val futureEventsFlow = kotlinx.coroutines.flow.MutableStateFlow(DemoData.demoFutureEvents)
    private val pastEventsFlow = kotlinx.coroutines.flow.MutableStateFlow(DemoData.demoPastEvents)

    override fun getParksFlow() = parksFlow

    override fun getFutureEventsFlow() = futureEventsFlow

    override fun getPastEventsFlow() = pastEventsFlow

    override suspend fun getAllParks(): Result<List<com.swparks.data.model.Park>> = Result.success(parksFlow.value)

    override suspend fun getPark(id: Long): Result<com.swparks.data.model.Park> {
        val fallback = parksFlow.value.firstOrNull { it.id == id }
        val park = DemoData.parkDetailsById(id, fallback)
        if (fallback == null && park.id != id) {
            return Result.failure(
                com.swparks.domain.exception.NotFoundException
                    .ParkNotFound(id)
            )
        }
        return Result.success(park)
    }

    override suspend fun getParkFromCache(parkId: Long): com.swparks.data.model.Park? {
        val fallback = parksFlow.value.firstOrNull { it.id == parkId }
        return if (fallback != null) DemoData.parkDetailsById(parkId, fallback) else null
    }

    override suspend fun getParksForUser(userId: Long): Result<List<com.swparks.data.model.Park>> =
        Result.success(DemoData.demoParksForUser(parksFlow.value))

    override suspend fun importSeedParks(context: android.content.Context) = Unit

    override suspend fun upsertParks(parks: List<com.swparks.data.model.Park>) {
        parksFlow.value = if (parks.isEmpty()) parksFlow.value else parks
    }

    override suspend fun cachePark(park: com.swparks.data.model.Park) {
        parksFlow.value =
            parksFlow.value
                .filterNot { it.id == park.id }
                .plus(park)
                .sortedBy { it.id }
    }

    override suspend fun getCachedParksForUser(userId: Long): List<com.swparks.data.model.Park>? =
        DemoData.demoParksForUser(parksFlow.value)

    override suspend fun hasCachedParksForUser(userId: Long): Boolean = true

    override suspend fun getUpdatedParks(date: String): Result<List<com.swparks.data.model.Park>> = Result.success(parksFlow.value)

    override suspend fun getEvents(type: com.swparks.ui.model.EventType): Result<List<com.swparks.data.model.Event>> =
        when (type) {
            com.swparks.ui.model.EventType.FUTURE -> Result.success(futureEventsFlow.value)
            com.swparks.ui.model.EventType.PAST -> Result.success(pastEventsFlow.value)
        }

    override suspend fun getEvent(id: Long): Result<com.swparks.data.model.Event> {
        val allEvents = futureEventsFlow.value + pastEventsFlow.value
        val event =
            allEvents.firstOrNull { it.id == id }
                ?: return Result.failure(
                    com.swparks.domain.exception.NotFoundException
                        .EventNotFound(id)
                )
        return Result.success(event)
    }

    override suspend fun deleteEvent(eventId: Long): Result<Unit> {
        futureEventsFlow.value = futureEventsFlow.value.filterNot { it.id == eventId }
        pastEventsFlow.value = pastEventsFlow.value.filterNot { it.id == eventId }
        return Result.success(Unit)
    }

    override suspend fun changeIsGoingToEvent(
        go: Boolean,
        eventId: Long
    ): Result<Unit> {
        val update = { event: com.swparks.data.model.Event ->
            if (event.id == eventId) event.copy(trainHere = go) else event
        }
        futureEventsFlow.value = futureEventsFlow.value.map(update)
        pastEventsFlow.value = pastEventsFlow.value.map(update)
        return Result.success(Unit)
    }

    override suspend fun syncFutureEvents(): Result<Unit> = Result.success(Unit)

    override suspend fun syncPastEvents(): Result<Unit> = Result.success(Unit)
}

private class ScreenshotCountriesRepository(
    appContext: Context,
    private val countries: List<Country>,
    delegate: DefaultAppContainer
) : CountriesRepositoryImpl(
        context = appContext,
        swApi = delegate.provideMessagesApi(),
        logger = delegate.logger
    ) {
    private val countriesFlow = MutableStateFlow(countries)
    private val citiesById = countries.flatMap { it.cities }.associateBy { it.id }
    private val countriesById = countries.associateBy { it.id }

    override fun ensureCountriesLoaded() = Unit

    override fun getCountriesFlow(): Flow<List<com.swparks.data.model.Country>> = countriesFlow

    override suspend fun getCountryById(countryId: String): com.swparks.data.model.Country? = countriesById[countryId]

    override suspend fun getCityById(cityId: String): com.swparks.data.model.City? = citiesById[cityId]

    override suspend fun getCitiesByCountry(countryId: String): List<com.swparks.data.model.City> =
        getCountryById(countryId)?.cities.orEmpty()

    override suspend fun getAllCities(): List<com.swparks.data.model.City> = citiesById.values.toList()

    override suspend fun getCountryForCity(cityId: String): com.swparks.data.model.Country? {
        val city = getCityById(cityId) ?: return null
        return countries.firstOrNull { country ->
            country.cities.any { it.id == city.id }
        }
    }

    override suspend fun updateCountriesFromServer(): Result<Unit> = Result.success(Unit)
}

private class ScreenshotMessagesRepository(
    swApi: SWApi,
    logger: Logger,
    crashReporter: CrashReporter
) : MessagesRepositoryImpl(
        swApi = swApi,
        logger = logger,
        crashReporter = crashReporter
    ) {
    override val dialogs: Flow<List<com.swparks.data.database.entity.DialogEntity>> =
        MutableStateFlow(emptyList())

    override suspend fun refreshDialogs(): Result<Unit> = Result.success(Unit)
}
