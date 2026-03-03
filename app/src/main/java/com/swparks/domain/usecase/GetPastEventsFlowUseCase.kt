package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.repository.SWRepository
import kotlinx.coroutines.flow.Flow

class GetPastEventsFlowUseCase(
    private val swRepository: SWRepository
) : IGetPastEventsFlowUseCase {
    override operator fun invoke(): Flow<List<Event>> {
        return swRepository.getPastEventsFlow()
    }
}
