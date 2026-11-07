package com.swparks.domain.model

/**
 * Тип площадки
 */
enum class ParkType(
    val rawValue: Int
) {
    SOVIET(rawValue = 1),
    MODERN(rawValue = 2),
    COLLARS(rawValue = 3),
    LEGENDARY(rawValue = 6)
}
