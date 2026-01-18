package com.swparks.utils

import kotlinx.serialization.json.Json

/**
 * Объект с настройками Json для десериализации данных
 */
val WorkoutAppJson = Json {
    isLenient = true
    ignoreUnknownKeys = true
}