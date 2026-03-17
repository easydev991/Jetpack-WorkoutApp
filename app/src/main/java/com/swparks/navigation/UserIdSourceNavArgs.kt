package com.swparks.navigation

import androidx.navigation.NavBackStackEntry

internal data class UserIdSourceNavArgs(
    val userId: Long,
    val source: String
)

internal fun NavBackStackEntry.consumeUserIdSourceArgs(defaultSource: String = "profile"): UserIdSourceNavArgs? {
    val userId = arguments?.getString("userId")?.toLongOrNull() ?: return null
    val source = arguments?.getString("source") ?: defaultSource

    return UserIdSourceNavArgs(
        userId = userId,
        source = source
    )
}
