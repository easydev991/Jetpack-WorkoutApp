package com.workout.jetpack_workout.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.workout.jetpack_workout.model.Event
import com.workout.jetpack_workout.network.SWApi
import kotlinx.coroutines.flow.Flow

interface SWRepository {
    suspend fun getPastEvents(): List<Event>
    val isAuthorized: Flow<Boolean>
    suspend fun savePreference(isAuthorized: Boolean)
}

class SWRepositoryImp(
    private val swApi: SWApi,
    private val dataStore: DataStore<Preferences>
): SWRepository {
    private val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(dataStore)
    }

    override suspend fun getPastEvents(): List<Event> = swApi.getPastEvents()

    override val isAuthorized: Flow<Boolean>
        get() = preferencesRepository.isAuthorized

    override suspend fun savePreference(isAuthorized: Boolean) {
        preferencesRepository.savePreference(isAuthorized)
    }
}