package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.ui.model.EventForm

interface ICreateEventUseCase {
    suspend operator fun invoke(
        form: EventForm,
        photos: List<ByteArray>? = null
    ): Result<Event>
}
