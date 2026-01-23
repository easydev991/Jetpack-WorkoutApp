# Примеры тестирования

Все примеры основаны на реальных тестах из проекта Jetpack-WorkoutApp.

## Unit-тесты ViewModels (с MockK)

**Важно:** Для ViewModels, использующих `viewModelScope.launch`, необходимо добавлять `MainDispatcherRule`. `viewModelScope` работает на `Dispatchers.Main`, который не настроен по умолчанию в Unit-тестах, поэтому корутины не смогут запуститься без этого правила.

### Создание MainDispatcherRule

Создайте файл `MainDispatcherRule.kt` в пакете тестов, если он еще не существует в проекте:

```bash
# Проверьте, существует ли файл:
find app/src/test -name "MainDispatcherRule.kt"

# Если файл не найден, создайте его:
```

```kotlin
package com.swparks.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

### Пример unit-теста ViewModel

```kotlin
package com.swparks.ui.viewmodel

import com.swparks.domain.usecase.ILoginUseCase
import com.swparks.domain.usecase.ILogoutUseCase
import com.swparks.model.LoginSuccess
import com.swparks.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var loginUseCase: ILoginUseCase
    private lateinit var logoutUseCase: ILogoutUseCase
    private lateinit var authViewModel: AuthViewModel

    private val testToken = "test_auth_token_12345"
    private val testLoginSuccess = LoginSuccess(userId = 1L)

    @Before
    fun setup() {
        loginUseCase = mockk(relaxed = true)
        logoutUseCase = mockk(relaxed = true)
        coEvery { loginUseCase(any()) } returns Result.success(testLoginSuccess)
        authViewModel = AuthViewModel(loginUseCase, logoutUseCase)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun login_whenCalled_thenInvokesLoginUseCase() = runTest {
        // When
        authViewModel.login(testToken)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { loginUseCase(testToken) }
    }

    @Test
    fun clearError_whenCalled_thenSetsIdleState() {
        // When
        authViewModel.clearError()

        // Then
        val state = authViewModel.uiState.value
        assertTrue(state is AuthUiState.Idle)
    }
}
```

## Unit-тесты Use Cases

```kotlin
package com.swparks.domain.usecase

