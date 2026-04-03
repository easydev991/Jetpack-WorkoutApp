package com.swparks.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Утилиты для работы с сетью и multipart-запросами
 */
object NetworkUtils {
    /**
     * Создает RequestBody из строки
     */
    fun createStringPart(value: String): RequestBody =
        value.toRequestBody("text/plain".toMediaType())

    /**
     * Создает опциональный RequestBody из строки
     */
    fun createOptionalStringPart(value: String?): RequestBody? = value?.let { createStringPart(it) }

    /**
     * Создает часть multipart-запроса с именем поля
     * Примечание: имя поля не используется в текущей реализации
     */
    @Suppress("UnusedParameter")
    fun createPartWithName(
        name: String,
        value: String
    ): RequestBody = value.toRequestBody("text/plain".toMediaType())

    /**
     * Создает опциональную часть multipart-запроса с именем поля
     */
    @Suppress("UnusedParameter")
    fun createOptionalPartWithName(
        name: String,
        value: String?
    ): RequestBody? = value?.let { createPartWithName(name, it) }

    fun createImagePart(
        data: ByteArray,
        name: String,
        filename: String = "$name.jpg",
        mimeType: String = "image/jpeg"
    ): MultipartBody.Part {
        val requestFile = data.toRequestBody(mimeType.toMediaType())
        return MultipartBody.Part.createFormData(name, filename, requestFile)
    }

    fun createOptionalImagePart(
        data: ByteArray?,
        name: String,
        filename: String = "$name.jpg",
        mimeType: String = "image/jpeg"
    ): MultipartBody.Part? = data?.let { createImagePart(it, name, filename, mimeType) }
}
