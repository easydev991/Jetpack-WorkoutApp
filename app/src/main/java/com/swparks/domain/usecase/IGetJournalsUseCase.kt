package com.swparks.domain.usecase

import com.swparks.domain.model.Journal
import kotlinx.coroutines.flow.Flow

/** Интерфейс для use case получения списка дневников. Создан для удобства тестирования ViewModels. */
interface IGetJournalsUseCase {
    operator fun invoke(userId: Long): Flow<List<Journal>>
}
