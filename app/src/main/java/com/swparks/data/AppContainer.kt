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
import com.swparks.data.database.MIGRATION_1_2
import com.swparks.data.database.SWDatabase
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.interceptor.AuthInterceptor
import com.swparks.data.interceptor.RetryInterceptor
import com.swparks.data.interceptor.TokenInterceptor
import com.swparks.data.repository.CountriesRepositoryImpl
import com.swparks.data.repository.JournalEntriesRepositoryImpl
import com.swparks.data.repository.JournalsRepositoryImpl
import com.swparks.data.repository.MessagesRepositoryImpl
import com.swparks.data.repository.SWRepository
import com.swparks.data.repository.SWRepositoryImp
import com.swparks.data.serializer.EncryptedStringSerializer
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.repository.JournalEntriesRepository
import com.swparks.domain.repository.MessagesRepository
import com.swparks.domain.usecase.CanDeleteJournalEntryUseCase
import com.swparks.domain.usecase.CreateJournalUseCase
import com.swparks.domain.usecase.DeleteJournalEntryUseCase
import com.swparks.domain.usecase.DeleteJournalUseCase
import com.swparks.domain.usecase.EditJournalSettingsUseCase
import com.swparks.domain.usecase.GetJournalEntriesUseCase
import com.swparks.domain.usecase.GetJournalsUseCase
import com.swparks.domain.usecase.ICanDeleteJournalEntryUseCase
import com.swparks.domain.usecase.ICreateJournalUseCase
import com.swparks.domain.usecase.IDeleteJournalEntryUseCase
import com.swparks.domain.usecase.IDeleteJournalUseCase
import com.swparks.domain.usecase.IEditJournalSettingsUseCase
import com.swparks.domain.usecase.IGetJournalEntriesUseCase
import com.swparks.domain.usecase.IGetJournalsUseCase
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.ILogoutUseCase
import com.swparks.domain.usecase.IResetPasswordUseCase
import com.swparks.domain.usecase.ISyncJournalEntriesUseCase
import com.swparks.domain.usecase.ISyncJournalsUseCase
import com.swparks.domain.usecase.ITextEntryUseCase
import com.swparks.domain.usecase.LoginUseCase
import com.swparks.domain.usecase.LogoutUseCase
import com.swparks.domain.usecase.ResetPasswordUseCase
import com.swparks.domain.usecase.SyncJournalEntriesUseCase
import com.swparks.domain.usecase.SyncJournalsUseCase
import com.swparks.domain.usecase.TextEntryUseCase
import com.swparks.network.SWApi
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.viewmodel.BlacklistViewModel
import com.swparks.ui.viewmodel.FriendsListViewModel
import com.swparks.ui.viewmodel.JournalEntriesDeps
import com.swparks.ui.viewmodel.JournalEntriesViewModel
import com.swparks.ui.viewmodel.JournalsViewModel
import com.swparks.ui.viewmodel.ProfileViewModel
import com.swparks.ui.viewmodel.TextEntryViewModel
import com.swparks.ui.viewmodel.UserTrainingParksViewModel
import com.swparks.util.AndroidLogger
import com.swparks.util.ErrorHandler
import com.swparks.util.ErrorReporter
import com.swparks.util.Logger
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

interface AppContainer {
    val swRepository: SWRepository
    val secureTokenRepository: SecureTokenRepository
    val countriesRepository: CountriesRepository
    val journalsRepository: com.swparks.domain.repository.JournalsRepository
    val journalEntriesRepository: JournalEntriesRepository
    val messagesRepository: MessagesRepository

    // Сервисы для обработки ошибок
    val logger: Logger
    val errorReporter: ErrorReporter

    // Use cases для авторизации
    val loginUseCase: ILoginUseCase
    val logoutUseCase: ILogoutUseCase
    val resetPasswordUseCase: IResetPasswordUseCase

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

    /** Фабрика для ProfileViewModel (единый контейнер обеспечивает одну БД с LoginViewModel). */
    fun profileViewModelFactory(): ProfileViewModel

    /** Фабрика для FriendsListViewModel */
    fun friendsListViewModelFactory(): FriendsListViewModel

    /** Фабрика для BlacklistViewModel */
    fun blacklistViewModelFactory(): BlacklistViewModel

