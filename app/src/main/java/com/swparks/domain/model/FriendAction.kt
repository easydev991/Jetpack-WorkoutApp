package com.swparks.domain.model

import com.swparks.R

enum class FriendAction(
    val description: Int
) {
    SEND_FRIEND_REQUEST(R.string.send_friend_request),
    REMOVE_FRIEND(R.string.remove_friend)
}
