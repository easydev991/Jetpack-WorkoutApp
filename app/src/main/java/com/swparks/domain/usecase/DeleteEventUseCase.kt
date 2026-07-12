package com.swparks.domain.usecase

import com.swparks.data.repository.ParksEventsRepository

class DeleteEventUseCase(
    private val parksEventsRepository: ParksEventsRepository
) {
    suspend operator fun invoke(eventId: Long): Result<Unit> = parksEventsRepository.removeEventLocally(eventId)
}
