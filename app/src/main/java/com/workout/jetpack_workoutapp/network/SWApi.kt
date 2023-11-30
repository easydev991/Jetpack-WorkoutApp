package com.workout.jetpack_workoutapp.network

import com.workout.jetpack_workoutapp.model.Event
import retrofit2.http.GET

interface SWApi {
    @GET("trainings/last")
    suspend fun getPastEvents(): List<Event>
}