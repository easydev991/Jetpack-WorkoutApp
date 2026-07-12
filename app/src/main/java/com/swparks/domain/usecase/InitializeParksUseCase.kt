package com.swparks.domain.usecase

import android.content.Context
import com.swparks.data.repository.ParksEventsRepository
import com.swparks.util.Logger
import kotlinx.coroutines.CancellationException

class InitializeParksUseCase(
    private val context: Context,
    private val parksEventsRepository: ParksEventsRepository,
    private val logger: Logger
) {
    suspend operator fun invoke(): Result<Unit> =
        kotlin
            .runCatching {
                logger.d(TAG, "Импорт seed parks в Room")
                parksEventsRepository.importSeedParks(context)
            }.onFailure { error ->
                if (error is CancellationException) {
                    throw error
                }
                logger.e(TAG, "Ошибка инициализации площадок", error)
            }

    companion object {
        private const val TAG = "InitializeParksUseCase"
    }
}
