package com.swparks.model

import kotlinx.serialization.Serializable

/**
 * Модель фотографии
 * @property id Идентификатор
 * @property photo Ссылка на фотографию (может содержать кириллицу)
 */
@Serializable
data class Photo(
    val id: Long,
    val photo: String
)
