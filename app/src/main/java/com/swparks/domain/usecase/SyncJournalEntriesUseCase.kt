package com.swparks.domain.usecase

import android.util.Log
import com.swparks.domain.repository.JournalEntriesRepository

/**
 * Use case для синхронизации записей дневника с сервером.
 *
 * Делегирует вызов репозиторию записей для обновления данных с сервера.
 * Обновленные данные сохраняются в локальную базу данных.
 *
 * @param journalEntriesRepository Репозиторий для работы с записями в дневнике
 */
class SyncJournalEntriesUseCase(
    private val journalEntriesRepository: JournalEntriesRepository
) : ISyncJournalEntriesUseCase {
    /**
     * Обновить записи дневника с сервера.
     *
     * @param userId Идентификатор пользователя
     * @param journalId Идентификатор дневника
     * @return Result успеха или ошибки операции
     */
    override suspend operator fun invoke(
        userId: Long,
        journalId: Long
    ): Result<Unit> {
        Log.i(TAG, "Синхронизация записей дневника: userId=$userId, journalId=$journalId")
        return journalEntriesRepository
            .refreshJournalEntries(userId, journalId)
            .onFailure { error ->
                Log.e(TAG, "Ошибка при синхронизации записей: ${error.message}")
            }
    }

    private companion object {
        const val TAG = "SyncJournalEntriesUseCase"
    }
}
