package com.swparks.model

/**
 * Тип записи комментария (площадка/мероприятие/дневник)
 */
sealed class TextEntryOption {
    data class Park(val id: Long) : TextEntryOption()
    data class Event(val id: Long) : TextEntryOption()
    data class Journal(
        val ownerId: Long,
        val journalId: Long,
        val entryId: Long? = null  // null - работа с самим дневником, not null - работа с записью в дневнике
    ) : TextEntryOption()
}
