package com.swparks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.swparks.data.crypto.CryptoManager
import com.swparks.data.crypto.CryptoManagerImpl
import com.swparks.data.database.SWDatabase
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.EventDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.UserDao
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
import com.swparks.domain.event.MessageSentNotifier
import com.swparks.domain.provider.AvatarHelper
import com.swparks.domain.provider.GeocodingService
import com.swparks.domain.provider.LocationService
import com.swparks.domain.provider.ResourcesProvider
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.repository.JournalEntriesRepository
import com.swparks.domain.repository.JournalsRepository
import com.swparks.domain.repository.MessagesRepository
import com.swparks.domain.usecase.CanDeleteJournalEntryUseCase
import com.swparks.domain.usecase.ChangePasswordUseCase
import com.swparks.domain.usecase.CreateEventUseCase
import com.swparks.domain.usecase.CreateJournalUseCase
import com.swparks.domain.usecase.DefaultCreateParkLocationHandler
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
import com.swparks.domain.usecase.ICanDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IChangePasswordUseCase
import com.swparks.domain.usecase.ICreateEventUseCase
import com.swparks.domain.usecase.ICreateJournalUseCase
import com.swparks.domain.usecase.ICreateParkLocationHandler
import com.swparks.domain.usecase.IDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IDeleteJournalUseCase
import com.swparks.domain.usecase.IDeleteUserUseCase
import com.swparks.domain.usecase.IEditEventUseCase
import com.swparks.domain.usecase.IEditJournalSettingsUseCase
import com.swparks.domain.usecase.IFilterParksUseCase
import com.swparks.domain.usecase.IFindCityByCoordinatesUseCase
import com.swparks.domain.usecase.IGetFutureEventsFlowUseCase
import com.swparks.domain.usecase.IGetJournalEntriesUseCase
import com.swparks.domain.usecase.IGetJournalsUseCase
import com.swparks.domain.usecase.IGetPastEventsFlowUseCase
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.ILogoutUseCase
import com.swparks.domain.usecase.IResetPasswordUseCase
import com.swparks.domain.usecase.ISyncFutureEventsUseCase
import com.swparks.domain.usecase.ISyncJournalEntriesUseCase
import com.swparks.domain.usecase.ISyncJournalsUseCase
import com.swparks.domain.usecase.ISyncPastEventsUseCase
import com.swparks.domain.usecase.ITextEntryUseCase
import com.swparks.domain.usecase.LoginUseCase
import com.swparks.domain.usecase.LogoutUseCase
import com.swparks.domain.usecase.ResetPasswordUseCase
import com.swparks.domain.usecase.SyncFutureEventsUseCase
import com.swparks.domain.usecase.SyncJournalEntriesUseCase
import com.swparks.domain.usecase.SyncJournalsUseCase
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
import com.swparks.util.UserNotifier
import com.swparks.util.UserNotifierImpl
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

@Suppress("TooManyFunctions")
interface AppContainer {
    val swRepository: SWRepository
    val secureTokenRepository: SecureTokenRepository
    val countriesRepository: CountriesRepository
    val journalsRepository: JournalsRepository
    val journalEntriesRepository: JournalEntriesRepository
    val messagesRepository: MessagesRepository
    val userPreferencesRepository: UserPreferencesRepository

    // Сервисы для обработки ошибок
    val logger: Logger
    val userNotifier: UserNotifier
    val crashReporter: CrashReporter

    // Event notifiers
    val messageSentNotifier: MessageSentNotifier

    // Location & Geocoding services
    val locationService: LocationService
    val geocodingService: GeocodingService
    val findCityByCoordinatesUseCase: IFindCityByCoordinatesUseCase
    val createParkLocationHandler: ICreateParkLocationHandler

    // Parks filter
    val filterParksUseCase: IFilterParksUseCase
    val parksFilterDataStore: ParksFilterDataStore

