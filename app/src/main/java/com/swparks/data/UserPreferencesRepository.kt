package com.swparks.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
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
        val current_user_id = longPreferencesKey("currentUserId")
        const val tag = "UserPreferencesRepository"
    }

    /**
     * ID текущего авторизованного пользователя.
     * Эмитит изменения при сохранении/очистке через saveCurrentUserId/clearCurrentUserId.
     */
    val currentUserId: Flow<Long?> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(
                    tag,
                    "Ошибка при загрузке userId",
                    it
                )
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { it[current_user_id] }
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
            it[current_user_id] = userId
        }
        Log.i(tag, "Сохранен текущий userId: $userId")
    }

    /**
     * Синхронно получает ID текущего пользователя
     */
    fun getCurrentUserIdSync(): Long? = runBlocking {
        try {
            dataStore.data.first()[current_user_id]
        } catch (e: IOException) {
            Log.e(tag, "Ошибка при чтении userId синхронно", e)
            null
        }
    }

    /**
     * Очищает ID текущего пользователя (логаут)
     */
    suspend fun clearCurrentUserId() {
        dataStore.edit {
            it.remove(current_user_id)
        }
        Log.i(tag, "Очищен текущий userId (логаут)")
    }

    /**
     * Синхронно очищает все данные пользователя
     * Используется в AuthInterceptor при 401
     */
    fun clearAllUserData() {
        runBlocking {
            dataStore.edit {
                it.remove(current_user_id)
            }
        }
        Log.i(tag, "Все данные пользователя очищены")
    }
}
