package com.swparks.navigation

import androidx.navigation.NavController
import com.swparks.data.model.User
import com.swparks.util.WorkoutAppJson

internal const val PARK_TRAINEES_USERS_JSON_KEY = "park_trainees_users_json"

internal data class ParkTraineesNavigationData(
    val route: String,
    val usersJson: String
)

internal fun buildParkTraineesNavigationData(
    parkId: Long,
    source: String,
    users: List<User>
): ParkTraineesNavigationData {
    val route = Screen.ParkTrainees.createRoute(parkId, source)
    val usersJson = WorkoutAppJson.encodeToString(users)
    return ParkTraineesNavigationData(route, usersJson)
}

fun NavController.navigateToParkTrainees(
    parkId: Long,
    source: String = "parks",
    users: List<User>
) {
    val data = buildParkTraineesNavigationData(parkId, source, users)
    navigate(data.route)
    val backStackEntry = getBackStackEntry(data.route)
    backStackEntry.savedStateHandle[PARK_TRAINEES_USERS_JSON_KEY] = data.usersJson
}