    /** Фабрика для UserTrainingParksViewModel */
    fun userTrainingParksViewModelFactory(userId: Long): UserTrainingParksViewModel

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
    fun dialogsViewModelFactory(): com.swparks.ui.viewmodel.DialogsViewModel

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

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext: Context = context.applicationContext
    private val baseUrl = "https://workout.su/api/v3/"
    private val jsonFactory = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    override val logger: Logger = AndroidLogger()
    override val errorReporter: ErrorReporter = ErrorHandler(logger)

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
        ).addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /**
     * DAO для работы с пользователями
     */
    val userDao: UserDao by lazy { database.userDao() }

    /**
     * DAO для работы с дневниками
     */
    private val journalDao: com.swparks.data.database.dao.JournalDao by lazy { database.journalDao() }

    /**
     * DAO для работы с диалогами
     */
    private val dialogDao: com.swparks.data.database.dao.DialogDao by lazy { database.dialogDao() }

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
    private val preferencesRepository: UserPreferencesRepository by lazy {
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
        AuthInterceptor(preferencesRepository)
    }

    // Создаем OkHttpClient с interceptor chain
    // Порядок важен: retry → token → auth
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(retryInterceptor)  // ← ОБЯЗАТЕЛЬНО ПЕРВЫМ!
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
            dialogDao = dialogDao
        )
    }

// ==================== Справочник стран и городов ====================

    override val countriesRepository: CountriesRepository by lazy {
        CountriesRepositoryImpl(context = appContext, swApi = retrofitService, logger = logger)
    }

    override val journalsRepository: com.swparks.domain.repository.JournalsRepository by lazy {
        JournalsRepositoryImpl(swApi = retrofitService, journalDao = journalDao)
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
        JournalEntriesRepositoryImpl(swApi = retrofitService, journalEntryDao = journalEntryDao)
    }

    /**
     * Репозиторий для работы с диалогами
     */
    override val messagesRepository: MessagesRepository by lazy {
        MessagesRepositoryImpl(
            dialogsDao = dialogDao,
            swApi = retrofitService,
            logger = logger
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
            preferencesRepository
        )
    }

    override val logoutUseCase: ILogoutUseCase by lazy {
        LogoutUseCase(
            secureTokenRepository,
            swRepository
        )
    }

    override val resetPasswordUseCase: IResetPasswordUseCase by lazy {
        ResetPasswordUseCase(swRepository)
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
        TextEntryUseCase(swRepository, createJournalUseCase)
    }

    /** Factory метод для создания ProfileViewModel */
    override fun profileViewModelFactory() = ProfileViewModel(
        countriesRepository = countriesRepository,
        swRepository = swRepository,
        logger = logger,
        errorReporter = errorReporter
    )

    /** Factory метод для создания FriendsListViewModel */
    override fun friendsListViewModelFactory() = FriendsListViewModel(
        userDao = userDao,
        swRepository = swRepository,
        logger = logger,
        errorReporter = errorReporter
    )

    /** Factory метод для создания BlacklistViewModel */
    override fun blacklistViewModelFactory() = BlacklistViewModel(
        swRepository = swRepository,
        logger = logger,
        errorReporter = errorReporter
    )

    /** Factory метод для создания UserTrainingParksViewModel */
    override fun userTrainingParksViewModelFactory(userId: Long) = UserTrainingParksViewModel(
        swRepository = swRepository,
        userId = userId,
        logger = logger,
        errorReporter = errorReporter
    )

    /** Factory метод для создания JournalsViewModel */
    override fun journalsViewModelFactory(userId: Long) = JournalsViewModel(
        userId = userId,
        getJournalsUseCase = getJournalsUseCase,
        syncJournalsUseCase = syncJournalsUseCase,
        deleteJournalUseCase = deleteJournalUseCase,
        editJournalSettingsUseCase = editJournalSettingsUseCase,
        errorReporter = errorReporter
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
                preferencesRepository = preferencesRepository,
                swRepository = swRepository,
                savedStateHandle = savedStateHandle,
                errorReporter = errorReporter
            )
        )

    /** Factory метод для создания TextEntryViewModel */
    override fun textEntryViewModelFactory(mode: TextEntryMode) = TextEntryViewModel(
        textEntryUseCase = textEntryUseCase,
        errorReporter = errorReporter,
        mode = mode,
        context = appContext
    )

    /** Factory метод для создания DialogsViewModel */
    override fun dialogsViewModelFactory() = com.swparks.ui.viewmodel.DialogsViewModel(
        messagesRepository = messagesRepository,
        logger = logger
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
