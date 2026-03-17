package com.swparks.navigation

import androidx.navigation.NavController
import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.util.WorkoutAppJson

internal const val EVENT_PARTICIPANTS_USERS_JSON_KEY = "event_participants_users_json"
internal const val EDIT_EVENT_JSON_KEY = "edit_event_json"

internal data class EventParticipantsNavigationData(
    val route: String,
    val usersJson: String
)

internal fun buildEventParticipantsNavigationData(
    eventId: Long,
    source: String,
    users: List<User>
): EventParticipantsNavigationData {
    val route = Screen.EventParticipants.createRoute(eventId, source)
    val usersJson = WorkoutAppJson.encodeToString(users)
    return EventParticipantsNavigationData(route, usersJson)
}

fun NavController.navigateToEventParticipants(
    eventId: Long,
    source: String = "events",
    users: List<User>
) {
    val data = buildEventParticipantsNavigationData(eventId, source, users)
    navigate(data.route)
    val backStackEntry = getBackStackEntry(data.route)
    backStackEntry.savedStateHandle[EVENT_PARTICIPANTS_USERS_JSON_KEY] = data.usersJson
}

internal data class EditEventNavigationData(
    val route: String,
    val eventJson: String
)

internal fun buildEditEventNavigationData(
    eventId: Long,
    source: String,
    event: Event
): EditEventNavigationData {
    val route = Screen.EditEvent.createRoute(eventId, source)
    val eventJson = WorkoutAppJson.encodeToString(event)
    return EditEventNavigationData(route, eventJson)
}

fun NavController.navigateToEditEvent(
    eventId: Long,
    source: String = "events",
    event: Event
) {
    val data = buildEditEventNavigationData(eventId, source, event)
    navigate(data.route)
    val backStackEntry = getBackStackEntry(data.route)
    backStackEntry.savedStateHandle[EDIT_EVENT_JSON_KEY] = data.eventJson
}
