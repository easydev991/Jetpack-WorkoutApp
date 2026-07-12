package com.swparks.data.provider

import android.content.Context
import android.net.Uri
import com.swparks.util.ImageProcessor
import com.swparks.util.UriUtils

/**
 * Реализация AvatarHelper на основе Android Context.
 *
 * @param context Application Context для доступа к ContentResolver
 * @param imageProcessor Процессор изображений для конвертации и сжатия
 */
class AvatarHelperImpl(
    private val context: Context,
    private val imageProcessor: ImageProcessor
) {
    fun isSupportedMimeType(uri: Uri): Boolean = imageProcessor.isSupportedMimeType(context, uri)

    fun uriToByteArray(uri: Uri): Result<ByteArray> = UriUtils.uriToByteArray(context, uri)

    /**
     * Подготавливает изображение: конвертирует в JPEG и сжимает при необходимости.
     *
     * @param uri URI изображения
     * @return Result с ByteArray подготовленного изображения
     */
    fun processImage(uri: Uri): Result<ByteArray> =
        uriToByteArray(uri).map { bytes ->
            val jpeg = imageProcessor.convertToJpeg(bytes)
            imageProcessor.compressIfNeeded(jpeg)
        }

    /**
     * Сжимает изображение, если оно превышает максимальный размер.
     *
     * @param bytes ByteArray с данными изображения
     * @return Сжатый ByteArray или исходный, если размер в норме
     */
    fun compressImage(bytes: ByteArray): ByteArray = imageProcessor.compressIfNeeded(bytes)
}