import com.swparks.data.SecureTokenRepository
import com.swparks.data.repository.SWRepository
import com.swparks.model.LoginSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {

    private lateinit var secureTokenRepository: SecureTokenRepository
    private lateinit var swRepository: SWRepository
    private lateinit var loginUseCase: LoginUseCase

    private val testToken = "test_auth_token_12345"
    private val testUserId = 12345L

    @Before
    fun setup() {
        secureTokenRepository = mockk(relaxed = true)
        swRepository = mockk(relaxed = true)
        loginUseCase = LoginUseCase(secureTokenRepository, swRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenValidToken_thenSavesTokenAndCallsLogin() = runTest {
        // Given
        coEvery { swRepository.login(any()) } returns Result.success(LoginSuccess(testUserId))

        // When
        val result = loginUseCase(testToken)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUserId, result.getOrNull()?.userId)
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken(testToken) }
        coVerify(exactly = 1) { swRepository.login(null) }
    }

    @Test
    fun invoke_whenEmptyToken_thenSavesEmptyToken() = runTest {
        // Given
        coEvery { swRepository.login(any()) } returns Result.success(LoginSuccess(testUserId))

        // When
        val result = loginUseCase("")

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { secureTokenRepository.saveAuthToken("") }
    }
}
```

## Unit-тесты Repository (с моками API и DataStore)

```kotlin
package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.domain.exception.NetworkException
import com.swparks.model.Park
import com.swparks.network.SWApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class SWRepositoryParksTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getAllParks_whenApiReturnsParks_thenReturnsParks() = runTest {
        // Given
        val mockParksList = listOf(createMockPark(1L), createMockPark(2L))
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getAllParks() } returns mockParksList

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getAllParks()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockParksList, result.getOrNull())
        coVerify { mockApi.getAllParks() }
    }

    @Test
    fun getAllParks_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.getAllParks() } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(mockApi, mockDataStore)

        // When
        val result = repository.getAllParks()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    private fun createMockPark(id: Long = 1L): Park {
        return Park(
            id = id,
            name = "Test Park",
            sizeID = 1,
            typeID = 1,
            longitude = "0.0",
            latitude = "0.0",
            address = "Test Address",
            cityID = 1,
            countryID = 1,
            preview = ""
        )
    }
}
```

## Unit-тесты DataStore Preferences (с Turbine)

```kotlin
package com.swparks.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
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
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val isAuthorizedKey = booleanPreferencesKey("isAuthorized")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun isAuthorized_whenNoValueStored_thenReturnsFalse() = runTest {
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
    fun isAuthorized_whenValueIsTrue_thenReturnsTrue() = runTest {
        // Given
        val mockDataStore = mockk<DataStore<Preferences>>()
        val preferences = mutablePreferencesOf(isAuthorizedKey to true)
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
        every { mockDataStore.data } returns flow { throw IOException("Test error") }
        val repository = UserPreferencesRepository(mockDataStore)

        // When & Then
        repository.isAuthorized.test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setAuthorized_whenValueChanges_thenUpdatesDataStore() = runTest {
        // Given
        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())
        val repository = UserPreferencesRepository(mockDataStore)

        // When
        repository.setAuthorized(true)
        advanceUntilIdle()

        // Then
        coVerify { mockDataStore.edit(any()) }
    }
}
```

## Unit-тесты моделей данных

```kotlin
package com.swparks.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ParkTest {

    @Test
    fun parkModel_whenCreated_thenHasCorrectFields() {
        // Given
        val park = Park(
            id = 1L,
            name = "Test Park",
            sizeID = 1,
            typeID = 2,
            longitude = "55.75",
            latitude = "37.61",
            address = "Test Address",
            cityID = 10,
            countryID = 5,
            preview = "preview_url"
        )

        // Then
        assertEquals(1L, park.id)
        assertEquals("Test Park", park.name)
        assertEquals(1, park.sizeID)
        assertEquals(2, park.typeID)
        assertEquals("55.75", park.longitude)
        assertEquals("37.61", park.latitude)
        assertNotNull(park.address)
    }

    @Test
    fun parkModel_whenDefaultValues_thenValid() {
        // When
        val park = Park(
            id = 0L,
            name = "",
            sizeID = 0,
            typeID = 0,
            longitude = "0.0",
            latitude = "0.0",
            address = "",
            cityID = 0,
            countryID = 0,
            preview = ""
        )

        // Then
        assertEquals(0L, park.id)
        assertEquals("", park.name)
        assertEquals(0.0, park.longitude.toDouble())
        assertEquals(0.0, park.latitude.toDouble())
    }
}
```

## Интеграционные тесты ViewModels (androidTest/)

**Важно:** Рекомендуется использовать unit-тесты с MockK, но интеграционные тесты возможны для проверки взаимодействия с реальным Repository/БД.

Примеры из проекта JetpackDays с работающими интеграционными тестами ViewModels:

```kotlin
package com.dayscounter.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.repository.ItemRepositoryImpl
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.test.MainDispatcherRule
import com.dayscounter.util.NoOpLogger
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты для DetailScreenViewModel.
 * Тестируют взаимодействие ViewModel с реальным Repository и базой данных.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailScreenViewModelIntegrationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: DaysDatabase
    private lateinit var repository: ItemRepositoryImpl
    private lateinit var viewModel: DetailScreenViewModel
    private lateinit var context: Context

    private val testItem = Item(
        id = 0L,
        title = "Тестовое событие",
        details = "Описание события",
        timestamp = System.currentTimeMillis(),
        colorTag = null,
        displayOption = DisplayOption.DAY,
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Создаем новую in-memory базу для каждого теста
        database = Room
            .inMemoryDatabaseBuilder(
                context,
                DaysDatabase::class.java,
            ).allowMainThreadQueries()
            .build()

        repository = ItemRepositoryImpl(database.itemDao())

        // Очищаем базу данных перед каждым тестом
        database.clearAllTables()
    }

    @After
    fun tearDown() {
        // Закрываем базу данных после каждого теста
        database.close()
    }

    @Test
    fun whenItemExistsInDatabase_thenLoadsSuccessfully() {
        runTest {
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // Тестируем эмиссии StateFlow с помощью Turbine
            viewModel.uiState.test {
                // Проверяем начальное состояние Loading
                val loadingState = awaitItem()
                assertTrue(
                    "Начальное состояние должно быть Loading",
                    loadingState is DetailScreenState.Loading,
                )

                // Проверяем состояние Success
                val successState = awaitItem()
                assertTrue(
                    "Состояние должно быть Success",
                    successState is DetailScreenState.Success
                )
                val success = successState as DetailScreenState.Success
                assertEquals("Тестовое событие", success.item.title)
                assertEquals("Описание события", success.item.details)
                assertEquals(insertedId, success.item.id)
            }
        }
    }

    @Test
    fun whenRequestDelete_thenShowsDeleteDialog() {
        runTest {
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)
            
            viewModel.requestDelete()
            assertTrue(
                "Диалог удаления должен быть показан",
                viewModel.showDeleteDialog.value,
            )
        }
    }
}
```

**Ключевые моменты для интеграционных тестов ViewModels:**

1. **Используйте `runTest`** вместо `runBlocking`
2. **Используйте `MainDispatcherRule`** для замены `Dispatchers.Main`
3. **Используйте `Turbine`** для тестирования StateFlow эмиссий
4. **Используйте `advanceUntilIdle()`** для ожидания завершения корутин
5. **Создавайте in-memory базу данных** для каждого теста

**Примечание:** В Jetpack-WorkoutApp пока не используются интеграционные тесты ViewModels, предпочитаются unit-тесты с MockK.

## Интеграционные тесты для компонентов с Android API (androidTest/)

Интеграционные тесты используются для тестирования компонентов, которые требуют реальных Android API (например, Android Keystore для шифрования).

```kotlin
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
    fun encryptAndDecrypt_whenMultipleEncryptions_thenAllDecryptCorrectly() {
        // Given
        val dataList = listOf(
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
```

**Важно:** Интеграционные тесты в Jetpack-WorkoutApp предназначены только для компонентов, которые требуют реальных Android API. ViewModels и Repository тестируются через unit-тесты с моками.

## Настройка тестов с ExperimentalCoroutinesApi

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SomeTestClass {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // Устанавливаем тестовый диспетчер корутин
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // Очищаем моки после каждого теста
        unmockkAll()
    }
}
```

## Мокирование Android Log

```kotlin
import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic

@Before
fun setup() {
    mockkStatic(Log::class)
    every { Log.e(any(), any(), any()) } returns 0
}

@After
fun tearDown() {
    unmockkAll()
}
```

## Общие паттерны для всех тестов

### Настройка (setup)

```kotlin
@Before
fun setup() {
    // Создаем моки с relaxed = true для игнорирования необязательных вызовов
    loginUseCase = mockk(relaxed = true)
    swRepository = mockk(relaxed = true)

    // Устанавливаем тестовый диспетчер корутин
    Dispatchers.setMain(StandardTestDispatcher())

    // Мокируем Android Log для избежания вывода в консоль
    mockkStatic(Log::class)
    every { Log.e(any(), any(), any()) } returns 0
}
```

### Очистка (tearDown)

```kotlin
@After
fun tearDown() {
    // Обязательно очищаем все моки после каждого теста
    unmockkAll()
}
```

### Использование runTest

```kotlin
@Test
fun someTest() = runTest {
    // Тестовый код с корутинами
    val result = repository.getData()

    // Проверки
    assertTrue(result.isSuccess)
}
```

### Использование Turbine для Flow

```kotlin
@Test
fun flowTest() = runTest {
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
```

### MockK паттерны

```kotlin
// Создание мока с relaxed режимом
val mockRepository = mockk<ItemRepository>(relaxed = true)

// Настройка поведения мока
coEvery { mockRepository.getData() } returns listOf(item)
every { mockDataStore.data } returns flowOf(emptyPreferences())

// Проверка вызовов
coVerify(exactly = 1) { mockRepository.getData() }
coVerify(atLeast = 1) { mockRepository.saveData(any()) }
```

### Result паттерны

```kotlin
// Проверка успешного результата
val result = repository.getData()
assertTrue(result.isSuccess)
assertEquals(expectedData, result.getOrNull())

// Проверка неудачного результата
assertTrue(result.isFailure)
assertTrue(result.exceptionOrNull() is NetworkException)
```
