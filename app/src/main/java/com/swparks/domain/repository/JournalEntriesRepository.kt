package com.swparks.domain.repository

import com.swparks.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с записями в дневнике
 *
 * Предоставляет Single Source of Truth (SSOT) для записей через Flow
 * и методы для обновления данных с сервера
 */
interface JournalEntriesRepository {
    /**
     * Получить поток записей дневника (SSOT)
     *
     * @param userId Идентификатор пользователя
     * @param journalId Идентификатор дневника
     * @return Flow со списком записей, отсортированным по дате изменения
     */
    fun observeJournalEntries(userId: Long, journalId: Long): Flow<List<JournalEntry>>

    /**
     * Обновить записи дневника с сервера
     *
     * @param userId Идентификатор пользователя
     * @param journalId Идентификатор дневника
     * @return Result успеха или ошибки операции
     */
    suspend fun refreshJournalEntries(userId: Long, journalId: Long): Result<Unit>

    /**
     * Удалить запись из дневника
     *
     * @param userId Идентификатор пользователя
     * @param journalId Идентификатор дневника
     * @param entryId Идентификатор записи
     * @return Result успеха или ошибки операции
     */
    suspend fun deleteJournalEntry(
        userId: Long,
        journalId: Long,
        entryId: Long
    ): Result<Unit>

    /**
     * Проверить, можно ли удалить запись
     *
     * Первую запись в дневнике (с минимальным id) нельзя удалить
     *
     * @param entryId Идентификатор записи
     * @param journalId Идентификатор дневника
     * @return true если удаление разрешено, false если это первая запись
     */
    suspend fun canDeleteEntry(entryId: Long, journalId: Long): Boolean
}
