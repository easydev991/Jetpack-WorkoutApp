package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository

class SyncPastEventsUseCase(
    private val swRepository: SWRepository
) : ISyncPastEventsUseCase {
    override suspend fun invoke(): Result<Unit> = swRepository.syncPastEvents()
}
