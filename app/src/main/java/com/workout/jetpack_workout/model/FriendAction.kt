package com.workout.jetpack_workout.model

import androidx.annotation.StringRes
import com.workout.jetpack_workout.R

/**
 * Действие со списком друзей по отношению к другому пользователю (добавить/удалить)
 */
enum class FriendAction(@StringRes val description: Int) {
    SEND_FRIEND_REQUEST(R.string.send_friend_request),
    REMOVE_FRIEND(R.string.remove_friend)
}