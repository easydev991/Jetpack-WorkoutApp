package com.swparks.ui.model

import com.swparks.R

/**
 * Режим работы экрана ввода текста
 *
 * Используется для определения типа операции (создание/редактирование),
 * заголовка экрана и логики валидации.
 */
sealed class TextEntryMode {
    // Новые записи
    data class NewForPark(val parkId: Long) : TextEntryMode()
    data class NewForEvent(val eventId: Long) : TextEntryMode()
    data class NewForJournal(val ownerId: Long, val journalId: Long) : TextEntryMode()

    // Редактирование
    data class EditPark(val editInfo: EditInfo) : TextEntryMode()
    data class EditEvent(val editInfo: EditInfo) : TextEntryMode()
    data class EditJournalEntry(val ownerId: Long, val editInfo: EditInfo) : TextEntryMode()
}

/**
 * Возвращает ID строкового ресурса для заголовка экрана
 */
fun TextEntryMode.getTitle(): Int = when (this) {
    is TextEntryMode.NewForPark, is TextEntryMode.NewForEvent -> R.string.new_comment_title
    is TextEntryMode.NewForJournal -> R.string.new_entry_title
    is TextEntryMode.EditPark, is TextEntryMode.EditEvent -> R.string.edit_comment_title
    is TextEntryMode.EditJournalEntry -> R.string.edit_entry_title
}

/**
 * Возвращает ID строкового ресурса для placeholder текстового поля (или null, если placeholder не нужен)
 */
fun TextEntryMode.getPlaceholder(): Int? = when (this) {
    is TextEntryMode.NewForJournal -> R.string.new_entry_placeholder
    else -> null
}
