package com.swparks.data

import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.swparks.data.serializer.EncryptedStringSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

/**
 * Репозиторий для безопасного хранения токена авторизации.
 *
 * Токен шифруется через [EncryptedStringSerializer] с использованием CryptoManager (Tink)
 * и сохраняется в Preferences DataStore. UserPreferencesRepository остаётся без изменений
 * и хранит только настройки приложения (is_authorized и т.д.).
 *
 * **Важно:** Токен кэшируется в памяти для синхронного доступа из Interceptor.
 * Это позволяет избежать runBlocking, который может вызывать deadlock в OkHttp.
 *
 * @param dataStore DataStore для хранения Preferences
 * @param serializer Serializer для шифрования/дешифрования токена
 */
class SecureTokenRepository(
    private val dataStore: DataStore<Preferences>,
    private val serializer: EncryptedStringSerializer
) {
    private companion object {
        val encrypted_token = stringPreferencesKey("encrypted_token")
        const val TAG = "SecureTokenRepository"
    }

    // In-memory кэш токена для синхронного доступа из Interceptor
    // Используем AtomicReference для thread-safety
    private val tokenCache = AtomicReference<String?>(null)

    /**
     * Flow токена авторизации.
     *
     * @return Flow<String?> Токен авторизации или null если не установлен
     */
    val authToken: Flow<String?> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(
                    TAG,
                    "Ошибка при загрузке токена",
                    it
                )
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            val encryptedTokenBase64 = preferences[encrypted_token]
            if (encryptedTokenBase64 != null) {
                val encryptedToken = Base64.decode(encryptedTokenBase64, Base64.NO_WRAP)
                serializer.deserialize(encryptedToken)
            } else {
                null
            }
        }

    /**
     * Сохраняет токен авторизации с шифрованием.
     *
     * Также обновляет in-memory кэш для синхронного доступа.
     *
     * @param token Токен авторизации или null для очистки
     */
    suspend fun saveAuthToken(token: String?) {
        // Обновляем кэш synchronously (до сохранения на диск)
        tokenCache.set(token)

        val encryptedToken = serializer.serialize(token)
        dataStore.edit { preferences ->
            if (encryptedToken.isEmpty()) {
                preferences.remove(encrypted_token)
                Log.i(TAG, "Токен авторизации удален из DataStore")
            } else {
                val encryptedTokenBase64 = Base64.encodeToString(encryptedToken, Base64.NO_WRAP)
                preferences[encrypted_token] = encryptedTokenBase64
            }
        }
    }

    /**
     * Синхронно получает токен авторизации из in-memory кэша.
     *
     * Используется в Interceptor, который работает в не-suspend контексте.
     * Читает токен из кэша, что позволяет избежать runBlocking и потенциального deadlock.
     *
     * Если кэш пуст, возвращает null (токен не установлен или приложение только запущено).
     *
     * @return Токен авторизации или null если не установлен
     */
    fun getAuthTokenSync(): String? = tokenCache.get()

    /**
     * Загружает токен из DataStore в кэш при запуске приложения.
     *
     * Должен вызываться один раз при инициализации приложения.
     */
    suspend fun loadTokenToCache() {
        try {
            val preferences = dataStore.data.first()
            val encryptedTokenBase64 = preferences[encrypted_token]
            if (encryptedTokenBase64 != null) {
                val encryptedToken = Base64.decode(encryptedTokenBase64, Base64.NO_WRAP)
                val token = serializer.deserialize(encryptedToken)
                tokenCache.set(token)
                Log.i(TAG, "Токен загружен в кэш при старте")
            } else {
                Log.i(TAG, "Токен отсутствует в DataStore")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка при загрузке токена в кэш", e)
        }
    }

    /**
     * Синхронно очищает токен авторизации.
     *
     * Используется в Interceptor при ошибке 401 или при выходе из системы.
     * Очищает и кэш, и DataStore.
     */
    fun clearAuthTokenSync() {
        // Очищаем кэш
        tokenCache.set(null)
        Log.i(TAG, "Токен авторизации очищен из кэша")
    }
}
