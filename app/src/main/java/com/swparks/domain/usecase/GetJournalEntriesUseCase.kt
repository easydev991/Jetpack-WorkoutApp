package com.swparks.domain.usecase

import android.util.Log
import com.swparks.domain.model.JournalEntry
import com.swparks.domain.repository.JournalEntriesRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case для получения списка записей в дневнике.
 *
 * Делегирует вызов репозиторию записей. Предоставляет Single Source of Truth (SSOT)
 * через Flow для подписки UI на изменения записей в дневнике.
 *
 * @param journalEntriesRepository Репозиторий для работы с записями в дневнике
 */
class GetJournalEntriesUseCase(
    private val journalEntriesRepository: JournalEntriesRepository
) : IGetJournalEntriesUseCase {
    companion object {
        private const val TAG = "GetJournalEntriesUseCase"
    }

    /**
     * Получить поток записей в дневнике.
     *
     * @param userId Идентификатор пользователя
     * @param journalId Идентификатор дневника
     * @return Flow со списком записей, отсортированным по дате изменения
     */
    override operator fun invoke(userId: Long, journalId: Long): Flow<List<JournalEntry>> {
        Log.i(TAG, "Наблюдение за записями дневника: userId=$userId, journalId=$journalId")
        return journalEntriesRepository.observeJournalEntries(userId, journalId)
    }
}
