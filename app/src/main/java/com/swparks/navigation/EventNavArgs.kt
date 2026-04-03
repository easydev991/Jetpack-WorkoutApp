package com.swparks.navigation

import androidx.navigation.NavBackStackEntry
import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.util.WorkoutAppJson

internal data class EventParticipantsNavArgs(
    val eventId: Long,
    val source: String,
    val users: List<User>
)

internal fun NavBackStackEntry.consumeEventParticipantsArgs(): EventParticipantsNavArgs? {
    val eventId = arguments?.getLong("eventId") ?: return null
    val source = arguments?.getString("source") ?: "events"
    val usersJson = savedStateHandle.get<String>(EVENT_PARTICIPANTS_USERS_JSON_KEY)
    val users =
        usersJson
            ?.let { json ->
                runCatching { WorkoutAppJson.decodeFromString<List<User>>(json) }.getOrNull()
            }.orEmpty()

    savedStateHandle.remove<String>(EVENT_PARTICIPANTS_USERS_JSON_KEY)

    return EventParticipantsNavArgs(
        eventId = eventId,
        source = source,
        users = users
    )
}

internal data class EditEventNavArgs(
    val eventId: Long,
    val source: String,
    val event: Event?
)

internal fun NavBackStackEntry.consumeEditEventArgs(): EditEventNavArgs? {
    val eventId = arguments?.getLong("eventId") ?: return null
    val source = arguments?.getString("source") ?: "events"
    val eventJson = savedStateHandle.get<String>(EDIT_EVENT_JSON_KEY)
    val event =
        eventJson?.let { json ->
            runCatching { WorkoutAppJson.decodeFromString<Event>(json) }.getOrNull()
        }

    savedStateHandle.remove<String>(EDIT_EVENT_JSON_KEY)

    return EditEventNavArgs(
        eventId = eventId,
        source = source,
        event = event
    )
}
