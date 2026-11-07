package com.swparks.domain.model

/**
 * Действие с друзьями по отношению к другому пользователю (добавить/удалить).
 * Не зависит от Android-ресурсов: подпись выбирается в UI-слое
 */
enum class FriendAction {
    SEND_FRIEND_REQUEST,
    REMOVE_FRIEND
}
