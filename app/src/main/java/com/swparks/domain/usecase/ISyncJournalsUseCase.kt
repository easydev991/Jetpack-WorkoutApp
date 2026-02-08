package com.swparks.domain.usecase

/** Интерфейс для use case синхронизации дневников. Создан для удобства тестирования ViewModels. */
interface ISyncJournalsUseCase {
    suspend operator fun invoke(userId: Long): Result<Unit>
}
