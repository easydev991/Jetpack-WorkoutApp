package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.EventDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.ParkDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.model.LoginSuccess
import com.swparks.data.model.User
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.exception.ServerException
import com.swparks.domain.model.RegistrationParams
import com.swparks.network.SWApi
import com.swparks.util.NoOpCrashReporter
import com.swparks.util.NoOpLogger
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Unit тесты для методов авторизации в SWRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SWRepositoryAuthTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockUserDao = mockk<UserDao>(relaxed = true)
    private val mockJournalDao = mockk<JournalDao>(relaxed = true)
    private val mockJournalEntryDao = mockk<JournalEntryDao>(relaxed = true)
    private val mockDialogDao = mockk<DialogDao>(relaxed = true)
    private val mockEventDao = mockk<EventDao>(relaxed = true)
    private val mockParkDao = mockk<ParkDao>(relaxed = true)
    private val crashReporter = NoOpCrashReporter()
    private val logger = NoOpLogger()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun login_whenApiReturnsSuccess_thenReturnsSuccessAndSavesToken() = runTest {
        // Given
        val mockSuccess = LoginSuccess(userId = 123L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.login() } returns mockSuccess

        val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.login("test_token")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockSuccess, result.getOrNull())
        coVerify { mockApi.login() }
    }

    @Test
    fun login_whenApiThrowsIOException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.login() } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.login("test_token")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun login_whenTokenIsNull_thenSavesPreference() = runTest {
        // Given
        val mockSuccess = LoginSuccess(userId = 123L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.login() } returns mockSuccess

        val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.login(null)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.login() }
    }

    @Test
    fun resetPassword_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockSuccess = LoginSuccess(userId = 123L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.resetPassword(any()) } returns mockSuccess

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.resetPassword("test_login")

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.resetPassword(any()) }
    }

    @Test
    fun resetPassword_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.resetPassword(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.resetPassword("test_login")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun changePassword_whenApiReturnsSuccess_thenReturnsSuccess() = runTest {
        // Given
        val mockResponse = mockk<Response<Unit>>(relaxed = true)
        every { mockResponse.isSuccessful } returns true

        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.changePassword(any(), any())
        } returns mockResponse

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.changePassword("current", "new")

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApi.changePassword("current", "new") }
    }

    @Test
    fun changePassword_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.changePassword(any(), any())
        } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.changePassword("current", "new")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun changePassword_whenApiReturnsError400_thenReturnsFailure() = runTest {
        // Given
        val errorBody = """{"errors":["Некорректный текущий пароль"]}""".trimIndent()
            .toResponseBody("application/json".toMediaType())

        val mockResponse = mockk<Response<Unit>>(relaxed = true)
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code() } returns 400
        every { mockResponse.errorBody() } returns errorBody

        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.changePassword(any(), any())
        } returns mockResponse

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.changePassword("current", "new")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ServerException)
    }

    @Test
    fun register_whenApiReturnsSuccess_thenReturnsSuccessAndSavesToken() = runTest {
        // Given
        val mockSuccess = User(id = 456L, name = "testuser", image = null)
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.register(
                name = any(),
                fullName = any(),
                email = any(),
                password = any(),
                birthDate = any(),
                genderCode = any(),
                countryId = any(),
                cityId = any()
            )
        } returns mockSuccess

        val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter = crashReporter,
            logger = logger
        )

        // When
        val result = repository.register(
            RegistrationParams(
                name = "testuser",
                fullName = "Test User",
                email = "test@example.com",
                password = "password123",
                birthDate = "1990-01-01",
                genderCode = 1,
                countryId = null,
                cityId = null
            )
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockSuccess, result.getOrNull())
        coVerify {
            mockApi.register(
                name = any(),
                fullName = any(),
                email = any(),
                password = any(),
                birthDate = any(),
                genderCode = any(),
                countryId = any(),
                cityId = any()
            )
        }
        coVerify { mockUserDao.insert(any()) }
    }

    @Test
    fun register_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.register(
                name = any(),
                fullName = any(),
                email = any(),
                password = any(),
                birthDate = any(),
                genderCode = any(),
                countryId = any(),
                cityId = any()
            )
        } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter,
            logger
        )

        // When
        val result = repository.register(
            RegistrationParams(
                name = "testuser",
                fullName = "Test User",
                email = "test@example.com",
                password = "password123",
                birthDate = "1990-01-01",
                genderCode = 1,
                countryId = null,
                cityId = null
            )
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun resetPassword_whenServerErrorWithErrorsField_thenReturnsServerExceptionWithErrorsMessage() =
        runTest {
            val mockApi = mockk<SWApi>()
            val errorJson = """{"errors":["Не найден пользователь с таким логином или e-mail"]}"""
            val errorResponse = Response.error<Any>(
                400,
                errorJson.toResponseBody(null)
            )
            coEvery { mockApi.resetPassword(any()) } throws HttpException(errorResponse)

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository = SWRepositoryImp(
                mockApi,
                mockDataStore,
                mockUserDao,
                mockJournalDao,
                mockJournalEntryDao,
                mockDialogDao,
                mockEventDao,
                mockParkDao,
                crashReporter = crashReporter,
                logger = logger
            )

            // When
            val result = repository.resetPassword("1")

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is ServerException)
            assertEquals("Не найден пользователь с таким логином или e-mail", exception?.message)
        }

    @Test
    fun resetPassword_whenServerErrorWithMessageField_thenReturnsServerExceptionWithMessage() =
        runTest {
            val mockApi = mockk<SWApi>()
            val errorJson = """{"message":"Неверный пароль","code":401}"""
            val errorResponse = Response.error<Any>(
                401,
                errorJson.toResponseBody(null)
            )
            coEvery { mockApi.resetPassword(any()) } throws HttpException(errorResponse)

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository = SWRepositoryImp(
                mockApi,
                mockDataStore,
                mockUserDao,
                mockJournalDao,
                mockJournalEntryDao,
                mockDialogDao,
                mockEventDao,
                mockParkDao,
                crashReporter = crashReporter,
                logger = logger
            )

            // When
            val result = repository.resetPassword("test")

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is ServerException)
            assertEquals("Неверный пароль", exception?.message)
        }

    @Test
    fun resetPassword_whenServerErrorWithBothFields_thenReturnsMessageFieldPriority() = runTest {
        val mockApi = mockk<SWApi>()
        val errorJson = """{"message":"Приоритетное сообщение","errors":["Ошибка 1","Ошибка 2"]}"""
        val errorResponse = Response.error<Any>(
            400,
            errorJson.toResponseBody(null)
        )
        coEvery { mockApi.resetPassword(any()) } throws HttpException(errorResponse)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter = crashReporter,
            logger = logger
        )

        // When
        val result = repository.resetPassword("test")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ServerException)
        assertEquals("Приоритетное сообщение", exception?.message)
    }

    @Test
    fun resetPassword_whenServerErrorWithMultipleErrors_thenReturnsJoinedErrors() = runTest {
        val mockApi = mockk<SWApi>()
        val errorJson = """{"errors":["Ошибка 1","Ошибка 2","Ошибка 3"]}"""
        val errorResponse = Response.error<Any>(
            400,
            errorJson.toResponseBody(null)
        )
        coEvery { mockApi.resetPassword(any()) } throws HttpException(errorResponse)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter = crashReporter,
            logger = logger
        )

        // When
        val result = repository.resetPassword("test")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ServerException)
        assertEquals("Ошибка 1, Ошибка 2, Ошибка 3", exception?.message)
    }

    @Test
    fun resetPassword_whenServerErrorWithoutBody_thenReturnsStandardErrorMessage() = runTest {
        val mockApi = mockk<SWApi>()
        val errorResponse = Response.error<Any>(
            500,
            "".toResponseBody(null)
        )
        coEvery { mockApi.resetPassword(any()) } throws HttpException(errorResponse)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter = crashReporter,
            logger = logger
        )

        // When
        val result = repository.resetPassword("test")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ServerException)
        assertNotNull(exception?.message)
    }

    @Test
    fun login_whenServerErrorWithErrorsField_thenReturnsServerExceptionWithErrorsMessage() =
        runTest {
            val mockApi = mockk<SWApi>()
            val errorJson = """{"errors":["Неверные учетные данные"]}"""
            val errorResponse = Response.error<Any>(
                401,
                errorJson.toResponseBody(null)
            )
            coEvery { mockApi.login() } throws HttpException(errorResponse)

            val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository = SWRepositoryImp(
                mockApi,
                mockDataStore,
                mockUserDao,
                mockJournalDao,
                mockJournalEntryDao,
                mockDialogDao,
                mockEventDao,
                mockParkDao,
                crashReporter = crashReporter,
                logger = logger
            )

            // When
            val result = repository.login("test_token")

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is ServerException)
            assertEquals("Неверные учетные данные", exception?.message)
        }

    @Test
    fun register_whenServerErrorWithErrorsField_thenReturnsServerExceptionWithErrorsMessage() =
        runTest {
            val mockApi = mockk<SWApi>()
            val errorJson = """{"errors":["Email уже занят"]}"""
            val errorResponse = Response.error<Any>(
                400,
                errorJson.toResponseBody(null)
            )
            coEvery {
                mockApi.register(
                    name = any(),
                    fullName = any(),
                    email = any(),
                    password = any(),
                    birthDate = any(),
                    genderCode = any(),
                    countryId = any(),
                    cityId = any()
                )
            } throws HttpException(errorResponse)

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository = SWRepositoryImp(
                mockApi,
                mockDataStore,
                mockUserDao,
                mockJournalDao,
                mockJournalEntryDao,
                mockDialogDao,
                mockEventDao,
                mockParkDao,
                crashReporter,
                logger
            )

            // When
            val result = repository.register(
                RegistrationParams(
                    name = "testuser",
                    fullName = "Test User",
                    email = "test@example.com",
                    password = "password123",
                    birthDate = "1990-01-01",
                    genderCode = 1,
                    countryId = null,
                    cityId = null
                )
            )

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is ServerException)
            assertEquals("Email уже занят", exception?.message)
        }

    @Test
    fun resetPassword_whenEmptyErrorsArray_thenUsesDefaultMessage() = runTest {
        val mockApi = mockk<SWApi>()
        val errorJson = """{"errors":[],"code":400}"""
        val errorResponse = Response.error<Any>(
            400,
            errorJson.toResponseBody(null)
        )
        coEvery { mockApi.resetPassword(any()) } throws HttpException(errorResponse)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao,
            mockEventDao,
            mockParkDao,
            crashReporter = crashReporter,
            logger = logger
        )

        // When
        val result = repository.resetPassword("test")

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ServerException)
        assertEquals("Ошибка сервера: 400", exception?.message)
    }
}