    // Use cases для авторизации
    val loginUseCase: ILoginUseCase
    val logoutUseCase: ILogoutUseCase
    val resetPasswordUseCase: IResetPasswordUseCase
    val changePasswordUseCase: IChangePasswordUseCase
    val deleteUserUseCase: IDeleteUserUseCase

    // Use cases для дневников
    val getJournalsUseCase: IGetJournalsUseCase
    val syncJournalsUseCase: ISyncJournalsUseCase
    val deleteJournalUseCase: IDeleteJournalUseCase
    val editJournalSettingsUseCase: IEditJournalSettingsUseCase
    val getJournalEntriesUseCase: IGetJournalEntriesUseCase
    val syncJournalEntriesUseCase: ISyncJournalEntriesUseCase
    val deleteJournalEntryUseCase: IDeleteJournalEntryUseCase
    val canDeleteJournalEntryUseCase: ICanDeleteJournalEntryUseCase
    val textEntryUseCase: ITextEntryUseCase

    // Use cases для мероприятий
    val getFutureEventsFlowUseCase: IGetFutureEventsFlowUseCase
    val syncFutureEventsUseCase: ISyncFutureEventsUseCase
    val getPastEventsFlowUseCase: IGetPastEventsFlowUseCase
    val syncPastEventsUseCase: ISyncPastEventsUseCase
    val createEventUseCase: ICreateEventUseCase
    val editEventUseCase: IEditEventUseCase

    /** Фабрика для ProfileViewModel (единый контейнер обеспечивает одну БД с LoginViewModel). */
    fun profileViewModelFactory(): ProfileViewModel

    /** Фабрика для FriendsListViewModel */
    fun friendsListViewModelFactory(): FriendsListViewModel

    /** Фабрика для UserFriendsViewModel */
    fun userFriendsViewModelFactory(userId: Long): UserFriendsViewModel

    /** Фабрика для BlacklistViewModel */
    fun blacklistViewModelFactory(): BlacklistViewModel

    /** Фабрика для UserTrainingParksViewModel */
    fun userTrainingParksViewModelFactory(userId: Long): UserTrainingParksViewModel

    /** Фабрика для UserAddedParksViewModel */
    fun userAddedParksViewModelFactory(
        userId: Long,
        seedParks: List<Park>?,
        requiresFetch: Boolean
    ): UserAddedParksViewModel

    /** Фабрика для JournalsViewModel */
    fun journalsViewModelFactory(userId: Long): JournalsViewModel

    /** Фабрика для JournalEntriesViewModel */
    fun journalEntriesViewModelFactory(
        journalOwnerId: Long,
        journalId: Long,
        savedStateHandle: SavedStateHandle
    ): JournalEntriesViewModel

    /** Фабрика для TextEntryViewModel */
    fun textEntryViewModelFactory(mode: TextEntryMode): TextEntryViewModel

    /** Фабрика для DialogsViewModel */
    fun dialogsViewModelFactory(): DialogsViewModel

    /** Фабрика для ChatViewModel */
    fun chatViewModelFactory(): ChatViewModel

    /** Фабрика для SearchUserViewModel */
    fun searchUserViewModelFactory(): SearchUserViewModel

    /** Фабрика для OtherUserProfileViewModel */
    fun otherUserProfileViewModelFactory(userId: Long): OtherUserProfileViewModel

    /** Фабрика для EditProfileViewModel */
    fun editProfileViewModelFactory(): EditProfileViewModel

    /** Фабрика для ChangePasswordViewModel */
    fun changePasswordViewModelFactory(): ChangePasswordViewModel

    /** Фабрика для RegisterViewModel */
    fun registerViewModelFactory(): RegisterViewModel

    /** Фабрика для EventsViewModel */
    fun eventsViewModelFactory(): EventsViewModel

    /** Фабрика для EventDetailViewModel */
    fun eventDetailViewModelFactory(savedStateHandle: SavedStateHandle): EventDetailViewModel

    /** Фабрика для EventFormViewModel */
    fun eventFormViewModelFactory(mode: EventFormMode): EventFormViewModel

