package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.EventForm

class EditEventUseCase(
    private val repository: SWRepository
) : IEditEventUseCase {
    override suspend fun invoke(
        eventId: Long,
        form: EventForm,
        photos: List<ByteArray>?
    ): Result<Event> = repository.saveEvent(id = eventId, form = form, photos = photos)
}
