package com.swparks.domain.model

/**
 * Пол пользователя
 *
 * @property rawValue Числовой код пола (0 - мужской, 1 - женский)
 */
enum class Gender(
    val rawValue: Int
) {
    MALE(rawValue = 0),
    FEMALE(rawValue = 1)
}
