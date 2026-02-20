package com.swparks.data.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Гибкий десериализатор дат для kotlinx.serialization
 *
 * Поддерживает следующие форматы дат:
 * - `2024-01-15T10:30:00Z` (стандартный ISO8601)
 * - `2024-01-15T10:30:00.123Z` (с дробными секундами)
 * - `2024-01-15T10:30:00+00:00` (с часовым поясом +HH:MM)
 * - `2024-01-15T10:30:00-05:00` (с часовым поясом -HH:MM)
 * - `2024-01-15T10:30:00` (server date time без часового пояса)
 * - `2024-01-15` (ISO short date)
 *
 * @see FlexibleDateSerializer для сериализации
 */
object FlexibleDateDeserializer : KSerializer<String> {

    private const val MONTH_MIN = 1
    private const val MONTH_MAX = 12
    private const val DAY_MIN = 1
    private const val DAY_MAX = 31
    private const val HOUR_MIN = 0
    private const val HOUR_MAX = 23
    private const val MINUTE_MIN = 0
    private const val MINUTE_MAX = 59
    private const val SECOND_MIN = 0
    private const val SECOND_MAX = 59

    // Индексы групп в результате regex
    @Suppress("UnusedPrivateProperty")
    private const val GROUP_INDEX_YEAR = 1
    private const val GROUP_INDEX_MONTH = 2
    private const val GROUP_INDEX_DAY = 3
    private const val GROUP_INDEX_HOUR = 4
    private const val GROUP_INDEX_MINUTE = 5
    private const val GROUP_INDEX_SECOND = 6

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleDate", PrimitiveKind.STRING)

    /**
     * Десериализует строку даты из JSON
     *
     * Пробует последовательно все поддерживаемые форматы дат
     * и возвращает первое успешное совпадение.
     *
     * @param decoder Декодер kotlinx.serialization
     * @return Строка даты в исходном формате
     * @throws SerializationException Если дату невозможно распарсить ни одним из форматов
     */
    override fun deserialize(decoder: Decoder): String {
        val dateString = decoder.decodeString()

        // Проверяем, что строка не пустая (для опциональных полей)
        if (dateString.isBlank()) {
            return dateString
        }

        // Проверяем соответствие формату перед парсингом
        validateDateFormat(dateString)

        // Проверка диапазона значений для даты и времени
        validateDateRange(dateString)

        // Пробуем каждый формат по очереди
        val parsedDate = parseDate(dateString)

        return parsedDate ?: throw SerializationException(
            "Невозможно распарсить дату: '$dateString'. " +
                "Ожидается один из форматов: " +
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z', yyyy-MM-dd'T'HH:mm:ss'Z', " +
                "yyyy-MM-dd'T'HH:mm:ssXXX, yyyy-MM-dd'T'HH:mm:ss.SSSXXX, " +
                "yyyy-MM-dd'T'HH:mm:ss, yyyy-MM-dd"
        )
    }

    /**
     * Проверяет соответствие формату даты
     */
    private fun validateDateFormat(dateString: String): Boolean {
        val isValid =
            when {
                // Формат 1: ISO8601 с дробными секундами (1-6 цифр)
                dateString.matches(
                    Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{1,6}Z$""")
                ) -> true

                // Формат 2: ISO8601 без дробных секунд
                dateString.matches(Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$""")) -> true

                // Формат 3: ISO8601 с часовым поясом (+HH:MM или -HH:MM)
                dateString.matches(Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}$""")) -> true

                // Формат 4: ISO8601 с дробными секундами и часовым поясом
                dateString.matches(Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{1,6}[+-]\d{2}:\d{2}$""")) -> true

                // Формат 5: Server date time (без часового пояса)
                dateString.matches(Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$""")) -> true

                // Формат 6: ISO short date
                dateString.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) -> true

                else -> false
            }

        if (!isValid) {
            throw SerializationException(
                "Невозможно распарсить дату: '$dateString'. " +
                    "Ожидается один из форматов: " +
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z', yyyy-MM-dd'T'HH:mm:ss'Z', " +
                    "yyyy-MM-dd'T'HH:mm:ssXXX, yyyy-MM-dd'T'HH:mm:ss.SSSXXX, " +
                    "yyyy-MM-dd'T'HH:mm:ss, yyyy-MM-dd"
            )
        }

        return true
    }

    /**
     * Проверяет диапазон значений даты и времени
     */
    private fun validateDateRange(dateString: String) {
        // Упрощенный regex для извлечения компонентов даты/времени
        val dateMatch =
            Regex("""^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}):(\d{2})(?:\.\d{1,6})?(?:Z|[+-]\d{2}:\d{2})?)?$""")
                .find(dateString)

        if (dateMatch != null) {
            val groups = dateMatch.groupValues
            val monthInt = groups[GROUP_INDEX_MONTH].toInt()
            val dayInt = groups[GROUP_INDEX_DAY].toInt()
            val hourInt = groups.getOrNull(GROUP_INDEX_HOUR)?.toIntOrNull() ?: HOUR_MIN
            val minuteInt = groups.getOrNull(GROUP_INDEX_MINUTE)?.toIntOrNull() ?: MINUTE_MIN
            val secondInt = groups.getOrNull(GROUP_INDEX_SECOND)?.toIntOrNull() ?: SECOND_MIN


            validateMonthRange(monthInt)
            validateDayRange(dayInt)
            validateHourRange(hourInt)
            validateMinuteRange(minuteInt)
            validateSecondRange(secondInt)
        }
    }

    private fun validateMonthRange(month: Int) {
        if (month < MONTH_MIN || month > MONTH_MAX) {
            throw SerializationException("Месяц должен быть в диапазоне 1-12")
        }
    }

    private fun validateDayRange(day: Int) {
        if (day < DAY_MIN || day > DAY_MAX) {
            throw SerializationException("День должен быть в диапазоне 1-31")
        }
    }

    private fun validateHourRange(hour: Int) {
        if (hour < HOUR_MIN || hour > HOUR_MAX) {
            throw SerializationException("Часы должны быть в диапазоне 0-23")
        }
    }

    private fun validateMinuteRange(minute: Int) {
        if (minute < MINUTE_MIN || minute > MINUTE_MAX) {
            throw SerializationException("Минуты должны быть в диапазоне 0-59")
        }
    }

    private fun validateSecondRange(second: Int) {
        if (second < SECOND_MIN || second > SECOND_MAX) {
            throw SerializationException("Секунды должны быть в диапазоне 0-59")
        }
    }

    /**
     * Пытается распарсить дату, используя все поддерживаемые форматы
     */
    private fun parseDate(dateString: String): String? {
        val supportedFormats =
            listOf(
                // Формат 1: ISO8601 с дробными секундами
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                // Формат 2: ISO8601 без дробных секунд
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                // Формат 3: ISO8601 с часовым поясом (+HH:MM или -HH:MM)
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                // Формат 4: ISO8601 с дробными секундами и часовым поясом
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                // Формат 5: Server date time (без часового пояса)
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                },
                // Формат 6: ISO short date
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
            )

        for (format in supportedFormats) {
            try {
                format.isLenient = false
                val parsedDate = format.parse(dateString)
                // Если парсинг прошел успешно, возвращаем строку
                if (parsedDate != null) {
                    return dateString
                }
            } catch (_: Exception) {
                // Игнорируем исключение и пробуем следующий формат
            }
        }

        return null
    }

    /**
     * Сериализует строку даты в JSON
     *
     * @param encoder Энкодер kotlinx.serialization
     * @param value Строка даты
     */
    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}
