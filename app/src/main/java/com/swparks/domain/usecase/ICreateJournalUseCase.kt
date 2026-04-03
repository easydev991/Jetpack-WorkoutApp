package com.swparks.domain.usecase

/**
 * Интерфейс use case для создания дневника
 */
interface ICreateJournalUseCase {
    /**
     * Создать новый дневник
     *
     * @param userId ID пользователя
     * @param title Название дневника
     * @return Result<Unit> результат операции
     */
    suspend operator fun invoke(
        userId: Long,
        title: String
    ): Result<Unit>
}
