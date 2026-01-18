package com.swparks.model

import androidx.annotation.StringRes
import com.swparks.R

/**
 * Действие с чёрным списком по отношению к другому пользователю (заблокировать/разблокировать)
 */
enum class BlacklistAction(
    @StringRes val description: Int,
    @StringRes val alertMessage: Int
) {
    BLOCK(
        description = R.string.block,
        alertMessage = R.string.user_will_be_blocked
    ),
    UNBLOCK(
        description = R.string.unblock,
        alertMessage = R.string.user_will_be_unblocked
    )
}