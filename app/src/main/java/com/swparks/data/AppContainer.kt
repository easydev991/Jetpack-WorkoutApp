package com.swparks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.swparks.BuildConfig
import com.swparks.analytics.AnalyticsService
import com.swparks.analytics.FirebaseAnalyticsProvider
import com.swparks.data.crypto.CryptoManagerImpl
import com.swparks.data.database.SWDatabase
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.EventDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.ParkDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.database.dao.UserTrainingParkDao
import com.swparks.data.interceptor.AuthInterceptor
import com.swparks.data.interceptor.LoggingInterceptor
import com.swparks.data.interceptor.RetryInterceptor
import com.swparks.data.interceptor.TokenInterceptor
import com.swparks.data.model.Park
import com.swparks.data.preferences.ParksFilterDataStore
import com.swparks.data.provider.AvatarHelperImpl
import com.swparks.data.provider.GeocodingServiceImpl
import com.swparks.data.provider.LocationServiceImpl
import com.swparks.data.provider.ResourcesProviderImpl
import com.swparks.data.repository.CountriesRepositoryImpl
import com.swparks.data.repository.JournalEntriesRepositoryImpl
import com.swparks.data.repository.JournalsRepositoryImpl
import com.swparks.data.repository.MessagesRepositoryImpl
import com.swparks.data.repository.SWRepository
import com.swparks.data.repository.SWRepositoryImp
import com.swparks.data.serializer.EncryptedStringSerializer
import com.swparks.data.util.SystemClock
import com.swparks.domain.event.MessageSentNotifier
import com.swparks.domain.usecase.CanDeleteJournalEntryUseCase
import com.swparks.domain.usecase.ChangePasswordUseCase
import com.swparks.domain.usecase.CreateEventUseCase
import com.swparks.domain.usecase.CreateJournalUseCase
import com.swparks.domain.usecase.DefaultCreateParkLocationHandler
import com.swparks.domain.usecase.DeleteEventUseCase
import com.swparks.domain.usecase.DeleteJournalEntryUseCase
import com.swparks.domain.usecase.DeleteJournalUseCase
import com.swparks.domain.usecase.DeleteUserUseCase
import com.swparks.domain.usecase.EditEventUseCase
import com.swparks.domain.usecase.EditJournalSettingsUseCase
import com.swparks.domain.usecase.FilterParksUseCase
import com.swparks.domain.usecase.FindCityByCoordinatesUseCase
import com.swparks.domain.usecase.GetFutureEventsFlowUseCase
import com.swparks.domain.usecase.GetJournalEntriesUseCase
import com.swparks.domain.usecase.GetJournalsUseCase
import com.swparks.domain.usecase.GetPastEventsFlowUseCase
import com.swparks.domain.usecase.ICreateParkLocationHandler
import com.swparks.domain.usecase.InitializeParksUseCase
import com.swparks.domain.usecase.LoginUseCase
import com.swparks.domain.usecase.LogoutUseCase
import com.swparks.domain.usecase.ResetPasswordUseCase
import com.swparks.domain.usecase.SyncCountriesUseCase
import com.swparks.domain.usecase.SyncFutureEventsUseCase
import com.swparks.domain.usecase.SyncJournalEntriesUseCase
import com.swparks.domain.usecase.SyncJournalsUseCase
import com.swparks.domain.usecase.SyncParksUseCase
import com.swparks.domain.usecase.SyncPastEventsUseCase
import com.swparks.domain.usecase.TextEntryUseCase
import com.swparks.network.SWApi
import com.swparks.ui.model.EventFormMode
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.viewmodel.BlacklistViewModel
import com.swparks.ui.viewmodel.ChangePasswordViewModel
import com.swparks.ui.viewmodel.ChatViewModel
import com.swparks.ui.viewmodel.DialogsViewModel
import com.swparks.ui.viewmodel.EditProfileViewModel
import com.swparks.ui.viewmodel.EventDetailViewModel
import com.swparks.ui.viewmodel.EventFormViewModel
import com.swparks.ui.viewmodel.EventsViewModel
import com.swparks.ui.viewmodel.FriendsListViewModel
import com.swparks.ui.viewmodel.JournalEntriesDeps
import com.swparks.ui.viewmodel.JournalEntriesViewModel
import com.swparks.ui.viewmodel.JournalsViewModel
import com.swparks.ui.viewmodel.OtherUserProfileViewModel
import com.swparks.ui.viewmodel.ParkFormViewModel
import com.swparks.ui.viewmodel.ProfileViewModel
import com.swparks.ui.viewmodel.RegisterViewModel
import com.swparks.ui.viewmodel.SearchUserViewModel
import com.swparks.ui.viewmodel.TextEntryViewModel
import com.swparks.ui.viewmodel.UserAddedParksViewModel
import com.swparks.ui.viewmodel.UserFriendsViewModel
import com.swparks.ui.viewmodel.UserTrainingParksViewModel
import com.swparks.util.AndroidLogger
import com.swparks.util.CrashReporter
import com.swparks.util.Logger
import com.swparks.util.NoOpLogger
import com.swparks.util.UserNotifier
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Таймауты для сетевых запросов
 */
