package com.swparks.model

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
