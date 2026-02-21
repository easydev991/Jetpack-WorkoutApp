package com.swparks.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.IOException

/**
 * Утилиты для работы с Uri.
 */
object UriUtils {
    private const val TAG = "UriUtils"

    /**
     * Конвертирует Uri в ByteArray с обработкой ошибок.
     *
     * @param context Контекст приложения для доступа к ContentResolver
     * @param uri Uri для конвертации
     * @return Result.success(ByteArray) или Result.failure с описанием ошибки
     */
    fun uriToByteArray(context: Context, uri: Uri): Result<ByteArray> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                Result.success(inputStream.readBytes())
            } ?: Result.failure(IOException("Cannot open input stream for uri: $uri"))
        } catch (e: SecurityException) {
            Log.w(TAG, "Ошибка доступа при чтении uri: $uri", e)
            Result.failure(SecurityException("No permission to read uri: $uri"))
        } catch (e: IOException) {
            Log.w(TAG, "Ошибка ввода-вывода при чтении uri: $uri", e)
            Result.failure(e)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "Неожиданная ошибка при чтении uri: $uri", e)
            Result.failure(IOException("Unexpected error reading uri: ${e.message}"))
        }
    }
}