private object NetworkTimeouts {
    const val CONNECT_SECONDS = 15L
    const val READ_SECONDS = 30L
    const val WRITE_SECONDS = 30L
    const val CALL_SECONDS = 60L
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Suppress("TooManyFunctions")
open class DefaultAppContainer(
    context: Context
) {
    private val appContext: Context = context.applicationContext
    private val baseUrl = "https://workout.su/api/v3/"
    private val jsonFactory =
        Json {
            isLenient = true
            ignoreUnknownKeys = true
        }

    val logger: Logger = if (BuildConfig.DEBUG) AndroidLogger() else NoOpLogger()
    val userNotifier: UserNotifier = UserNotifier(logger)
    val crashReporter: CrashReporter = com.swparks.util.crash.FirebaseCrashReporter
    val analyticsService: AnalyticsService by lazy {
        if (BuildConfig.DEBUG) {
            AnalyticsService(emptyList(), logger)
        } else {
            val firebase = FirebaseAnalyticsProvider(appContext, logger, crashReporter)
            AnalyticsService(listOf(firebase::log), logger)
        }
    }
    val messageSentNotifier: MessageSentNotifier = MessageSentNotifier()
    val clock: com.swparks.domain.util.Clock by lazy { SystemClock() }

    // ==================== Location & Geocoding Services ====================

    open val locationService: LocationServiceImpl by lazy {
        LocationServiceImpl(appContext)
    }

    val geocodingService: GeocodingServiceImpl by lazy {
        GeocodingServiceImpl(appContext)
    }

    val findCityByCoordinatesUseCase: FindCityByCoordinatesUseCase by lazy {
        FindCityByCoordinatesUseCase(countriesRepository)
    }

    val createParkLocationHandler: ICreateParkLocationHandler by lazy {
        DefaultCreateParkLocationHandler(locationService, userNotifier)
    }

    // ==================== Parks Filter ====================

    val parksFilterDataStore: ParksFilterDataStore by lazy {
        ParksFilterDataStore(appContext)
    }

    val filterParksUseCase: FilterParksUseCase by lazy {
        FilterParksUseCase()
    }

    // ==================== Sync Use Cases ====================

    open val syncParksUseCase: SyncParksUseCase by lazy {
        SyncParksUseCase(clock, userPreferencesRepository, swRepository, logger)
    }

    open val syncCountriesUseCase: SyncCountriesUseCase by lazy {
        SyncCountriesUseCase(clock, userPreferencesRepository, countriesRepository, logger, analyticsService)
    }

    open val initializeParksUseCase: InitializeParksUseCase by lazy {
        InitializeParksUseCase(appContext, swRepository, logger)
    }

    // ==================== Resources Provider ====================

    /**
     * Провайдер для доступа к строковым ресурсам
     * Используется в ViewModel для локализации без зависимости от Context
     */
    private val resourcesProvider: ResourcesProviderImpl by lazy {
        ResourcesProviderImpl(appContext)
    }

    // ==================== Avatar Helper ====================

    /**
     * Хелпер для работы с аватарами (изображениями)
     * Используется в ViewModel для работы с Uri без зависимости от Context
     */
    private val avatarHelper: AvatarHelperImpl by lazy {
        AvatarHelperImpl(appContext)
    }

    // ==================== Room Database ====================

    /**
     * Room Database для локального хранения данных
     * Использует миграции для сохранения данных при обновлении схемы
     */
    val database: SWDatabase by lazy {
        Room
            .databaseBuilder(
                appContext,
                SWDatabase::class.java,
                "sw_database"
            ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /**
     * DAO для работы с пользователями
     */
    val userDao: UserDao by lazy { database.userDao() }

    /**
     * DAO для работы с дневниками
     */
    private val journalDao: JournalDao by lazy { database.journalDao() }

    /**
     * DAO для работы с диалогами
     */
    private val dialogDao: DialogDao by lazy { database.dialogDao() }

    /**
     * DAO для работы с мероприятиями
     */
    private val eventDao: EventDao by lazy { database.eventDao() }

    /**
     * DAO для работы с площадками
     */
    val parkDao: ParkDao by lazy { database.parkDao() }

    /**
     * DAO для работы с тренировочными площадками пользователя
     */
    private val userTrainingParkDao: UserTrainingParkDao by lazy { database.userTrainingParkDao() }

    // ==================== Криптография и хранение токена ====================

    // Создаем CryptoManager для шифрования токена
    private val cryptoManager: CryptoManagerImpl by lazy {
        CryptoManagerImpl(appContext)
    }

    // Создаем EncryptedStringSerializer для шифрования/дешифрования токена
    private val encryptedStringSerializer: EncryptedStringSerializer by lazy {
        EncryptedStringSerializer(cryptoManager)
    }

    // Создаем SecureTokenRepository для безопасного хранения токена
    val secureTokenRepository: SecureTokenRepository by lazy {
        SecureTokenRepository(appContext.dataStore, encryptedStringSerializer)
    }

    // Создаем UserPreferencesRepository для использования в AuthInterceptor
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext.dataStore)
    }

    // ==================== Interceptors ====================

    // Создаем RetryInterceptor для автоматического повторения запросов при временных ошибках
    private val retryInterceptor: RetryInterceptor by lazy {
        RetryInterceptor(logger)
    }

    // Создаем TokenInterceptor для добавления токена в заголовки
    private val tokenInterceptor: TokenInterceptor by lazy {
        TokenInterceptor(secureTokenRepository)
    }

    // Создаем AuthInterceptor для обработки ошибок 401
    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(userPreferencesRepository)
    }

