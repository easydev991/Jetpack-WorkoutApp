package com.swparks.data.model

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
