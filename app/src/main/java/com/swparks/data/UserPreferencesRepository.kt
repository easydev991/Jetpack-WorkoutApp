package com.swparks.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val CURRENT_USER_ID = longPreferencesKey("currentUserId")
        val LAST_PARKS_UPDATE_DATE = stringPreferencesKey("lastParksUpdateDate")
        val LAST_COUNTRIES_UPDATE_DATE = stringPreferencesKey("lastCountriesUpdateDate")
        const val DEFAULT_PARKS_DATE = "2025-10-25T00:00:00Z"
        const val DEFAULT_COUNTRIES_DATE = "2025-10-25T00:00:00Z"
        const val TAG = "UserPreferencesRepository"
    }

    /**
     * ID текущего авторизованного пользователя.
     * Эмитит изменения при сохранении/очистке через saveCurrentUserId/clearCurrentUserId.
     */
    val currentUserId: Flow<Long?> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    Log.e(
                        TAG,
                        "Ошибка при загрузке userId",
                        it
                    )
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }.map { it[CURRENT_USER_ID] }
            .distinctUntilChanged()

    /**
     * Состояние авторизации вычисляется на основе наличия currentUserId.
     * Если userId есть - пользователь авторизован, иначе - нет.
     */
    val isAuthorized: Flow<Boolean> = currentUserId.map { it != null }

    /**
     * Сохраняет ID текущего авторизованного пользователя
     */
    suspend fun saveCurrentUserId(userId: Long) {
        dataStore.edit {
            it[CURRENT_USER_ID] = userId
        }
        Log.i(TAG, "Сохранен текущий userId: $userId")
    }

    /**
     * Синхронно получает ID текущего пользователя
     */
    fun getCurrentUserIdSync(): Long? =
        runBlocking {
            try {
                dataStore.data.first()[CURRENT_USER_ID]
            } catch (e: IOException) {
                Log.e(TAG, "Ошибка при чтении userId синхронно", e)
                null
            }
        }

    /**
     * Очищает ID текущего пользователя (логаут)
     */
    suspend fun clearCurrentUserId() {
        dataStore.edit {
            it.remove(CURRENT_USER_ID)
        }
        Log.i(TAG, "Очищен текущий userId (логаут)")
    }

    /**
     * Синхронно очищает все данные пользователя
     * Используется в AuthInterceptor при 401
     */
    fun clearAllUserData() {
        runBlocking {
            dataStore.edit {
                it.remove(CURRENT_USER_ID)
            }
        }
        Log.i(TAG, "Все данные пользователя очищены")
    }

    val lastParksUpdateDate: Flow<String> =
        dataStore.data
            .catch { e ->
                if (e is IOException) {
                    Log.e(TAG, "Ошибка при загрузке lastParksUpdateDate", e)
                    emit(emptyPreferences())
                } else {
                    throw e
                }
            }.map { it[LAST_PARKS_UPDATE_DATE] ?: DEFAULT_PARKS_DATE }
            .distinctUntilChanged()

    val lastCountriesUpdateDate: Flow<String> =
        dataStore.data
            .catch { e ->
                if (e is IOException) {
                    Log.e(TAG, "Ошибка при загрузке lastCountriesUpdateDate", e)
                    emit(emptyPreferences())
                } else {
                    throw e
                }
            }.map { it[LAST_COUNTRIES_UPDATE_DATE] ?: DEFAULT_COUNTRIES_DATE }
            .distinctUntilChanged()

    suspend fun setLastParksUpdateDate(date: String) {
        dataStore.edit {
            it[LAST_PARKS_UPDATE_DATE] = date
        }
        Log.i(TAG, "Сохранена дата последнего обновления площадок: $date")
    }

    suspend fun setLastCountriesUpdateDate(date: String) {
        dataStore.edit {
            it[LAST_COUNTRIES_UPDATE_DATE] = date
        }
        Log.i(TAG, "Сохранена дата последнего обновления стран: $date")
    }
}
