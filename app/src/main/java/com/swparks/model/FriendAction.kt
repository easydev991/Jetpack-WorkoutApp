package com.swparks.model

import androidx.annotation.StringRes
import com.swparks.R

/**
 * Действие со списком друзей по отношению к другому пользователю (добавить/удалить)
 */
enum class FriendAction(@param:StringRes val description: Int) {
    SEND_FRIEND_REQUEST(R.string.send_friend_request),
    REMOVE_FRIEND(R.string.remove_friend)
}

/**
 * Преобразует UI-модель FriendAction в API-модель ApiFriendAction
 */
fun FriendAction.toApiAction(): ApiFriendAction = when (this) {
    FriendAction.SEND_FRIEND_REQUEST -> ApiFriendAction.ADD
    FriendAction.REMOVE_FRIEND -> ApiFriendAction.REMOVE
}