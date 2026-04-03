package com.swparks.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.swparks.data.model.ParkFilter
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.parksFilterDataStore: DataStore<Preferences> by preferencesDataStore(name = "parks_filter")

internal object ParksFilterPreferencesKeys {
    val SIZES_KEY = stringPreferencesKey("sizes")
    val TYPES_KEY = stringPreferencesKey("types")
    val SELECTED_CITY_ID_KEY = stringPreferencesKey("selected_city_id")
}

class ParksFilterDataStore(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.parksFilterDataStore)

    suspend fun saveFilter(filter: ParkFilter) {
        dataStore.edit { preferences ->
            preferences[ParksFilterPreferencesKeys.SIZES_KEY] =
                filter.sizes.joinToString(",") { it.rawValue.toString() }
            preferences[ParksFilterPreferencesKeys.TYPES_KEY] =
                filter.types.joinToString(",") { it.rawValue.toString() }
            preferences[ParksFilterPreferencesKeys.SELECTED_CITY_ID_KEY] =
                filter.selectedCityId?.toString() ?: ""
        }
    }

    val filter: Flow<ParkFilter> =
        dataStore.data
            .map { preferences ->
                val sizesStr = preferences[ParksFilterPreferencesKeys.SIZES_KEY]
                val typesStr = preferences[ParksFilterPreferencesKeys.TYPES_KEY]
                val cityIdStr = preferences[ParksFilterPreferencesKeys.SELECTED_CITY_ID_KEY]

                val sizes = sizesStr?.parseSizeSet() ?: ParkSize.entries.toSet()
                val types = typesStr?.parseTypeSet() ?: ParkType.entries.toSet()
                val cityId = cityIdStr?.parseCityId()

                ParkFilter(sizes = sizes, types = types, selectedCityId = cityId)
            }

    private fun String.parseSizeSet(): Set<ParkSize> {
        if (isEmpty()) return ParkSize.entries.toSet()
        return split(",")
            .mapNotNull { rawValue ->
                rawValue.toIntOrNull()?.let { value ->
                    ParkSize.entries.firstOrNull { it.rawValue == value }
                }
            }.toSet()
            .takeIf { it.isNotEmpty() }
            ?: ParkSize.entries.toSet()
    }

    private fun String.parseTypeSet(): Set<ParkType> {
        if (isEmpty()) return ParkType.entries.toSet()
        return split(",")
            .mapNotNull { rawValue ->
                rawValue.toIntOrNull()?.let { value ->
                    ParkType.entries.firstOrNull { it.rawValue == value }
                }
            }.toSet()
            .takeIf { it.isNotEmpty() }
            ?: ParkType.entries.toSet()
    }

    private fun String.parseCityId(): Int? {
        if (isEmpty()) return null
        return toIntOrNull()
    }
}

fun createParksFilterDataStore(context: Context): ParksFilterDataStore = ParksFilterDataStore(context.applicationContext)
