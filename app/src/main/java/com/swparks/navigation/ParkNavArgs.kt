package com.swparks.navigation

import androidx.navigation.NavBackStackEntry
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.data.model.User
import com.swparks.util.WorkoutAppJson

internal const val EDIT_PARK_JSON_KEY = "edit_park_json"
internal const val CREATE_PARK_DRAFT_JSON_KEY = "create_park_draft_json"

internal data class EditParkNavArgs(
    val parkId: Long,
    val source: String,
    val park: Park?
)

internal fun NavBackStackEntry.consumeEditParkArgs(): EditParkNavArgs? {
    val parkId = arguments?.getLong("parkId") ?: return null
    val source = arguments?.getString("source") ?: "parks"
    val parkJson = savedStateHandle.get<String>(EDIT_PARK_JSON_KEY)
    val park = parkJson?.let { json ->
        runCatching { WorkoutAppJson.decodeFromString<Park>(json) }.getOrNull()
    }

    savedStateHandle.remove<String>(EDIT_PARK_JSON_KEY)

    return EditParkNavArgs(
        parkId = parkId,
        source = source,
        park = park
    )
}

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

internal data class CreateParkNavArgs(
    val source: String,
    val draft: NewParkDraft?
)

internal fun NavBackStackEntry.consumeCreateParkArgs(): CreateParkNavArgs {
    val source = arguments?.getString("source") ?: "parks"
    val draftJson = savedStateHandle.get<String>(CREATE_PARK_DRAFT_JSON_KEY)
    val draft = draftJson?.let { json ->
        runCatching { WorkoutAppJson.decodeFromString<NewParkDraft>(json) }.getOrNull()
    }

    savedStateHandle.remove<String>(CREATE_PARK_DRAFT_JSON_KEY)

    return CreateParkNavArgs(
        source = source,
        draft = draft
    )
}
