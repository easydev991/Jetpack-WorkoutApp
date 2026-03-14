package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository

class SyncFutureEventsUseCase(
    private val swRepository: SWRepository
) : ISyncFutureEventsUseCase {
    override suspend fun invoke(): Result<Unit> = swRepository.syncFutureEvents()
}
