package com.swparks.domain.usecase

import com.swparks.data.model.Event

interface IGetFutureEventsUseCase {
    suspend operator fun invoke(): Result<List<Event>>
}
