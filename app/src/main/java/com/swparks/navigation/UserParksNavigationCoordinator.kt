package com.swparks.navigation

import androidx.navigation.NavController
import com.swparks.data.model.Park

internal data class UserAddedParksNavigationData(
    val route: String,
    val seedPayload: UserParksSeedPayload
)

internal fun buildUserAddedParksNavigationData(
    userId: Long,
    source: String,
    addedParks: List<Park>
): UserAddedParksNavigationData {
    val route = Screen.UserParks.createRoute(userId, source)
    val seedPayload = UserParksSeedPayload.fromParks(addedParks)
    return UserAddedParksNavigationData(route, seedPayload)
}

fun NavController.navigateToUserAddedParks(
    userId: Long,
    source: String = "profile",
    addedParks: List<Park>
) {
    val data = buildUserAddedParksNavigationData(userId, source, addedParks)
    navigate(data.route)
    val backStackEntry = getBackStackEntry(data.route)
    data.seedPayload.applyTo(backStackEntry.savedStateHandle)
}
