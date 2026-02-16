package com.swparks.ui.model

import androidx.annotation.StringRes
import com.swparks.R
import com.swparks.data.model.ApiBlacklistOption

/**
 * Действие с чёрным списком по отношению к другому пользователю (заблокировать/разблокировать)
 */
enum class BlacklistAction(
    @param:StringRes val description: Int,
    @param:StringRes val alertMessage: Int,
    @param:StringRes val alertMessageDetailed: Int
) {
    BLOCK(
        description = R.string.block,
        alertMessage = R.string.user_will_be_blocked,
        alertMessageDetailed = R.string.block_user_alert
    ),
    UNBLOCK(
        description = R.string.unblock,
        alertMessage = R.string.user_will_be_unblocked,
        alertMessageDetailed = R.string.unblock_user_alert
    )
}

/**
 * Преобразует UI-модель BlacklistAction в API-модель ApiBlacklistOption
 */
fun BlacklistAction.toApiOption(): ApiBlacklistOption = when (this) {
    BlacklistAction.BLOCK -> ApiBlacklistOption.ADD
    BlacklistAction.UNBLOCK -> ApiBlacklistOption.REMOVE
}
