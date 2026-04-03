package com.swparks.navigation

import androidx.navigation.NavBackStackEntry
import com.swparks.data.model.Park
import com.swparks.util.WorkoutAppJson

internal data class UserAddedParksNavArgs(
    val userId: Long,
    val seedParks: List<Park>?,
    val requiresFetch: Boolean
)

internal fun NavBackStackEntry.consumeUserAddedParksArgs(): UserAddedParksNavArgs? {
    val userId = arguments?.getString("userId")?.toLongOrNull() ?: return null
    val seedJson = savedStateHandle.get<String>(USER_PARKS_SEED_JSON_KEY)
    val seedParks =
        seedJson?.let { json ->
            runCatching { WorkoutAppJson.decodeFromString<List<Park>>(json) }.getOrNull()
        }
    val requiresFetch =
        (savedStateHandle.get<Boolean>(USER_PARKS_REQUIRES_FETCH_KEY) ?: false) ||
            seedParks == null

    savedStateHandle.remove<String>(USER_PARKS_SEED_JSON_KEY)
    savedStateHandle.remove<Boolean>(USER_PARKS_REQUIRES_FETCH_KEY)

    return UserAddedParksNavArgs(
        userId = userId,
        seedParks = seedParks,
        requiresFetch = requiresFetch
    )
}
