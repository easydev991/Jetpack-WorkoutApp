package com.swparks.data.model

import kotlinx.serialization.Serializable

/**
 * Модель страны
 */
@Serializable
data class Country(
    val id: String,
    val name: String,
    val cities: List<City>
)