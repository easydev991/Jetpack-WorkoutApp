package com.swparks.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val is_authorized = booleanPreferencesKey("isAuthorized")
        const val tag = "UserPreferencesRepository"
    }

    val isAuthorized: Flow<Boolean> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(
                    tag,
                    "Ошибка при загрузке preferences",
                    it
                )
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { it[is_authorized] ?: false }

    suspend fun savePreference(isAuthorized: Boolean) {
        dataStore.edit {
            it[is_authorized] = isAuthorized
        }
    }
}