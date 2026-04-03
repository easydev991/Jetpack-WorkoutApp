package com.swparks.domain.usecase

import com.swparks.domain.model.Journal
import com.swparks.domain.repository.JournalsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case для получения списка дневников пользователя.
 *
 * Делегирует вызов репозиторию дневников. Предоставляет Single Source of Truth (SSOT)
 * через Flow для подписки UI на изменения дневников.
 *
 * @param journalsRepository Репозиторий для работы с дневниками
 */
class GetJournalsUseCase(
    private val journalsRepository: JournalsRepository
) : IGetJournalsUseCase {
    /**
     * Получить поток дневников пользователя.
     *
     * @param userId Идентификатор пользователя
     * @return Flow со списком дневников, отсортированным по дате изменения
     */
    override operator fun invoke(userId: Long): Flow<List<Journal>> =
        journalsRepository.observeJournals(userId)
}
