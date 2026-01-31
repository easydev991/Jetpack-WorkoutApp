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
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * Репозиторий для безопасного хранения токена авторизации.
 *
 * Токен шифруется через [EncryptedStringSerializer] с использованием CryptoManager (Tink)
 * и сохраняется в Preferences DataStore. UserPreferencesRepository остаётся без изменений
 * и хранит только настройки приложения (is_authorized и т.д.).
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
     * @param token Токен авторизации или null для очистки
     */
    suspend fun saveAuthToken(token: String?) {
        val encryptedToken = serializer.serialize(token)
        dataStore.edit { preferences ->
            if (encryptedToken.isEmpty()) {
                preferences.remove(encrypted_token)
                Log.i(TAG, "Токен авторизации удален")
            } else {
                val encryptedTokenBase64 = Base64.encodeToString(encryptedToken, Base64.NO_WRAP)
                preferences[encrypted_token] = encryptedTokenBase64
                Log.i(TAG, "Токен авторизации сохранен")
            }
        }
    }

    /**
     * Синхронно получает токен авторизации.
     *
     * Используется в Interceptor, который работает в не-suspend контексте.
     * Использует runBlocking для блокирующего чтения из DataStore.
     *
     * @return Токен авторизации или null если не установлен
     */
    fun getAuthTokenSync(): String? = runBlocking {
        try {
            val preferences = dataStore.data.first()
            val encryptedTokenBase64 = preferences[encrypted_token]
            if (encryptedTokenBase64 != null) {
                val encryptedToken = Base64.decode(encryptedTokenBase64, Base64.NO_WRAP)
                serializer.deserialize(encryptedToken)
            } else {
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка при чтении токена синхронно", e)
            null
        }
    }

    /**
     * Синхронно очищает токен авторизации.
     *
     * Используется в Interceptor при ошибке 401 или при выходе из системы.
     */
    fun clearAuthTokenSync() {
        runBlocking {
            try {
                dataStore.edit { preferences ->
                    preferences.remove(encrypted_token)
                }
                Log.i(TAG, "Токен авторизации успешно очищен")
            } catch (e: IOException) {
                Log.e(TAG, "Ошибка при очистке токена", e)
            }
        }
    }
}
