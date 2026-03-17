package com.swparks.navigation

import androidx.lifecycle.SavedStateHandle
import com.swparks.data.model.Park
import com.swparks.util.WorkoutAppJson

internal const val USER_PARKS_SEED_JSON_KEY = "user_parks_seed_json"
internal const val USER_PARKS_REQUIRES_FETCH_KEY = "user_parks_requires_fetch"
private const val USER_PARKS_MAX_SEED_BYTES = 200_000

internal data class UserParksSeedPayload(
    val parksJson: String?,
    val requiresFetch: Boolean
) {
    fun applyTo(savedStateHandle: SavedStateHandle) {
        savedStateHandle[USER_PARKS_REQUIRES_FETCH_KEY] = requiresFetch
        if (parksJson != null) {
            savedStateHandle[USER_PARKS_SEED_JSON_KEY] = parksJson
        } else {
            savedStateHandle.remove<String>(USER_PARKS_SEED_JSON_KEY)
        }
    }

    companion object {
        fun fromParks(parks: List<Park>): UserParksSeedPayload {
            val parksJson = WorkoutAppJson.encodeToString(parks)
            val jsonBytes = parksJson.toByteArray(Charsets.UTF_8).size

            return if (jsonBytes <= USER_PARKS_MAX_SEED_BYTES) {
                UserParksSeedPayload(parksJson = parksJson, requiresFetch = false)
            } else {
                UserParksSeedPayload(parksJson = null, requiresFetch = true)
            }
        }
    }
}
