package com.swparks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.swparks.data.crypto.CryptoManager
import com.swparks.data.crypto.CryptoManagerImpl
import com.swparks.data.interceptor.AuthInterceptor
import com.swparks.data.interceptor.TokenInterceptor
import com.swparks.data.repository.SWRepository
import com.swparks.data.repository.SWRepositoryImp
import com.swparks.data.serializer.EncryptedStringSerializer
import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.ILogoutUseCase
import com.swparks.domain.usecase.IResetPasswordUseCase
import com.swparks.domain.usecase.LoginUseCase
import com.swparks.domain.usecase.LogoutUseCase
import com.swparks.domain.usecase.ResetPasswordUseCase
import com.swparks.network.SWApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

interface AppContainer {
    val swRepository: SWRepository
    val secureTokenRepository: SecureTokenRepository

    // Use cases для авторизации
    val loginUseCase: ILoginUseCase
    val logoutUseCase: ILogoutUseCase
    val resetPasswordUseCase: IResetPasswordUseCase


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

    // Создаем TokenInterceptor для добавления токена в заголовки
    private val tokenInterceptor: TokenInterceptor by lazy {
        TokenInterceptor(secureTokenRepository)
    }

    // Создаем AuthInterceptor для обработки ошибок 401
    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(preferencesRepository)
    }

    // Создаем OkHttpClient с обоими interceptors
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
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
            dataStore = context.dataStore
        )
    }

    // ==================== Use cases для авторизации ====================

    // Создаем TokenEncoder для генерации токена
    private val tokenEncoder: TokenEncoder by lazy {
        TokenEncoder()
    }

    override val loginUseCase: ILoginUseCase by lazy {
        LoginUseCase(tokenEncoder, secureTokenRepository, swRepository)
    }

    override val logoutUseCase: ILogoutUseCase by lazy {
        LogoutUseCase(secureTokenRepository, swRepository)
    }

    override val resetPasswordUseCase: IResetPasswordUseCase by lazy {
        ResetPasswordUseCase(swRepository)
    }

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
