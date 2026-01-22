package com.swparks.data.crypto

/**
 * Интерфейс для менеджера шифрования/дешифрования данных.
 *
 * Использует Tink для AES шифрования и Android Keystore для хранения ключей.
 */
interface CryptoManager {
    /**
     * Шифрует данные и возвращает зашифрованный массив байтов.
     *
     * @param data Данные для шифрования
     * @return Зашифрованные данные
     */
    fun encrypt(data: ByteArray): ByteArray

    /**
     * Дешифрует данные и возвращает исходный массив байтов.
     *
     * @param ciphertext Зашифрованные данные
     * @return Расшифрованные данные
     */
    fun decrypt(ciphertext: ByteArray): ByteArray
}
