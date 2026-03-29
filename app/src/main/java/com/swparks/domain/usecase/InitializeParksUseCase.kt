package com.swparks.domain.usecase

import android.content.Context
import com.swparks.data.repository.SWRepository
import com.swparks.util.Logger

class InitializeParksUseCase(
    private val context: Context,
    private val swRepository: SWRepository,
    private val logger: Logger
) : IInitializeParksUseCase {

    override suspend fun invoke(): Result<Unit> {
        return runCatching {
            logger.d(TAG, "Импорт seed parks в Room")
            swRepository.importSeedParks(context)
            Result.success(Unit)
        }
    }

    private inline fun <T> runCatching(block: () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка инициализации парков", e)
            Result.failure<T>(e)
        }
    }

    companion object {
        private const val TAG = "InitializeParksUseCase"
    }
}
