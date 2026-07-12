package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.repository.ParksEventsRepository
import com.swparks.ui.model.EventForm

class EditEventUseCase(
    private val parksEventsRepository: ParksEventsRepository
) {
    suspend operator fun invoke(
        eventId: Long,
        form: EventForm,
        photos: List<ByteArray>? = null
    ): Result<Event> = parksEventsRepository.saveEvent(id = eventId, form = form, photos = photos)
}
