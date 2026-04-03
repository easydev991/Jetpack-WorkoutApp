package com.swparks.ui.model

import android.content.res.Resources
import com.swparks.R

/**
 * Режим работы экрана ввода текста
 *
 * Используется для определения типа операции (создание/редактирование),
 * заголовка экрана и логики валидации.
 */
sealed class TextEntryMode {
    // Новые записи
    data class NewForPark(
        val parkId: Long
    ) : TextEntryMode()

    data class NewForEvent(
        val eventId: Long
    ) : TextEntryMode()

    data class NewForJournal(
        val ownerId: Long,
        val journalId: Long
    ) : TextEntryMode()

    data class NewJournal(
        val userId: Long
    ) : TextEntryMode()

    // Редактирование
    data class EditPark(
        val editInfo: EditInfo
    ) : TextEntryMode()

    data class EditEvent(
        val editInfo: EditInfo
    ) : TextEntryMode()

    data class EditJournalEntry(
        val ownerId: Long,
        val editInfo: EditInfo
    ) : TextEntryMode()

    // Сообщение пользователю
    data class Message(
        val userId: Long,
        val userName: String
    ) : TextEntryMode()
}

/**
 * Возвращает отформатированный заголовок для экрана ввода текста
 *
 * Для Message режима форматирует строку с именем пользователя
 */
fun TextEntryMode.getFormattedTitle(resources: Resources): String =
    when (this) {
        is TextEntryMode.Message -> resources.getString(R.string.message_for_user, userName)
        is TextEntryMode.NewForPark, is TextEntryMode.NewForEvent -> resources.getString(R.string.new_comment_title)
        is TextEntryMode.NewForJournal -> resources.getString(R.string.new_entry_title)
        is TextEntryMode.NewJournal -> resources.getString(R.string.new_journal)
        is TextEntryMode.EditPark, is TextEntryMode.EditEvent -> resources.getString(R.string.edit_comment_title)
        is TextEntryMode.EditJournalEntry -> resources.getString(R.string.edit_entry_title)
    }

/**
 * Возвращает ID строкового ресурса для placeholder текстового поля (или null, если placeholder не нужен)
 */
fun TextEntryMode.getPlaceholder(): Int? =
    when (this) {
        is TextEntryMode.NewForJournal -> R.string.new_entry_placeholder
        is TextEntryMode.NewJournal -> R.string.new_journal_placeholder
        is TextEntryMode.Message -> R.string.message_placeholder
        else -> null
    }
