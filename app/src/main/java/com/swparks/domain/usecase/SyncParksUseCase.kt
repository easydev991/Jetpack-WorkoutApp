package com.swparks.domain.usecase

import com.swparks.data.UserPreferencesRepository
import com.swparks.data.repository.SWRepository
import com.swparks.domain.util.Clock
import com.swparks.domain.util.isUpdateNeeded
import com.swparks.util.Logger
import kotlinx.coroutines.flow.first

class SyncParksUseCase(
    private val clock: Clock,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val swRepository: SWRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "SyncParksUseCase"
    }

    suspend operator fun invoke(force: Boolean = false): Result<Unit> {
        val lastUpdateDate = userPreferencesRepository.lastParksUpdateDate.first()

        if (!force && !lastUpdateDate.isUpdateNeeded(clock)) {
            logger.d(TAG, "Обновление парков не требуется, последнее обновление: $lastUpdateDate")
            return Result.success(Unit)
        }

        logger.i(TAG, "Проверка необходимости обновления парков")

        val result = swRepository.getUpdatedParks(lastUpdateDate)
        if (result.isFailure) {
            logger.e(TAG, "Ошибка обновления парков", result.exceptionOrNull())
            return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }

        val parks = result.getOrNull() ?: emptyList()
        if (parks.isNotEmpty()) {
            swRepository.upsertParks(parks)
            logger.i(TAG, "Обновлено ${parks.size} парков с сервера")
        } else {
            logger.i(TAG, "Обновление парков с сервера: изменений нет")
        }

        userPreferencesRepository.setLastParksUpdateDate(clock.nowIsoString())

        return Result.success(Unit)
    }
}