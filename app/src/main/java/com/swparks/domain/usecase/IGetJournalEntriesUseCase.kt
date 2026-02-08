package com.swparks.domain.usecase

import com.swparks.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

/** Интерфейс для use case получения списка записей в дневнике. Создан для удобства тестирования ViewModels. */
interface IGetJournalEntriesUseCase {
    operator fun invoke(userId: Long, journalId: Long): Flow<List<JournalEntry>>
}
