package com.example.jetpack_workoutapp.network

import com.example.jetpack_workoutapp.model.Event
import retrofit2.http.GET

interface SWApi {
    @GET("trainings/last")
    suspend fun getPastEvents(): List<Event>
}