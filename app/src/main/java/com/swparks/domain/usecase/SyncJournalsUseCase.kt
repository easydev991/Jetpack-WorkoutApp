package com.swparks.domain.usecase

import android.util.Log
import com.swparks.domain.repository.JournalsRepository

/**
 * Use case для синхронизации дневников пользователя с сервера.
 *
 * Делегирует вызов репозиторию дневников для обновления данных с сервера.
 * Обновленные данные сохраняются в локальную базу данных.
 *
 * @param journalsRepository Репозиторий для работы с дневниками
 */
class SyncJournalsUseCase(private val journalsRepository: JournalsRepository) :
    ISyncJournalsUseCase {
    /**
     * Обновить дневники пользователя с сервера.
     *
     * @param userId Идентификатор пользователя
     * @return Result успеха или ошибки операции
     */
    override suspend operator fun invoke(userId: Long): Result<Unit> {
        Log.i("SyncJournalsUseCase", "Запуск синхронизации дневников для пользователя: $userId")
        return journalsRepository.refreshJournals(userId)
    }
}
