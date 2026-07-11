package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository

class SyncFutureEventsUseCase(
    private val swRepository: SWRepository
) {
    suspend operator fun invoke(): Result<Unit> = swRepository.syncFutureEvents()
}
