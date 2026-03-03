package com.swparks.domain.usecase

interface ISyncPastEventsUseCase {
    suspend operator fun invoke(): Result<Unit>
}
