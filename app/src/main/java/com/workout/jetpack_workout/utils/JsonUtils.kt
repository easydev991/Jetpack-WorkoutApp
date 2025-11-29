package com.workout.jetpack_workout.utils

import kotlinx.serialization.json.Json

/**
 * Объект с настройками Json для десериализации данных
 */
val WorkoutAppJson = Json {
    isLenient = true
    ignoreUnknownKeys = true
}