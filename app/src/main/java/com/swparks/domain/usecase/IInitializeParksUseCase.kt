package com.swparks.domain.usecase

interface IInitializeParksUseCase {
    suspend operator fun invoke(): Result<Unit>
}
