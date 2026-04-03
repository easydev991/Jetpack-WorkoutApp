package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.EventForm

class CreateEventUseCase(
    private val repository: SWRepository
) : ICreateEventUseCase {
    override suspend fun invoke(
        form: EventForm,
        photos: List<ByteArray>?
    ): Result<Event> = repository.saveEvent(id = null, form = form, photos = photos)
}
