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
 * Сериализатор для десериализации Int из строк или чисел.
 *
 * Используется для полей, которые сервер может присылать как строки ("123") или как числа (123).
 *
 * Пример использования:
 * ```kotlin
 * @Serializable
 * data class Park(
 *     @Serializable(with = IntStringSerializer::class)
 *     val id: Long,
 *     @Serializable(with = IntStringSerializer::class)
 *     val sizeID: Int
 * )
 * ```
 */
object IntStringSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntString", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("IntStringSerializer поддерживает только JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                // Пробуем конвертировать в Int из строки или числа
                element.content.toIntOrNull()
                    ?: throw SerializationException(
                        "Не удалось конвертировать значение в Int: ${element.content}"
                    )
            }

            else -> throw SerializationException(
                "Ожидали JsonPrimitive для IntString, но получили: ${element::class.simpleName}"
            )
        }
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }
}
