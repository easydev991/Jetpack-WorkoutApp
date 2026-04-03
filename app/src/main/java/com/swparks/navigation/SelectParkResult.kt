package com.swparks.navigation

import androidx.navigation.NavBackStackEntry

internal const val SELECTED_PARK_ID_KEY = "selectedParkId"
internal const val SELECTED_PARK_NAME_KEY = "selectedParkName"

data class SelectedParkResult(
    val parkId: Long,
    val parkName: String
)

fun NavBackStackEntry.setSelectedParkResult(
    parkId: Long,
    parkName: String
) {
    savedStateHandle[SELECTED_PARK_ID_KEY] = parkId
    savedStateHandle[SELECTED_PARK_NAME_KEY] = parkName
}

fun NavBackStackEntry.consumeSelectedParkResult(): SelectedParkResult? {
    val parkId = savedStateHandle.get<Long>(SELECTED_PARK_ID_KEY)
    val parkName = savedStateHandle.get<String>(SELECTED_PARK_NAME_KEY)
    val result =
        if (parkId != null && parkName != null) {
            SelectedParkResult(parkId, parkName)
        } else {
            null
        }

    if (result != null) {
        savedStateHandle.remove<Long>(SELECTED_PARK_ID_KEY)
        savedStateHandle.remove<String>(SELECTED_PARK_NAME_KEY)
    }

    return result
}
