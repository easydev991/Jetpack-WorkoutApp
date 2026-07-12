package com.swparks.domain.usecase

import com.swparks.data.repository.ParksEventsRepository

class DeleteParkUseCase(
    private val parksEventsRepository: ParksEventsRepository
) {
    suspend operator fun invoke(parkId: Long): Result<Unit> = parksEventsRepository.removeParkLocally(parkId)
}
