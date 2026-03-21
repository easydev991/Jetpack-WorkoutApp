package com.swparks.navigation

import androidx.navigation.NavBackStackEntry
import com.swparks.data.model.User
import com.swparks.util.WorkoutAppJson

internal data class ParkTraineesNavArgs(
    val parkId: Long,
    val source: String,
    val users: List<User>
)

internal fun NavBackStackEntry.consumeParkTraineesArgs(): ParkTraineesNavArgs? {
    val parkId = arguments?.getLong("parkId") ?: return null
    val source = arguments?.getString("source") ?: "parks"
    val usersJson = savedStateHandle.get<String>(PARK_TRAINEES_USERS_JSON_KEY)
    val users = usersJson?.let { json ->
        runCatching { WorkoutAppJson.decodeFromString<List<User>>(json) }.getOrNull()
    }.orEmpty()

    savedStateHandle.remove<String>(PARK_TRAINEES_USERS_JSON_KEY)

    return ParkTraineesNavArgs(
        parkId = parkId,
        source = source,
        users = users
    )
}
