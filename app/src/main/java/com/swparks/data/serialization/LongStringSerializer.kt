package com.swparks.data.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * Сериализатор для десериализации Long из строк или чисел.
 *
 * Используется для полей, которые сервер может присылать как строки ("123") или как числа (123).
 *
 * Пример использования:
 * ```kotlin
 * @Serializable
 * data class Park(
 *     @Serializable(with = LongStringSerializer::class)
 *     val id: Long
 * )
 * ```
 */
object LongStringSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LongString", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("LongStringSerializer поддерживает только JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                // Пробуем конвертировать в Long из строки или числа
                element.content.toLongOrNull()
                    ?: throw SerializationException(
                        "Не удалось конвертировать значение в Long: ${element.content}"
                    )
            }

            else -> throw SerializationException(
                "Ожидали JsonPrimitive для LongString, но получили: ${element::class.simpleName}"
            )
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Long
    ) {
        encoder.encodeLong(value)
    }
}
