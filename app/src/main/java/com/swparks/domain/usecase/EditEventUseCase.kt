package com.swparks.domain.usecase

import com.swparks.data.repository.ParksEventsRepository
import com.swparks.domain.model.Event
import com.swparks.domain.model.EventForm

class EditEventUseCase(
    private val parksEventsRepository: ParksEventsRepository
) {
    suspend operator fun invoke(
        eventId: Long,
        form: EventForm,
        photos: List<ByteArray>? = null
    ): Result<Event> = parksEventsRepository.saveEvent(id = eventId, form = form, photos = photos)
}
