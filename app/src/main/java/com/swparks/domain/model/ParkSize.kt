package com.swparks.domain.model

/**
 * Размер площадки
 */
enum class ParkSize(
    val rawValue: Int
) {
    SMALL(rawValue = 1),
    MEDIUM(rawValue = 2),
    LARGE(rawValue = 3)
}
