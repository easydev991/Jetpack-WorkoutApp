package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.repository.ParksEventsRepository
import com.swparks.ui.model.EventForm

class CreateEventUseCase(
    private val parksEventsRepository: ParksEventsRepository
) {
    suspend operator fun invoke(
        form: EventForm,
        photos: List<ByteArray>? = null
    ): Result<Event> = parksEventsRepository.saveEvent(id = null, form = form, photos = photos)
}
