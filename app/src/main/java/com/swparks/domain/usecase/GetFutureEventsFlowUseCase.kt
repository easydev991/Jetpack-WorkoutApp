package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.repository.SWRepository
import kotlinx.coroutines.flow.Flow

class GetFutureEventsFlowUseCase(
    private val swRepository: SWRepository
) : IGetFutureEventsFlowUseCase {
    override fun invoke(): Flow<List<Event>> = swRepository.getFutureEventsFlow()
}
