package com.example.jetpack_workoutapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Начал делать по [гайду](https://www.geeksforgeeks.org/retrofit-with-kotlin-coroutine-in-android/)
 */
object RetrofitHelper {
    private const val baseUrl = "https://workout.su/api/v3"
    fun getInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}