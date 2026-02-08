package com.swparks.domain.repository

import com.swparks.domain.model.Journal
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с дневниками
 *
 * Предоставляет Single Source of Truth (SSOT) для дневников через Flow
 * и методы для обновления данных с сервера
 */
interface JournalsRepository {
    /**
     * Получить поток дневников пользователя (SSOT)
     *
     * @param userId Идентификатор пользователя
     * @return Flow со списком дневников, отсортированным по дате изменения
     */
    fun observeJournals(userId: Long): Flow<List<Journal>>

    /**
     * Обновить дневники пользователя с сервера
     *
     * @param userId Идентификатор пользователя
     * @return Result успеха или ошибки операции
     */
    suspend fun refreshJournals(userId: Long): Result<Unit>
}
