package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.EventForm

class CreateEventUseCase(
    private val repository: SWRepository
) {
    suspend operator fun invoke(
        form: EventForm,
        photos: List<ByteArray>? = null
    ): Result<Event> = repository.saveEvent(id = null, form = form, photos = photos)
}
