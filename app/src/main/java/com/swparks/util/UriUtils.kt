package com.swparks.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

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

fun readJSONFromAssets(
    context: Context,
    path: String
): String {
    val identifier = "[readJSONFromAssetsreadJSONFromAssets]"
    try {
        val file = context.assets.open(path)
        Log.i(
            identifier,
            "Нашли файл: $file.",
        )
        val bufferedReader = BufferedReader(InputStreamReader(file))
        val stringBuilder = StringBuilder()
        bufferedReader.useLines { lines ->
            lines.forEach {
                stringBuilder.append(it)
            }
        }
        val jsonString = stringBuilder.toString()
        Log.i(
            identifier,
            "Успешно прочитали JSON из ассетов по адресу: $path",
        )
        return jsonString
    } catch (e: IOException) {
        Log.e(
            identifier,
            "Не смогли прочитать JSON, ошибка: $e.",
        )
        return ""
    }
}
