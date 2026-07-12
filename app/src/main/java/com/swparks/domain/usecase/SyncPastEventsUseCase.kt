package com.swparks.domain.usecase

import com.swparks.data.repository.ParksEventsRepository

class SyncPastEventsUseCase(
    private val parksEventsRepository: ParksEventsRepository
) {
    suspend operator fun invoke(): Result<Unit> = parksEventsRepository.syncPastEvents()
}
