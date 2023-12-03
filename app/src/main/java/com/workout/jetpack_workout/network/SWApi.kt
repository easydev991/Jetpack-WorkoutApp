package com.workout.jetpack_workout.network

import com.workout.jetpack_workout.model.Event
import retrofit2.http.GET

interface SWApi {
    @GET("trainings/last")
    suspend fun getPastEvents(): List<Event>
}