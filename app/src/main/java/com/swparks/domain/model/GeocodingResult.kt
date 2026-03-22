package com.swparks.domain.model

/**
 * Результат геокодирования обратного геокодирования (reverse geocoding).
 *
 * @property address Полный адрес
 * @property locality Город/населенный пункт (опционально)
 * @property countryName Название страны (опционально)
 */
data class GeocodingResult(
    val address: String,
    val locality: String?,
    val countryName: String?
)