    /** Фабрика для ParkFormViewModel */
    fun parkFormViewModelFactory(mode: com.swparks.ui.model.ParkFormMode): ParkFormViewModel

    // API клиенты для разных функциональных областей
    fun provideAuthApi(): SWApi
    fun provideProfileApi(): SWApi
    fun provideFriendsApi(): SWApi
    fun provideParksApi(): SWApi
    fun provideEventsApi(): SWApi
    fun provideMessagesApi(): SWApi
    fun provideJournalsApi(): SWApi
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Suppress("TooManyFunctions")
class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext: Context = context.applicationContext
    private val baseUrl = "https://workout.su/api/v3/"
    private val jsonFactory = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    override val logger: Logger = AndroidLogger()
    override val userNotifier: UserNotifier = UserNotifierImpl(logger)
    override val crashReporter: CrashReporter = com.swparks.util.crash.FirebaseCrashReporter
    override val messageSentNotifier: MessageSentNotifier = MessageSentNotifier()

    // ==================== Location & Geocoding Services ====================

    override val locationService: LocationService by lazy {
        LocationServiceImpl(appContext)
    }

    override val geocodingService: GeocodingService by lazy {
        GeocodingServiceImpl(appContext)
    }

    override val findCityByCoordinatesUseCase: IFindCityByCoordinatesUseCase by lazy {
        FindCityByCoordinatesUseCase(countriesRepository)
    }

    override val createParkLocationHandler: ICreateParkLocationHandler by lazy {
        DefaultCreateParkLocationHandler(locationService, userNotifier)
    }

    // ==================== Parks Filter ====================

    override val parksFilterDataStore: ParksFilterDataStore by lazy {
        ParksFilterDataStore(appContext)
    }

    override val filterParksUseCase: IFilterParksUseCase by lazy {
        FilterParksUseCase()
    }

    // ==================== Resources Provider ====================

    /**
     * Провайдер для доступа к строковым ресурсам
     * Используется в ViewModel для локализации без зависимости от Context
     */
    private val resourcesProvider: ResourcesProvider by lazy {
        ResourcesProviderImpl(appContext)
    }

    // ==================== Avatar Helper ====================

    /**
     * Хелпер для работы с аватарами (изображениями)
     * Используется в ViewModel для работы с Uri без зависимости от Context
     */
    private val avatarHelper: AvatarHelper by lazy {
        AvatarHelperImpl(appContext)
    }

    // ==================== Room Database ====================

