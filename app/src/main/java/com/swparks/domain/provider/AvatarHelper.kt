package com.swparks.domain.provider

import android.net.Uri

/**
 * Интерфейс для работы с аватарами (изображениями).
 *
 * Используется в ViewModel для работы с Uri изображений
 * без прямой зависимости от Android Context.
 */
interface AvatarHelper {
    /**
     * Проверяет, поддерживается ли MIME-тип изображения.
     *
     * @param uri Uri изображения
     * @return true если MIME-тип поддерживается (jpeg, png, webp)
     */
    fun isSupportedMimeType(uri: Uri): Boolean

    /**
     * Конвертирует Uri в ByteArray.
     *
     * @param uri Uri для конвертации
     * @return Result.success(ByteArray) или Result.failure с ошибкой
     */
    fun uriToByteArray(uri: Uri): Result<ByteArray>
}
