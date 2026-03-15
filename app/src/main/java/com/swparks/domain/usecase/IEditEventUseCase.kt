package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.ui.model.EventForm

interface IEditEventUseCase {
    suspend operator fun invoke(
        eventId: Long,
        form: EventForm,
        photos: List<ByteArray>? = null
    ): Result<Event>
}