    /**
     * Room Database для локального хранения данных
     * Использует миграции для сохранения данных при обновлении схемы
     */
    val database: SWDatabase by lazy {
        Room.databaseBuilder(
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

    // ==================== Криптография и хранение токена ====================

    // Создаем CryptoManager для шифрования токена
    private val cryptoManager: CryptoManager by lazy {
        CryptoManagerImpl(appContext)
    }

    // Создаем EncryptedStringSerializer для шифрования/дешифрования токена
    private val encryptedStringSerializer: EncryptedStringSerializer by lazy {
        EncryptedStringSerializer(cryptoManager)
    }

    // Создаем SecureTokenRepository для безопасного хранения токена
    override val secureTokenRepository: SecureTokenRepository by lazy {
        SecureTokenRepository(appContext.dataStore, encryptedStringSerializer)
    }

    // Создаем UserPreferencesRepository для использования в AuthInterceptor
    override val userPreferencesRepository: UserPreferencesRepository by lazy {
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

    // LoggingInterceptor для отладки HTTP запросов
    private val loggingInterceptor: LoggingInterceptor by lazy {
        LoggingInterceptor()
    }

    // Создаем OkHttpClient с interceptor chain
    // Порядок важен: logging → retry → token → auth
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Таймауты для сетевых операций
            .connectTimeout(NetworkTimeouts.CONNECT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkTimeouts.READ_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkTimeouts.WRITE_SECONDS, TimeUnit.SECONDS)
            .callTimeout(NetworkTimeouts.CALL_SECONDS, TimeUnit.SECONDS)
            // Interceptors
            .addInterceptor(loggingInterceptor) // Логирование запросов/ответов
            .addInterceptor(retryInterceptor)   // ← ОБЯЗАТЕЛЬНО ПЕРВЫМ после logging!
            .addInterceptor(tokenInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(jsonFactory.asConverterFactory("application/json".toMediaType()))
        .build()

    // Единый экземпляр SWApi для всех API запросов
    private val retrofitService: SWApi by lazy {
        retrofit.create(SWApi::class.java)
    }

    override val swRepository: SWRepository by lazy {
        SWRepositoryImp(
            swApi = retrofitService,
            dataStore = appContext.dataStore,
            userDao = userDao,
            journalDao = journalDao,
            journalEntryDao = journalEntryDao,
            dialogDao = dialogDao,
            eventDao = eventDao,
            crashReporter = crashReporter,
            logger = logger
        )
    }

// ==================== Справочник стран и городов ====================

    override val countriesRepository: CountriesRepository by lazy {
        CountriesRepositoryImpl(context = appContext, swApi = retrofitService, logger = logger)
    }

    override val journalsRepository: JournalsRepository by lazy {
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
    override val journalEntriesRepository: JournalEntriesRepository by lazy {
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
    override val messagesRepository: MessagesRepository by lazy {
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

    override val loginUseCase: ILoginUseCase by lazy {
        LoginUseCase(
            tokenEncoder,
            secureTokenRepository,
            swRepository,
            userPreferencesRepository,
            crashReporter
        )
    }

    override val logoutUseCase: ILogoutUseCase by lazy {
        LogoutUseCase(
            secureTokenRepository,
            swRepository,
            crashReporter
        )
    }

    override val resetPasswordUseCase: IResetPasswordUseCase by lazy {
        ResetPasswordUseCase(swRepository)
    }

    override val changePasswordUseCase: IChangePasswordUseCase by lazy {
        ChangePasswordUseCase(swRepository, secureTokenRepository, tokenEncoder)
    }

    override val deleteUserUseCase: IDeleteUserUseCase by lazy {
        DeleteUserUseCase(secureTokenRepository, swRepository)
    }

    // ==================== Use cases для дневников ====================

    override val getJournalsUseCase: IGetJournalsUseCase by lazy {
        GetJournalsUseCase(journalsRepository)
    }

    override val syncJournalsUseCase: ISyncJournalsUseCase by lazy {
        SyncJournalsUseCase(journalsRepository)
    }

    // ==================== Use cases для записей дневника ====================

    // Примечание: Use Case'ы являются stateless-компонентами и не зависят от конкретных
    // userId и journalId при создании. Эти параметры передаются в методах invoke() Use Case'ов.

    override val getJournalEntriesUseCase: IGetJournalEntriesUseCase by lazy {
        GetJournalEntriesUseCase(journalEntriesRepository)
    }

    override val syncJournalEntriesUseCase: ISyncJournalEntriesUseCase by lazy {
        SyncJournalEntriesUseCase(journalEntriesRepository)
    }
    override val deleteJournalEntryUseCase: IDeleteJournalEntryUseCase by lazy {
        DeleteJournalEntryUseCase(journalEntriesRepository)
    }
    override val canDeleteJournalEntryUseCase: ICanDeleteJournalEntryUseCase by lazy {
        CanDeleteJournalEntryUseCase(journalEntriesRepository)
    }
    override val deleteJournalUseCase: IDeleteJournalUseCase by lazy {
        DeleteJournalUseCase(swRepository)
    }
    override val editJournalSettingsUseCase: IEditJournalSettingsUseCase by lazy {
        EditJournalSettingsUseCase(swRepository)
    }
    val createJournalUseCase: ICreateJournalUseCase by lazy {
        CreateJournalUseCase(swRepository)
    }
    override val textEntryUseCase: ITextEntryUseCase by lazy {
        TextEntryUseCase(swRepository, createJournalUseCase, messageSentNotifier)
    }

    // ==================== Use cases для мероприятий ====================

    override val getFutureEventsFlowUseCase: IGetFutureEventsFlowUseCase by lazy {
        GetFutureEventsFlowUseCase(swRepository)
    }
    override val syncFutureEventsUseCase: ISyncFutureEventsUseCase by lazy {
        SyncFutureEventsUseCase(swRepository)
    }
    override val getPastEventsFlowUseCase: IGetPastEventsFlowUseCase by lazy {
        GetPastEventsFlowUseCase(swRepository)
    }
    override val syncPastEventsUseCase: ISyncPastEventsUseCase by lazy {
        SyncPastEventsUseCase(swRepository)
    }
    override val createEventUseCase: ICreateEventUseCase by lazy {
        CreateEventUseCase(swRepository)
    }
    override val editEventUseCase: IEditEventUseCase by lazy {
        EditEventUseCase(swRepository)
    }

    /** Factory метод для создания ProfileViewModel */
    override fun profileViewModelFactory() = ProfileViewModel(
        countriesRepository = countriesRepository,
        swRepository = swRepository,
        logger = logger,
        userNotifier = userNotifier
    )

    /** Factory метод для создания FriendsListViewModel */
    override fun friendsListViewModelFactory() = FriendsListViewModel(
        userDao = userDao,
        swRepository = swRepository,
        logger = logger,
        userNotifier = userNotifier
    )

    /** Factory метод для создания UserFriendsViewModel */
    override fun userFriendsViewModelFactory(userId: Long) = UserFriendsViewModel(
        userId = userId,
        swRepository = swRepository,
        logger = logger,
        userNotifier = userNotifier
    )

    /** Factory метод для создания BlacklistViewModel */
    override fun blacklistViewModelFactory() = BlacklistViewModel(
        swRepository = swRepository,
        logger = logger,
        userNotifier = userNotifier
    )

    /** Factory метод для создания UserTrainingParksViewModel */
    override fun userTrainingParksViewModelFactory(userId: Long) = UserTrainingParksViewModel(
        swRepository = swRepository,
        userId = userId,
        logger = logger,
        userNotifier = userNotifier
    )

    override fun userAddedParksViewModelFactory(
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
    override fun journalsViewModelFactory(userId: Long) = JournalsViewModel(
        userId = userId,
        getJournalsUseCase = getJournalsUseCase,
        syncJournalsUseCase = syncJournalsUseCase,
        deleteJournalUseCase = deleteJournalUseCase,
        editJournalSettingsUseCase = editJournalSettingsUseCase,
        userNotifier = userNotifier,
        resources = resourcesProvider
    )

    /** Factory метод для создания JournalEntriesViewModel */
    override fun journalEntriesViewModelFactory(
        journalOwnerId: Long,
        journalId: Long,
        savedStateHandle: SavedStateHandle
    ) =
        JournalEntriesViewModel(
            journalOwnerId = journalOwnerId,
            journalId = journalId,
            deps = JournalEntriesDeps(
                getJournalEntriesUseCase = getJournalEntriesUseCase,
                syncJournalEntriesUseCase = syncJournalEntriesUseCase,
                deleteJournalEntryUseCase = deleteJournalEntryUseCase,
                canDeleteJournalEntryUseCase = canDeleteJournalEntryUseCase,
                editJournalSettingsUseCase = editJournalSettingsUseCase,
                userPreferencesRepository = userPreferencesRepository,
                swRepository = swRepository,
                savedStateHandle = savedStateHandle,
                userNotifier = userNotifier,
                resources = resourcesProvider
            )
        )

    /** Factory метод для создания TextEntryViewModel */
    override fun textEntryViewModelFactory(mode: TextEntryMode) = TextEntryViewModel(
        textEntryUseCase = textEntryUseCase,
        userNotifier = userNotifier,
        mode = mode,
        context = appContext
    )

    /** Factory метод для создания DialogsViewModel */
    override fun dialogsViewModelFactory() = DialogsViewModel(
        messagesRepository = messagesRepository,
        swRepository = swRepository,
        logger = logger,
        resources = resourcesProvider,
        messageSentNotifier = messageSentNotifier
    )

    /** Factory метод для создания ChatViewModel */
    override fun chatViewModelFactory() = ChatViewModel(
        swApi = provideMessagesApi(),
        swRepository = swRepository,
        userNotifier = userNotifier
    )

    /** Factory метод для создания SearchUserViewModel */
    override fun searchUserViewModelFactory() = SearchUserViewModel(
        swRepository = swRepository,
        logger = logger
    )

    /** Factory метод для создания OtherUserProfileViewModel */
    override fun otherUserProfileViewModelFactory(userId: Long) =
        OtherUserProfileViewModel(
            userId = userId,
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            logger = logger,
            userNotifier = userNotifier,
            resources = resourcesProvider
        )

    /** Factory метод для создания EditProfileViewModel */
    override fun editProfileViewModelFactory() = EditProfileViewModel(
        swRepository = swRepository,
        countriesRepository = countriesRepository,
        deleteUserUseCase = deleteUserUseCase,
        avatarHelper = avatarHelper,
        logger = logger,
        userNotifier = userNotifier,
        resources = resourcesProvider
    )

    /** Factory метод для создания ChangePasswordViewModel */
    override fun changePasswordViewModelFactory() = ChangePasswordViewModel(
        changePasswordUseCase = changePasswordUseCase,
        logger = logger,
        userNotifier = userNotifier,
        resources = resourcesProvider
    )

    /** Factory метод для создания RegisterViewModel */
    override fun registerViewModelFactory() = RegisterViewModel(
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
    override fun eventsViewModelFactory() = EventsViewModel(
        getFutureEventsFlowUseCase = getFutureEventsFlowUseCase,
        syncFutureEventsUseCase = syncFutureEventsUseCase,
        getPastEventsFlowUseCase = getPastEventsFlowUseCase,
        syncPastEventsUseCase = syncPastEventsUseCase,
        userPreferencesRepository = userPreferencesRepository,
        countriesRepository = countriesRepository,
        userNotifier = userNotifier,
        logger = logger
    )

    /** Factory метод для создания EventDetailViewModel */
    override fun eventDetailViewModelFactory(savedStateHandle: SavedStateHandle) =
        EventDetailViewModel(
            swRepository = swRepository,
            countriesRepository = countriesRepository,
            userPreferencesRepository = userPreferencesRepository,
            savedStateHandle = savedStateHandle,
            userNotifier = userNotifier,
            logger = logger
        )

    /** Factory метод для создания EventFormViewModel */
    override fun eventFormViewModelFactory(mode: EventFormMode) =
        EventFormViewModel(
            mode = mode,
            createEventUseCase = createEventUseCase,
            editEventUseCase = editEventUseCase,
            avatarHelper = avatarHelper,
            logger = logger,
            userNotifier = userNotifier
        )

    /** Factory метод для создания ParkFormViewModel */
    override fun parkFormViewModelFactory(mode: com.swparks.ui.model.ParkFormMode) =
        ParkFormViewModel(
            mode = mode,
            swRepository = swRepository,
            avatarHelper = avatarHelper,
            logger = logger,
            userNotifier = userNotifier,
            geocodingService = geocodingService,
            findCityByCoordinatesUseCase = findCityByCoordinatesUseCase,
            userDao = userDao
        )

    // ==================== API клиенты для разных функциональных областей ====================
    // Все фабричные методы возвращают один и тот же экземпляр SWApi для консистентности
    // Разделение по областям обеспечивает лучшую организацию кода и гибкость для будущего рефакторинга

    override fun provideAuthApi(): SWApi = retrofitService

    override fun provideProfileApi(): SWApi = retrofitService

    override fun provideFriendsApi(): SWApi = retrofitService

    override fun provideParksApi(): SWApi = retrofitService

    override fun provideEventsApi(): SWApi = retrofitService

    override fun provideMessagesApi(): SWApi = retrofitService

    override fun provideJournalsApi(): SWApi = retrofitService
}
