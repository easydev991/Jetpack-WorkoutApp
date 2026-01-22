package com.swparks.data

import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.swparks.data.crypto.CryptoManager
import com.swparks.data.serializer.EncryptedStringSerializer
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Тесты для [SecureTokenRepository]
 * - безопасного хранения токена авторизации.
 */
class SecureTokenRepositoryTest {

    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var mockCryptoManager: CryptoManager
    private lateinit var serializer: EncryptedStringSerializer

    private val encryptedTokenKey = stringPreferencesKey("encrypted_token")

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers
                {
                    java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
                }
        every { Base64.decode(any<String>(), any()) } answers
                {
                    java.util.Base64.getDecoder().decode(firstArg<String>())
                }

        mockDataStore = mockk(relaxed = true)
        mockCryptoManager = mockk()

        // Настраиваем дефолтный ответ для dataStore.data
        every { mockDataStore.data } returns flowOf(preferencesOf())

        serializer = EncryptedStringSerializer(mockCryptoManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun saveAuthToken_whenValidToken_thenSavesEncryptedToken() = runTest {
        // Given
        val repository = SecureTokenRepository(mockDataStore, serializer)
        val token = "my_auth_token_67890"
        val encryptedBytes = "encrypted_data".toByteArray()

        every { mockCryptoManager.encrypt(token.toByteArray()) } returns encryptedBytes

        // When
        repository.saveAuthToken(token)

        // Then
        coVerify(exactly = 1) { mockDataStore.updateData(any()) }
    }

    @Test
    fun saveAuthToken_whenNullToken_thenRemovesToken() = runTest {
        // Given
        val repository = SecureTokenRepository(mockDataStore, serializer)
        val encryptedBytes = byteArrayOf()
        every { mockCryptoManager.encrypt(any()) } returns encryptedBytes

        // When
        repository.saveAuthToken(null)

        // Then
        coVerify(exactly = 1) { mockDataStore.updateData(any()) }
    }

    @Test
    fun getAuthTokenSync_whenTokenExists_thenReturnsDecryptedToken() {
        // Given
        val repository = SecureTokenRepository(mockDataStore, serializer)
        val encryptedToken = "encrypted_data".toByteArray()
        val encryptedTokenBase64 = java.util.Base64.getEncoder().encodeToString(encryptedToken)
        val plainToken = "decrypted_token"

        val preferences = preferencesOf(encryptedTokenKey to encryptedTokenBase64)
        every { mockDataStore.data } returns flowOf(preferences)
        every { mockCryptoManager.decrypt(encryptedToken) } returns plainToken.toByteArray()

        // When
        val result = repository.getAuthTokenSync()

        // Then
        assertEquals(plainToken, result)
    }

    @Test
    fun getAuthTokenSync_whenNoToken_thenReturnsNull() {
        // Given
        val repository = SecureTokenRepository(mockDataStore, serializer)
        val emptyPreferences = preferencesOf()
        every { mockDataStore.data } returns flowOf(emptyPreferences)

        // When
        val result = repository.getAuthTokenSync()

        // Then
        assertNull(result)
    }

    @Test
    fun clearAuthTokenSync_thenRemovesToken() {
        // Given
        val repository = SecureTokenRepository(mockDataStore, serializer)

        // When
        repository.clearAuthTokenSync()

        // Then
        coVerify(exactly = 1) { mockDataStore.updateData(any()) }
    }

    @Test
    fun authTokenFlow_whenTokenExists_thenEmitsDecryptedToken() = runTest {
        // Given
        val encryptedToken = "encrypted_data".toByteArray()
        val encryptedTokenBase64 = java.util.Base64.getEncoder().encodeToString(encryptedToken)
        val plainToken = "decrypted_token"

        val preferences = preferencesOf(encryptedTokenKey to encryptedTokenBase64)

        // Настраиваем мок ПЕРЕД созданием репозитория
        every { mockDataStore.data } returns flowOf(preferences)
        every { mockCryptoManager.decrypt(encryptedToken) } returns plainToken.toByteArray()

        // Создаем репозиторий ПОСЛЕ настройки мока
        val repository = SecureTokenRepository(mockDataStore, serializer)

        // When
        val result = repository.authToken.first()

        // Then
        assertEquals(plainToken, result)
    }

    @Test
    fun authTokenFlow_whenNoToken_thenEmitsNull() = runTest {
        // Given
        val emptyPreferences = preferencesOf()

        // Настраиваем мок ПЕРЕД созданием репозитория
        every { mockDataStore.data } returns flowOf(emptyPreferences)

        // Создаем репозиторий ПОСЛЕ настройки мока
        val repository = SecureTokenRepository(mockDataStore, serializer)

        // When
        val result = repository.authToken.first()

        // Then
        assertNull(result)
    }
}
