package com.swparks.data.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
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
    private const val SUPPORTED_FORMATS_MESSAGE =
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z', yyyy-MM-dd'T'HH:mm:ss'Z', " +
            "yyyy-MM-dd'T'HH:mm:ssXXX, yyyy-MM-dd'T'HH:mm:ss.SSSXXX, " +
            "yyyy-MM-dd'T'HH:mm:ss, yyyy-MM-dd"

    private val offsetFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME

    private val serverDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ss", Locale.US)
            .withResolverStyle(ResolverStyle.STRICT)

    private val localDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_LOCAL_DATE

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

        if (dateString.isBlank()) {
            return dateString
        }

        if (tryParse { OffsetDateTime.parse(dateString, offsetFormatter) } ||
            tryParse { LocalDateTime.parse(dateString, serverDateTimeFormatter) } ||
            tryParse { LocalDate.parse(dateString, localDateFormatter) }
        ) {
            return dateString
        }

        throw SerializationException(
            "Невозможно распарсить дату: '$dateString'. " +
                "Ожидается один из форматов: $SUPPORTED_FORMATS_MESSAGE"
        )
    }

    private inline fun tryParse(parse: () -> Any?): Boolean =
        try {
            parse()
            true
        } catch (_: DateTimeException) {
            false
        }

    /**
     * Сериализует строку даты в JSON
     *
     * @param encoder Энкодер kotlinx.serialization
     * @param value Строка даты
     */
    override fun serialize(
        encoder: Encoder,
        value: String
    ) {
        encoder.encodeString(value)
    }
}
