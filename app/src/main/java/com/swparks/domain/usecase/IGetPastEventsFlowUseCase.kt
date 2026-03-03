package com.swparks.domain.usecase

import com.swparks.data.model.Event
import kotlinx.coroutines.flow.Flow

interface IGetPastEventsFlowUseCase {
    operator fun invoke(): Flow<List<Event>>
}