    // Создаем OkHttpClient с interceptor chain
    // Порядок важен: logging → retry → token → auth
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            // Таймауты для сетевых операций
            .connectTimeout(NetworkTimeouts.CONNECT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkTimeouts.READ_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkTimeouts.WRITE_SECONDS, TimeUnit.SECONDS)
            .callTimeout(NetworkTimeouts.CALL_SECONDS, TimeUnit.SECONDS)
            // Interceptors
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(LoggingInterceptor())
                }
            }.addInterceptor(retryInterceptor) // ← ОБЯЗАТЕЛЬНО ПЕРВЫМ после logging!
            .addInterceptor(tokenInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    private val retrofit: Retrofit =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(jsonFactory.asConverterFactory("application/json".toMediaType()))
            .build()

    // Единый экземпляр SWApi для всех API запросов
    private val retrofitService: SWApi by lazy {
        retrofit.create(SWApi::class.java)
    }

    open val swRepository: SWRepository by lazy {
        SWRepositoryImp(
            swApi = retrofitService,
            dataStore = appContext.dataStore,
            userDao = userDao,
            journalDao = journalDao,
            journalEntryDao = journalEntryDao,
            dialogDao = dialogDao,
            eventDao = eventDao,
            parkDao = parkDao,
            crashReporter = crashReporter,
            logger = logger,
            userTrainingParkDao = userTrainingParkDao
        )
    }

// ==================== Справочник стран и городов ====================

    open val countriesRepository: CountriesRepositoryImpl by lazy {
        CountriesRepositoryImpl(context = appContext, swApi = retrofitService, logger = logger)
    }

    val journalsRepository: JournalsRepositoryImpl by lazy {
        JournalsRepositoryImpl(
            swApi = retrofitService,
            journalDao = journalDao,
            crashReporter = crashReporter,
            logger = logger
        )
    }

    /**
     * DAO для работы с записями дневника
     */
    private val journalEntryDao: JournalEntryDao by lazy { database.journalEntryDao() }

    /**
     * Репозиторий для работы с записями дневника
     * Примечание: репозиторий не зависит от конкретных userId или journalId,
     * эти параметры передаются в методах репозитория
     */
    val journalEntriesRepository: JournalEntriesRepositoryImpl by lazy {
        JournalEntriesRepositoryImpl(
            swApi = retrofitService,
            journalEntryDao = journalEntryDao,
            crashReporter = crashReporter,
            logger = logger
        )
    }

    /**
     * Репозиторий для работы с диалогами
     */
    open val messagesRepository: MessagesRepositoryImpl by lazy {
        MessagesRepositoryImpl(
            dialogsDao = dialogDao,
            swApi = retrofitService,
            logger = logger,
            crashReporter = crashReporter
        )
    }

    // ==================== Use cases для авторизации ====================

    // Создаем TokenEncoder для генерации токена
    private val tokenEncoder: TokenEncoder by lazy {
        TokenEncoder()
    }

    open val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(
            tokenEncoder,
            secureTokenRepository,
            swRepository,
            userPreferencesRepository,
            crashReporter
        )
    }

    val logoutUseCase: LogoutUseCase by lazy {
        LogoutUseCase(
            secureTokenRepository,
            swRepository,
            crashReporter
        )
    }

    val resetPasswordUseCase: ResetPasswordUseCase by lazy {
        ResetPasswordUseCase(swRepository)
    }

    val changePasswordUseCase: ChangePasswordUseCase by lazy {
        ChangePasswordUseCase(swRepository, secureTokenRepository, tokenEncoder)
    }

    val deleteUserUseCase: DeleteUserUseCase by lazy {
        DeleteUserUseCase(secureTokenRepository, swRepository)
    }

    // ==================== Use cases для дневников ====================

    val getJournalsUseCase: GetJournalsUseCase by lazy {
        GetJournalsUseCase(journalsRepository)
    }

    val syncJournalsUseCase: SyncJournalsUseCase by lazy {
        SyncJournalsUseCase(journalsRepository)
    }

    // ==================== Use cases для записей дневника ====================

    // Примечание: Use Case'ы являются stateless-компонентами и не зависят от конкретных
    // userId и journalId при создании. Эти параметры передаются в методах invoke() Use Case'ов.

    val getJournalEntriesUseCase: GetJournalEntriesUseCase by lazy {
        GetJournalEntriesUseCase(journalEntriesRepository)
    }

    val syncJournalEntriesUseCase: SyncJournalEntriesUseCase by lazy {
        SyncJournalEntriesUseCase(journalEntriesRepository)
    }
    val deleteJournalEntryUseCase: DeleteJournalEntryUseCase by lazy {
        DeleteJournalEntryUseCase(journalEntriesRepository)
    }
    val canDeleteJournalEntryUseCase: CanDeleteJournalEntryUseCase by lazy {
        CanDeleteJournalEntryUseCase(journalEntriesRepository)
    }
    val deleteJournalUseCase: DeleteJournalUseCase by lazy {
        DeleteJournalUseCase(swRepository)
    }
    val editJournalSettingsUseCase: EditJournalSettingsUseCase by lazy {
        EditJournalSettingsUseCase(swRepository)
    }
    val createJournalUseCase: CreateJournalUseCase by lazy {
        CreateJournalUseCase(swRepository)
    }
    val textEntryUseCase: TextEntryUseCase by lazy {
        TextEntryUseCase(swRepository, createJournalUseCase, messageSentNotifier)
    }

    // ==================== Use cases для мероприятий ====================

    open val getFutureEventsFlowUseCase: GetFutureEventsFlowUseCase by lazy {
        GetFutureEventsFlowUseCase(swRepository)
    }
    open val syncFutureEventsUseCase: SyncFutureEventsUseCase by lazy {
        SyncFutureEventsUseCase(swRepository)
    }
    open val getPastEventsFlowUseCase: GetPastEventsFlowUseCase by lazy {
        GetPastEventsFlowUseCase(swRepository)
    }
    open val syncPastEventsUseCase: SyncPastEventsUseCase by lazy {
        SyncPastEventsUseCase(swRepository)
    }
    val createEventUseCase: CreateEventUseCase by lazy {
        CreateEventUseCase(swRepository)
    }
    val editEventUseCase: EditEventUseCase by lazy {
        EditEventUseCase(swRepository)
    }

    /** Factory метод для создания ProfileViewModel */
    open fun profileViewModelFactory() =
        ProfileViewModel(
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            logger = logger,
            userNotifier = userNotifier,
            analyticsService = analyticsService
        )

    /** Factory метод для создания FriendsListViewModel */
    fun friendsListViewModelFactory() =
        FriendsListViewModel(
            userDao = userDao,
            swRepository = swRepository,
            logger = logger,
            userNotifier = userNotifier,
            analyticsService = analyticsService
        )

    /** Factory метод для создания UserFriendsViewModel */
    fun userFriendsViewModelFactory(userId: Long) =
        UserFriendsViewModel(
            userId = userId,
            swRepository = swRepository,
            logger = logger,
            userNotifier = userNotifier
        )

    /** Factory метод для создания BlacklistViewModel */
    fun blacklistViewModelFactory() =
        BlacklistViewModel(
            swRepository = swRepository,
            logger = logger,
            userNotifier = userNotifier,
            analyticsService = analyticsService
        )

    /** Factory метод для создания UserTrainingParksViewModel */
    fun userTrainingParksViewModelFactory(userId: Long) =
        UserTrainingParksViewModel(
            swRepository = swRepository,
            userId = userId,
            logger = logger,
            userNotifier = userNotifier
        )

    fun userAddedParksViewModelFactory(
        userId: Long,
        seedParks: List<Park>?,
        requiresFetch: Boolean
    ) = UserAddedParksViewModel(
        swRepository = swRepository,
        userId = userId,
        seedParks = seedParks,
        requiresFetch = requiresFetch,
        logger = logger,
        userNotifier = userNotifier
    )

    /** Factory метод для создания JournalsViewModel */
    fun journalsViewModelFactory(userId: Long) =
        JournalsViewModel(
            userId = userId,
            getJournalsUseCase = getJournalsUseCase,
            syncJournalsUseCase = syncJournalsUseCase,
            deleteJournalUseCase = deleteJournalUseCase,
            editJournalSettingsUseCase = editJournalSettingsUseCase,
            userNotifier = userNotifier,
            resources = resourcesProvider,
            analyticsService = analyticsService
        )

    /** Factory метод для создания JournalEntriesViewModel */
    fun journalEntriesViewModelFactory(
        journalOwnerId: Long,
        journalId: Long,
        savedStateHandle: SavedStateHandle
    ) = JournalEntriesViewModel(
        journalOwnerId = journalOwnerId,
        journalId = journalId,
        deps =
            JournalEntriesDeps(
                getJournalEntriesUseCase = getJournalEntriesUseCase,
                syncJournalEntriesUseCase = syncJournalEntriesUseCase,
                deleteJournalEntryUseCase = deleteJournalEntryUseCase,
                canDeleteJournalEntryUseCase = canDeleteJournalEntryUseCase,
                editJournalSettingsUseCase = editJournalSettingsUseCase,
                userPreferencesRepository = userPreferencesRepository,
                swRepository = swRepository,
                savedStateHandle = savedStateHandle,
                userNotifier = userNotifier,
                resources = resourcesProvider,
                analyticsService = analyticsService
            )
    )

    /** Factory метод для создания TextEntryViewModel */
    fun textEntryViewModelFactory(mode: TextEntryMode) =
        TextEntryViewModel(
            textEntryUseCase = textEntryUseCase,
            userNotifier = userNotifier,
            mode = mode,
            context = appContext
        )

    /** Factory метод для создания DialogsViewModel */
    open fun dialogsViewModelFactory() =
        DialogsViewModel(
            messagesRepository = messagesRepository,
            swRepository = swRepository,
            logger = logger,
            resources = resourcesProvider,
            messageSentNotifier = messageSentNotifier,
            analyticsService = analyticsService
        )

    /** Factory метод для создания ChatViewModel */
    fun chatViewModelFactory() =
        ChatViewModel(
            swApi = provideMessagesApi(),
            swRepository = swRepository,
            userNotifier = userNotifier,
            logger = logger,
            crashReporter = crashReporter,
            analyticsService = analyticsService
        )

    /** Factory метод для создания SearchUserViewModel */
    open fun searchUserViewModelFactory() =
        SearchUserViewModel(
            swRepository = swRepository,
            logger = logger,
            analyticsService = analyticsService
        )

    /** Factory метод для создания OtherUserProfileViewModel */
    open fun otherUserProfileViewModelFactory(userId: Long) =
        OtherUserProfileViewModel(
            userId = userId,
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            logger = logger,
            userNotifier = userNotifier,
            resources = resourcesProvider,
            analyticsService = analyticsService
        )

    /** Factory метод для создания EditProfileViewModel */
    fun editProfileViewModelFactory() =
        EditProfileViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            deleteUserUseCase = deleteUserUseCase,
            avatarHelper = avatarHelper,
            logger = logger,
            userNotifier = userNotifier,
            resources = resourcesProvider,
            analyticsService = analyticsService
        )

    /** Factory метод для создания ChangePasswordViewModel */
    fun changePasswordViewModelFactory() =
        ChangePasswordViewModel(
            changePasswordUseCase = changePasswordUseCase,
            logger = logger,
            userNotifier = userNotifier,
            resources = resourcesProvider,
            analyticsService = analyticsService
        )

    /** Factory метод для создания RegisterViewModel */
    fun registerViewModelFactory() =
        RegisterViewModel(
            logger = logger,
            swRepository = swRepository,
            secureTokenRepository = secureTokenRepository,
            userPreferencesRepository = userPreferencesRepository,
            tokenEncoder = tokenEncoder,
            countriesRepository = countriesRepository,
            resources = resourcesProvider,
            userNotifier = userNotifier
        )

    /** Factory метод для создания EventsViewModel */
    fun eventsViewModelFactory() =
        EventsViewModel(
            getFutureEventsFlowUseCase = getFutureEventsFlowUseCase,
            syncFutureEventsUseCase = syncFutureEventsUseCase,
            getPastEventsFlowUseCase = getPastEventsFlowUseCase,
            syncPastEventsUseCase = syncPastEventsUseCase,
            userPreferencesRepository = userPreferencesRepository,
            countriesRepository = countriesRepository,
            userNotifier = userNotifier,
            logger = logger,
            swRepository = swRepository,
            analyticsService = analyticsService
        )

    /** Factory метод для создания EventDetailViewModel */
    fun eventDetailViewModelFactory(savedStateHandle: SavedStateHandle) =
        EventDetailViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            userPreferencesRepository = userPreferencesRepository,
            savedStateHandle = savedStateHandle,
            userNotifier = userNotifier,
            logger = logger,
            deleteEventUseCase = DeleteEventUseCase(swRepository),
            resourcesProvider = resourcesProvider,
            analyticsService = analyticsService
        )

    /** Factory метод для создания EventFormViewModel */
    fun eventFormViewModelFactory(mode: EventFormMode) =
        EventFormViewModel(
            mode = mode,
            createEventUseCase = createEventUseCase,
            editEventUseCase = editEventUseCase,
            avatarHelper = avatarHelper,
            logger = logger,
            userNotifier = userNotifier,
            analyticsService = analyticsService
        )

    /** Factory метод для создания ParkFormViewModel */
    fun parkFormViewModelFactory(mode: com.swparks.ui.model.ParkFormMode) =
        ParkFormViewModel(
            mode = mode,
            swRepository = swRepository,
            avatarHelper = avatarHelper,
            logger = logger,
            userNotifier = userNotifier,
            geocodingService = geocodingService,
            findCityByCoordinatesUseCase = findCityByCoordinatesUseCase,
            userDao = userDao,
            analyticsService = analyticsService
        )

    // ==================== API клиенты для разных функциональных областей ====================
    // Все фабричные методы возвращают один и тот же экземпляр SWApi для консистентности
    // Разделение по областям обеспечивает лучшую организацию кода и гибкость для будущего рефакторинга

    fun provideAuthApi(): SWApi = retrofitService

    fun provideProfileApi(): SWApi = retrofitService

    fun provideFriendsApi(): SWApi = retrofitService

    fun provideParksApi(): SWApi = retrofitService

    fun provideEventsApi(): SWApi = retrofitService

    fun provideMessagesApi(): SWApi = retrofitService

    fun provideJournalsApi(): SWApi = retrofitService
}
