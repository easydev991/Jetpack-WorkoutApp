package com.swparks.domain.usecase

import com.swparks.analytics.AnalyticsEvent
import com.swparks.analytics.AnalyticsService
import com.swparks.analytics.AppErrorOperation
import com.swparks.data.UserPreferencesRepository
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.util.Clock
import com.swparks.domain.util.isUpdateNeeded
import com.swparks.util.Logger
import kotlinx.coroutines.flow.first

class SyncCountriesUseCase(
    private val clock: Clock,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val countriesRepository: CountriesRepository,
    private val logger: Logger,
    private val analyticsService: AnalyticsService
) {
    companion object {
        private const val TAG = "SyncCountriesUseCase"
    }

    suspend operator fun invoke(): Result<Unit> {
        val lastUpdateDate = userPreferencesRepository.lastCountriesUpdateDate.first()

        val shouldSkipSync = !lastUpdateDate.isUpdateNeeded(clock)
        if (shouldSkipSync) {
            logger.d(
                TAG,
                "Обновление справочника стран не требуется, последнее обновление: $lastUpdateDate"
            )
        } else {
            logger.i(TAG, "Проверка необходимости обновления справочника стран")

            val result = countriesRepository.updateCountriesFromServer()
            if (result.isFailure) {
                val exception = result.exceptionOrNull() ?: Exception("Unknown error")
                logger.e(TAG, "Ошибка обновления справочника стран", exception)
                analyticsService.log(
                    AnalyticsEvent.AppError(AppErrorOperation.COUNTRIES_UPDATE_FAILED, exception)
                )
                return Result.failure(exception)
            }

            logger.i(TAG, "Справочник стран успешно обновлен с сервера")
            userPreferencesRepository.setLastCountriesUpdateDate(clock.nowIsoString())
        }

        return Result.success(Unit)
    }
}
