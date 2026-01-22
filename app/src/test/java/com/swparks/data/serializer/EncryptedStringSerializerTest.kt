package com.swparks.data.serializer

import android.util.Log
import com.swparks.data.crypto.CryptoManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Тесты для [EncryptedStringSerializer] - шифрования/дешифрования строк для Preferences DataStore
 */
class EncryptedStringSerializerTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun deserialize_whenEncryptedData_thenReturnsDecryptedString() {
        // Given
        val mockCryptoManager = mockk<CryptoManager>()
        val plainText = "test_token_12345"
        val encryptedData = "encrypted_data_base64".toByteArray()

        every { mockCryptoManager.decrypt(encryptedData) } returns plainText.toByteArray()

        val serializer = EncryptedStringSerializer(mockCryptoManager)

        // When
        val result = serializer.deserialize(encryptedData)

        // Then
        assertEquals(plainText, result)
    }

    @Test
    fun deserialize_whenNullData_thenReturnsNull() {
        // Given
        val mockCryptoManager = mockk<CryptoManager>()
        val serializer = EncryptedStringSerializer(mockCryptoManager)

        // When
        val result = serializer.deserialize(null)

        // Then
        assertNull(result)
    }

    @Test
    fun serialize_whenValidString_thenReturnsEncryptedByteArray() {
        // Given
        val mockCryptoManager = mockk<CryptoManager>()
        val plainText = "test_token_12345"
        val plainTextBytes = plainText.toByteArray()
        val encryptedData = "encrypted_data_base64".toByteArray()

        every { mockCryptoManager.encrypt(plainTextBytes) } returns encryptedData

        val serializer = EncryptedStringSerializer(mockCryptoManager)

        // When
        val result = serializer.serialize(plainText)

        // Then
        assertEquals(encryptedData, result)
    }

    @Test
    fun serialize_whenNullString_thenReturnsEmptyByteArray() {
        // Given
        val mockCryptoManager = mockk<CryptoManager>()
        val serializer = EncryptedStringSerializer(mockCryptoManager)

        // When
        val result = serializer.serialize(null)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun serializeAndDeserialize_whenValidToken_thenReturnsOriginalToken() {
        // Given
        val mockCryptoManager = mockk<CryptoManager>()
        val plainText = "test_token_67890"
        val plainTextBytes = plainText.toByteArray()
        val encryptedData = "encrypted_token_base64".toByteArray()

        every { mockCryptoManager.encrypt(plainTextBytes) } returns encryptedData
        every { mockCryptoManager.decrypt(encryptedData) } returns plainText.toByteArray()

        val serializer = EncryptedStringSerializer(mockCryptoManager)

        // When
        val encrypted = serializer.serialize(plainText)
        val decrypted = serializer.deserialize(encrypted)

        // Then
        assertEquals(plainText, decrypted)
    }

    @Test
    fun serializeAndDeserialize_whenNullToken_thenReturnsNull() {
        // Given
        val mockCryptoManager = mockk<CryptoManager>()
        val serializer = EncryptedStringSerializer(mockCryptoManager)

        // When
        val encrypted = serializer.serialize(null)
        val decrypted = serializer.deserialize(encrypted)

        // Then
        assertNull(decrypted)
    }
}
