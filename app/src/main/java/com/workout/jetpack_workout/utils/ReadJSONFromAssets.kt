package com.workout.jetpack_workout.utils

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

fun ReadJSONFromAssets(
    context: Context,
    path: String
): String {
    val identifier = "[ReadJSONFromAssets]"
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
    } catch (e: Exception) {
        Log.e(
            identifier,
            "Не смогли прочитать JSON, ошибка: $e.",
        )
        e.printStackTrace()
        return ""
    }
}