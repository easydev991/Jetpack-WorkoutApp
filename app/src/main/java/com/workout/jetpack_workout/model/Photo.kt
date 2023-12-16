package com.workout.jetpack_workout.model

import kotlinx.serialization.Serializable

/**
 * Модель фотографии
 */
@Serializable
data class Photo (
    val id: Long,
    val photo: String
)
