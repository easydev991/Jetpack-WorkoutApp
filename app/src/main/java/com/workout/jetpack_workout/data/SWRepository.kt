package com.workout.jetpack_workout.data

import com.workout.jetpack_workout.model.Event
import com.workout.jetpack_workout.network.SWApi

interface SWRepository {
    /** Fetches list of MarsPhoto from marsApi */
    suspend fun getPastEvents(): List<Event>
}

class SWNetworkRepository(private val swApi: SWApi) : SWRepository {
    /** Загружает список прошедших мероприятий */
    override suspend fun getPastEvents(): List<Event> = swApi.getPastEvents()
}