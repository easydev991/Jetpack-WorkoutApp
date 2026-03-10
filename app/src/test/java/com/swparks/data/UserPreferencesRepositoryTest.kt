package com.swparks.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private val currentUserIdKey = longPreferencesKey("currentUserId")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
        // resetMain не доступен в kotlinx-coroutines-test, но это не критично для unit-тестов
    }

    @Test
    fun isAuthorized_whenNoUserIdStored_thenReturnsFalse() = runTest {
        // Given
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())
        val repository = UserPreferencesRepository(mockDataStore)

        // When & Then
        repository.isAuthorized.test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isAuthorized_whenUserIdExists_thenReturnsTrue() = runTest {
        // Given
        val mockDataStore = mockk<DataStore<Preferences>>()
        val preferences = mutablePreferencesOf(currentUserIdKey to 123L)
        every { mockDataStore.data } returns flowOf(preferences)
        val repository = UserPreferencesRepository(mockDataStore)

        // When & Then
        repository.isAuthorized.test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isAuthorized_whenIOExceptionOccurs_thenEmitsEmptyPreferencesAndReturnsFalse() = runTest {
        // Given
        val mockDataStore = mockk<DataStore<Preferences>>()
        // Создаем Flow, который выбрасывает IOException при подписке
        every { mockDataStore.data } returns flow { throw IOException("Test error") }
        val repository = UserPreferencesRepository(mockDataStore)

        // When & Then
        // После IOException catch блок должен эмитить emptyPreferences, а map преобразует в false
        val result = repository.isAuthorized.first()
        assertEquals(false, result)
    }

    @Test
    fun isAuthorized_whenOtherExceptionOccurs_thenThrowsException() = runTest {
        // Given
        val mockDataStore = mockk<DataStore<Preferences>>()
        val testException = RuntimeException("Test error")
        // Создаем Flow, который выбрасывает RuntimeException при подписке через onStart
        every { mockDataStore.data } returns
            flowOf<Preferences>(emptyPreferences()).onStart { throw testException }
        val repository = UserPreferencesRepository(mockDataStore)

        // When & Then - исключение должно быть проброшено дальше
        var exceptionThrown = false
        try {
            repository.isAuthorized.collect {
                // Не должно дойти до этого места
            }
        } catch (e: RuntimeException) {
            exceptionThrown = true
            assertEquals("Test error", e.message)
        }
        assertTrue("Expected RuntimeException was not thrown", exceptionThrown)
    }
}
