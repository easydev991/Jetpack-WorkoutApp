package com.swparks.domain.usecase

import com.swparks.data.repository.SWRepository

class DeleteParkUseCase(
    private val swRepository: SWRepository
) {
    suspend operator fun invoke(parkId: Long): Result<Unit> = swRepository.removeParkLocally(parkId)
}
