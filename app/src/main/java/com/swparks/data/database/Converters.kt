package com.swparks.data.database

import android.util.Log
import androidx.room.TypeConverter
import com.swparks.data.model.Park
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * TypeConverters для сложных типов в Room Database
 *
 * Обеспечивает сериализацию/десериализацию объектов в JSON для хранения в базе данных.
 */
object UserConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    /**
     * Сериализовать список площадок в JSON-строку
     *
     * @param parks Список площадок для сериализации
     * @return JSON-строка или null если parks = null
     */
    @TypeConverter
    fun fromParksList(parks: List<Park>?): String? {
        return parks?.let { json.encodeToString(it) }
    }

    /**
     * Десериализовать JSON-строку в список площадок
     *
     * @param data JSON-строка с данными о площадках
     * @return Список площадок или null если data = null или произошла ошибка парсинга
     */
    @TypeConverter
    fun toParksList(data: String?): List<Park>? {
        return data?.let {
            try {
                json.decodeFromString<List<Park>>(it)
            } catch (e: SerializationException) {
                Log.e("UserConverters", "Ошибка десериализации площадок: ${e.message}")
                null
            }
        }
    }
}
