package com.swparks.ui.model

/**
 * Уровень доступа к дневнику
 */
enum class JournalAccess(val rawValue: Int) {
    ALL(0),      // Все пользователи
    FRIENDS(1),   // Только друзья
    NOBODY(2);    // Никто

    companion object {
        fun from(rawValue: Int?): JournalAccess {
            return when (rawValue) {
                0 -> ALL
                1 -> FRIENDS
                2 -> NOBODY
                else -> ALL
            }
        }
    }
}

/**
 * Проверяет возможность создания записи в дневнике
 *
 * @param journalOwnerId ID владельца дневника
 * @param mainUserId ID авторизованного пользователя (null если не авторизован)
 * @param mainUserFriendsIds Список ID друзей авторизованного пользователя
 * @return true - можно создать запись, false - нельзя
 */
fun JournalAccess.canCreateEntry(
    journalOwnerId: Long,
    mainUserId: Long?,
    mainUserFriendsIds: List<Long>
): Boolean {
    if (mainUserId == null) return false
    val isOwner = journalOwnerId == mainUserId
    // Проверяем, является ли владелец дневника другом текущего пользователя
    val isFriend = mainUserFriendsIds.contains(journalOwnerId)

    return when (this) {
        JournalAccess.ALL -> true // уже проверили, что mainUserId != null
        JournalAccess.FRIENDS -> isFriend || isOwner // владелец всегда может создавать записи
        JournalAccess.NOBODY -> isOwner
    }
}
