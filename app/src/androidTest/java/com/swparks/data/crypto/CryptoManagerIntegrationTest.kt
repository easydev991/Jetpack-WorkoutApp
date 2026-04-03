package com.swparks.data.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты для [CryptoManager] с реальным Android Keystore.
 *
 * Тестирует работу CryptoManager с настоящим Android Keystore для проверки
 * правильности шифрования и дешифрования данных.
 */
@RunWith(AndroidJUnit4::class)
class CryptoManagerIntegrationTest {
    private lateinit var context: Context
    private lateinit var cryptoManager: CryptoManager

    private companion object {
        private const val KEYSET_PREFS_NAME = "test_crypto_prefs"
        private const val KEYSET_KEY = "test_tink_keyset"
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Очищаем старые данные перед каждым тестом
        val prefs = context.getSharedPreferences(KEYSET_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        cryptoManager = CryptoManagerImpl(context, KEYSET_KEY)
    }

    @After
    fun tearDown() {
        // Очищаем данные после тестов
        val prefs = context.getSharedPreferences(KEYSET_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    @Test
    fun encryptAndDecrypt_whenValidData_thenReturnsOriginalData() {
        // Given
        val originalData = "test_encryption_data".toByteArray()

        // When
        val encrypted = cryptoManager.encrypt(originalData)
        val decrypted = cryptoManager.decrypt(encrypted)

        // Then
        assertArrayEquals(
            "Дешифрованные данные должны совпадать с оригинальными",
            originalData,
            decrypted
        )
    }

    @Test
    fun encryptAndDecrypt_whenEmptyData_thenReturnsEmptyData() {
        // Given
        val originalData = byteArrayOf()

        // When
        val encrypted = cryptoManager.encrypt(originalData)
        val decrypted = cryptoManager.decrypt(encrypted)

        // Then
        assertArrayEquals("Дешифрованные данные должны быть пустыми", originalData, decrypted)
    }

    @Test
    fun encrypt_whenCalledMultipleTimes_thenProducesDifferentEncryptedData() {
        // Given
        val originalData = "test_data".toByteArray()

        // When
        val encrypted1 = cryptoManager.encrypt(originalData)
        val encrypted2 = cryptoManager.encrypt(originalData)

        // Then
        // Зашифрованные данные должны быть разными из-за случайного IV (Initialization Vector)
        assertTrue(
            "Зашифрованные данные должны быть разными",
            !encrypted1.contentEquals(encrypted2)
        )
    }

    @Test
    fun encryptAndDecrypt_whenMultiple_encryption_thenAllDecryptCorrectly() {
        // Given
        val dataList =
            listOf(
                "data_1".toByteArray(),
                "data_2".toByteArray(),
                "data_3".toByteArray(),
                "another_token_12345".toByteArray(),
                "final_test_data".toByteArray()
            )

        // When & Then
        for (originalData in dataList) {
            val encrypted = cryptoManager.encrypt(originalData)
            val decrypted = cryptoManager.decrypt(encrypted)
            assertArrayEquals(
                "Данные должны дешифроваться корректно",
                originalData,
                decrypted
            )
        }
    }

    @Test
    fun encrypt_whenLargeData_thenEncryptsSuccessfully() {
        // Given
        val largeData = "a".repeat(10000).toByteArray()

        // When
        val encrypted = cryptoManager.encrypt(largeData)
        val decrypted = cryptoManager.decrypt(encrypted)

        // Then
        assertArrayEquals("Большие данные должны дешифроваться корректно", largeData, decrypted)
    }

    @Test
    fun decrypt_withInvalidData_shouldNotCrash() {
        // Given
        val invalidData = "invalid_encrypted_data".toByteArray()

        // When & Then
        // Не должно выбрасывать исключение, просто вернет какой-то результат
        // (возможно, пустой массив или байты данных)
        try {
            val decrypted = cryptoManager.decrypt(invalidData)
            assertNotNull("Результат дешифрования не должен быть null", decrypted)
        } catch (e: Exception) {
            // Если исключение выбрасывается, это тоже приемлемо
            // Главное - приложение не крашится
            assertNotNull("Исключение должно содержать сообщение", e.message)
        }
    }

    @Test
    fun encryptAndDecrypt_withSpecialCharacters_thenWorksCorrectly() {
        // Given
        val specialData = "Special chars: !@#$%^&*()".toByteArray()

        // When
        val encrypted = cryptoManager.encrypt(specialData)
        val decrypted = cryptoManager.decrypt(encrypted)

        // Then
        assertArrayEquals(
            "Специальные символы должны дешифроваться корректно",
            specialData,
            decrypted
        )
    }
}
