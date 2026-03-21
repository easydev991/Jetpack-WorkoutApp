package com.swparks.navigation

import androidx.navigation.NavController
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
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

internal data class EditParkNavigationData(
    val route: String,
    val parkJson: String
)

internal fun buildEditParkNavigationData(
    parkId: Long,
    source: String,
    park: Park
): EditParkNavigationData {
    val route = Screen.EditPark.createRoute(parkId, source)
    val parkJson = WorkoutAppJson.encodeToString(park)
    return EditParkNavigationData(route, parkJson)
}

fun NavController.navigateToEditPark(
    parkId: Long,
    source: String = "parks",
    park: Park
) {
    val data = buildEditParkNavigationData(parkId, source, park)
    navigate(data.route)
    val backStackEntry = getBackStackEntry(data.route)
    backStackEntry.savedStateHandle[EDIT_PARK_JSON_KEY] = data.parkJson
}

internal data class CreateParkNavigationData(
    val route: String,
    val draftJson: String
)

internal fun buildCreateParkNavigationData(
    source: String,
    draft: NewParkDraft
): CreateParkNavigationData {
    val route = Screen.CreatePark.createRoute(source)
    val draftJson = WorkoutAppJson.encodeToString(draft)
    return CreateParkNavigationData(route, draftJson)
}

fun NavController.navigateToCreatePark(
    source: String = "parks",
    draft: NewParkDraft
) {
    val data = buildCreateParkNavigationData(source, draft)
    navigate(data.route)
    val backStackEntry = getBackStackEntry(data.route)
    backStackEntry.savedStateHandle[CREATE_PARK_DRAFT_JSON_KEY] = data.draftJson
}
