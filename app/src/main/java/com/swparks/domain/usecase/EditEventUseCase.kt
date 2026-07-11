package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.EventForm

class EditEventUseCase(
    private val repository: SWRepository
) {
    suspend operator fun invoke(
        eventId: Long,
        form: EventForm,
        photos: List<ByteArray>? = null
    ): Result<Event> = repository.saveEvent(id = eventId, form = form, photos = photos)
}
