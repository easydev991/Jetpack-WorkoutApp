package com.swparks.domain.usecase

interface ISyncFutureEventsUseCase {
    suspend operator fun invoke(): Result<Unit>
}
