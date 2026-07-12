package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.repository.ParksEventsRepository
import kotlinx.coroutines.flow.Flow

class GetPastEventsFlowUseCase(
    private val parksEventsRepository: ParksEventsRepository
) {
    operator fun invoke(): Flow<List<Event>> = parksEventsRepository.getPastEventsFlow()
}
