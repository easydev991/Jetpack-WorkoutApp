package com.swparks.domain.model

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
