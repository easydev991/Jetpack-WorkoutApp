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
        private const val ISO_DATE_LENGTH = 10
    }

    suspend operator fun invoke(force: Boolean = false): Result<Unit> {
        val lastUpdateDate = userPreferencesRepository.lastParksUpdateDate.first()

        val shouldSkipSync = !force && !lastUpdateDate.isUpdateNeeded(clock)
        if (shouldSkipSync) {
            logger.d(TAG, "Обновление площадок не требуется, последнее обновление: $lastUpdateDate")
        } else {
            logger.i(TAG, "Проверка необходимости обновления площадок")

            val dateOnly = lastUpdateDate.take(ISO_DATE_LENGTH)
            val result = swRepository.getUpdatedParks(dateOnly)
            if (result.isFailure) {
                logger.e(TAG, "Ошибка обновления площадок", result.exceptionOrNull())
                return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val parks = result.getOrNull() ?: emptyList()
            if (parks.isNotEmpty()) {
                swRepository.upsertParks(parks)
                logger.i(TAG, "Обновлено ${parks.size} площадок с сервера")
            } else {
                logger.i(TAG, "Обновление площадок с сервера: изменений нет")
            }

            userPreferencesRepository.setLastParksUpdateDate(clock.nowIsoString())
        }

        return Result.success(Unit)
    }
}
