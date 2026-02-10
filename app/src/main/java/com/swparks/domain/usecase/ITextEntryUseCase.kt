package com.swparks.domain.usecase

/**
 * Интерфейс для use case добавления/редактирования комментариев и записей в дневнике.
 * Создан для удобства тестирования ViewModels.
 */
interface ITextEntryUseCase {
    /**
     * Добавить запись в дневник
     */
    suspend fun addJournalEntry(ownerId: Long, journalId: Long, text: String): Result<Unit>

    /**
     * Редактировать запись в дневнике
     */
    suspend fun editJournalEntry(
        ownerId: Long,
        journalId: Long,
        entryId: Long,
        text: String
    ): Result<Unit>

    /**
     * Создать новый дневник
     */
    suspend fun createJournal(userId: Long, title: String): Result<Unit>

    /**
     * Добавить комментарий к площадке
     */
    suspend fun addParkComment(parkId: Long, text: String): Result<Unit>

    /**
     * Редактировать комментарий к площадке
     */
    suspend fun editParkComment(parkId: Long, commentId: Long, text: String): Result<Unit>

    /**
     * Добавить комментарий к мероприятию
     */
    suspend fun addEventComment(eventId: Long, text: String): Result<Unit>

    /**
     * Редактировать комментарий к мероприятию
     */
    suspend fun editEventComment(eventId: Long, commentId: Long, text: String): Result<Unit>
}
