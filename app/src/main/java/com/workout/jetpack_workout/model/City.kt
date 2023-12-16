package com.workout.jetpack_workout.model

import kotlinx.serialization.Serializable

/**
 * Модель города
 */
@Serializable
data class City(
    val id: String,
    val name: String,
    val lat: String,
    val lon: String
)
