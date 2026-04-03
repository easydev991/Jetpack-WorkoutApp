package com.swparks.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * Утилиты для работы с изображениями.
 */
object ImageUtils {
    /**
     * Максимальный размер изображения в байтах (5 MB).
     */
    const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024

    /**
     * Поддерживаемые MIME-типы изображений.
     */
    val SUPPORTED_MIME_TYPES = listOf("image/jpeg", "image/png", "image/webp")

    // Константы для сжатия
    private const val INITIAL_QUALITY = 90
    private const val QUALITY_STEP = 10
    private const val MIN_QUALITY = 10
    private const val SCALE_FACTOR = 0.5f
    private const val FALLBACK_QUALITY = 80

    /**
     * Проверяет, поддерживается ли MIME-тип изображения.
     *
     * @param context Контекст приложения для доступа к ContentResolver
     * @param uri Uri изображения
     * @return true если MIME-тип поддерживается
     */
    fun isSupportedMimeType(
        context: Context,
        uri: Uri
    ): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType in SUPPORTED_MIME_TYPES
    }

    /**
     * Сжимает изображение, если оно превышает максимальный размер.
     *
     * @param data ByteArray с данными изображения
     * @param maxSizeBytes Максимальный размер в байтах
     * @return Сжатый ByteArray или исходный, если размер в норме
     */
    fun compressIfNeeded(
        data: ByteArray,
        maxSizeBytes: Int = MAX_IMAGE_SIZE_BYTES
    ): ByteArray {
        // Если размер в норме - возвращаем как есть
        if (data.size <= maxSizeBytes) return data

        // Пытаемся декодировать и сжать
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        return if (bitmap != null) {
            compressBitmap(bitmap, maxSizeBytes)
        } else {
            data
        }
    }

    /**
     * Пытается привести изображение к JPEG-формату.
     *
     * Если декодирование не удалось, возвращает исходные данные.
     */
    fun convertToJpeg(
        data: ByteArray,
        quality: Int = 100
    ): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return data
        return compressBitmapToJpeg(bitmap, quality).takeIf { it.isNotEmpty() } ?: data
    }

    /**
     * Сжимает bitmap до достижения нужного размера.
     *
     * @param bitmap Bitmap для сжатия
     * @param maxSizeBytes Максимальный размер в байтах
     * @return Сжатый ByteArray
     */
    private fun compressBitmap(
        bitmap: Bitmap,
        maxSizeBytes: Int
    ): ByteArray {
        var quality = INITIAL_QUALITY
        var compressedData = compressBitmapToJpeg(bitmap, quality)

        while (compressedData.size > maxSizeBytes && quality > MIN_QUALITY) {
            quality -= QUALITY_STEP
            compressedData = compressBitmapToJpeg(bitmap, quality)
        }

        // Если все еще слишком большое - уменьшаем разрешение
        if (compressedData.size > maxSizeBytes) {
            val scaledBitmap = scaleBitmap(bitmap, SCALE_FACTOR)
            compressedData = compressBitmapToJpeg(scaledBitmap, FALLBACK_QUALITY)
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
        }

        return compressedData
    }

    /**
     * Сжимает bitmap в JPEG формат.
     */
    private fun compressBitmapToJpeg(
        bitmap: Bitmap,
        quality: Int
    ): ByteArray =
        ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }

    /**
     * Масштабирует bitmap.
     */
    private fun scaleBitmap(
        bitmap: Bitmap,
        scale: Float
    ): Bitmap {
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
