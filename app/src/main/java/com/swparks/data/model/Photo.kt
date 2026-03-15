package com.swparks.data.model

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

/**
 * Удаляет фотографию по ID и перенумеровывает оставшиеся.
 * Сервер перенумеровывает фото после удаления (ID начинаются с 1).
 *
 * @param photoId ID фотографии для удаления
 * @return Новый список с перенумерованными фотографиями
 */
fun List<Photo>.removePhotoById(photoId: Long): List<Photo> =
    filter { it.id != photoId }
        .mapIndexed { index, photo -> Photo(id = index + 1L, photo = photo.photo) }
