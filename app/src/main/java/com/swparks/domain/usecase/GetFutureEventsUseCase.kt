package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.EventType

class GetFutureEventsUseCase(
    private val swRepository: SWRepository
) : IGetFutureEventsUseCase {
    override suspend fun invoke(): Result<List<Event>> {
        return swRepository.getEvents(EventType.FUTURE)
    }
}
