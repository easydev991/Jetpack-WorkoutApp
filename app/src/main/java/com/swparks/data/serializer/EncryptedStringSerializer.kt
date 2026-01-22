package com.swparks.data.serializer

import android.util.Log
import com.swparks.data.crypto.CryptoManager

/**
 * Вспомогательный класс для шифрования/дешифрования строк с помощью Tink.
 *
 * Используется в [SecureTokenRepository] для безопасного хранения токена авторизации
 * в Preferences DataStore. Шифрует строки перед сохранением и расшифровывает при чтении.
 *
 * Пример использования:
 * ```kotlin
 * // Сохранение токена
 * val encryptedToken = EncryptedStringSerializer(cryptoManager).serialize(token)
 * dataStore.edit { it[tokenKey] = encryptedToken }
 *
 * // Чтение токена
 * val encryptedToken = dataStore.data.map { it[tokenKey] }
 * val token = EncryptedStringSerializer(cryptoManager).deserialize(encryptedToken)
 * ```
 */
class EncryptedStringSerializer(
    private val cryptoManager: CryptoManager
) {

    private companion object {
        private const val tag = "EncryptedStringSerializer"
    }

    /**
     * Дешифрует зашифрованные данные из DataStore.
     *
     * @param encryptedData Зашифрованные данные в формате Base64
     * @return Расшифрованная строка или null если данных нет
     */
    fun deserialize(encryptedData: ByteArray?): String? {
        return try {
            if (encryptedData == null || encryptedData.isEmpty()) {
                Log.d(tag, "Входные данные пустые, возвращаем null")
                return null
            }

            // Расшифровываем данные
            val decryptedData = cryptoManager.decrypt(encryptedData)

            // Конвертируем в строку
            String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(tag, "Ошибка дешифрования данных", e)
            null
        }
    }

    /**
     * Шифрует строку в формат для DataStore.
     *
     * @param value Строка для шифрования
     * @return Зашифрованные данные
     */
    fun serialize(value: String?): ByteArray {
        return try {
            if (value.isNullOrEmpty()) {
                Log.d(tag, "Входное значение пустое, возвращаем пустой массив байтов")
                return byteArrayOf()
            }

            // Конвертируем строку в массив байтов
            val data = value.toByteArray(Charsets.UTF_8)

            // Шифруем данные
            cryptoManager.encrypt(data)
        } catch (e: Exception) {
            Log.e(tag, "Ошибка шифрования данных", e)
            byteArrayOf()
        }
    }
}
