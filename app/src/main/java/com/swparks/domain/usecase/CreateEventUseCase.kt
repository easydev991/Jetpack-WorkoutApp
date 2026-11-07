package com.swparks.domain.usecase

import com.swparks.data.repository.ParksEventsRepository
import com.swparks.domain.model.Event
import com.swparks.domain.model.EventForm

class CreateEventUseCase(
    private val parksEventsRepository: ParksEventsRepository
) {
    suspend operator fun invoke(
        form: EventForm,
        photos: List<ByteArray>? = null
    ): Result<Event> = parksEventsRepository.saveEvent(id = null, form = form, photos = photos)
}
