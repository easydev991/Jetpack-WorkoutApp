package com.example.jetpack_workoutapp.data

import com.example.jetpack_workoutapp.model.Event
import com.example.jetpack_workoutapp.network.SWApi

interface SWRepository {
    /** Fetches list of MarsPhoto from marsApi */
    suspend fun getPastEvents(): List<Event>
}

class SWNetworkRepository(private val swApi: SWApi) : SWRepository {
    /** Загружает список прошедших мероприятий */
    override suspend fun getPastEvents(): List<Event> = swApi.getPastEvents()
}