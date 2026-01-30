package com.swparks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.swparks.data.crypto.CryptoManager
import com.swparks.data.crypto.CryptoManagerImpl
import com.swparks.data.database.SWDatabase
import com.swparks.data.database.UserDao
import com.swparks.data.interceptor.AuthInterceptor
import com.swparks.data.interceptor.RetryInterceptor
import com.swparks.data.interceptor.TokenInterceptor
import com.swparks.data.repository.CountriesRepositoryImpl
import com.swparks.data.repository.SWRepository
import com.swparks.data.repository.SWRepositoryImp
import com.swparks.data.serializer.EncryptedStringSerializer
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.ILogoutUseCase
import com.swparks.domain.usecase.IResetPasswordUseCase
import com.swparks.domain.usecase.LoginUseCase
import com.swparks.domain.usecase.LogoutUseCase
import com.swparks.domain.usecase.ResetPasswordUseCase
import com.swparks.network.SWApi
import com.swparks.util.AndroidLogger
import com.swparks.util.Logger
import com.swparks.viewmodel.ProfileViewModel
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

interface AppContainer {
    val swRepository: SWRepository
    val secureTokenRepository: SecureTokenRepository
    val countriesRepository: CountriesRepository


    // Use cases для авторизации
    val loginUseCase: ILoginUseCase
    val logoutUseCase: ILogoutUseCase
    val resetPasswordUseCase: IResetPasswordUseCase

    /** Фабрика для ProfileViewModel (единый контейнер обеспечивает одну БД с LoginViewModel). */
    fun profileViewModelFactory(): ProfileViewModel

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
    private val baseUrl = "https://workout.su/api/v3/"
    private val jsonFactory = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private val logger: Logger = AndroidLogger()

    // ==================== Room Database ====================

    /**
     * Room Database для локального хранения данных
     * Использует fallbackToDestructiveMigration для разработки - пересоздает БД при изменении схемы
     */
    val database: SWDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            SWDatabase::class.java,
            "sw_database"
        ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /**
     * DAO для работы с пользователями
     */
    val userDao: UserDao by lazy { database.userDao() }

    // ==================== Криптография и хранение токена ====================

    // Создаем CryptoManager для шифрования токена
    private val cryptoManager: CryptoManager by lazy {
        CryptoManagerImpl(context)
    }

    // Создаем EncryptedStringSerializer для шифрования/дешифрования токена
    private val encryptedStringSerializer: EncryptedStringSerializer by lazy {
        EncryptedStringSerializer(cryptoManager)
    }

    // Создаем SecureTokenRepository для безопасного хранения токена
    override val secureTokenRepository: SecureTokenRepository by lazy {
        SecureTokenRepository(context.dataStore, encryptedStringSerializer)
    }

    // Создаем UserPreferencesRepository для использования в AuthInterceptor
    private val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.dataStore)
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
            dataStore = context.dataStore,
            userDao = userDao
        )
    }

// ==================== Справочник стран и городов ====================

    override val countriesRepository: CountriesRepository by lazy {
        CountriesRepositoryImpl(context = context, swApi = retrofitService, logger = logger)
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
            swRepository,
            preferencesRepository
        )
    }

    override val resetPasswordUseCase: IResetPasswordUseCase by lazy {
        ResetPasswordUseCase(swRepository)
    }

    /** Factory метод для создания ProfileViewModel */
    override fun profileViewModelFactory() = ProfileViewModel(
        countriesRepository = countriesRepository,
        swRepository = swRepository
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
