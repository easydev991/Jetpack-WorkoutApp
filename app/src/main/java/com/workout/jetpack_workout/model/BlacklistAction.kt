package com.workout.jetpack_workout.model

import androidx.annotation.StringRes
import com.workout.jetpack_workout.R

/**
 * Действие с чёрным списком по отношению к другому пользователю (заблокировать/разблокировать)
 */
enum class BlacklistAction {
    BLOCK {
        override val model = Model(
            description = R.string.block,
            alertMessage = R.string.user_will_be_blocked
        )
    },
    UNBLOCK {
        override val model = Model(
            description = R.string.unblock,
            alertMessage = R.string.user_will_be_unblocked
        )
    };

    abstract val model: Model

    data class Model(
        @StringRes
        val description: Int,
        @StringRes
        val alertMessage: Int
    )
}