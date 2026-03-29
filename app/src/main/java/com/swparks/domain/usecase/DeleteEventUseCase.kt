package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository

class DeleteEventUseCase(
    private val swRepository: SWRepository
) {
    suspend operator fun invoke(eventId: Long): Result<Unit> {
        return swRepository.removeEventLocally(eventId)
    }
}
