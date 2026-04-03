package com.swparks.domain.usecase

/**
 * Интерфейс для use case проверки возможности удаления записи из дневника.
 * Создан для удобства тестирования ViewModels.
 */
interface ICanDeleteJournalEntryUseCase {
    /**
     * Проверить, можно ли удалить запись.
     *
     * Первую запись в дневнике (с минимальным id) нельзя удалить.
     *
     * @param entryId Идентификатор записи
     * @param journalId Идентификатор дневника
     * @return true если удаление разрешено, false если это первая запись
     */
    suspend operator fun invoke(
        entryId: Long,
        journalId: Long
    ): Boolean
}
