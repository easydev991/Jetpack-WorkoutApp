package com.swparks.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.swparks.data.database.dao.DialogDao
import com.swparks.data.database.dao.JournalDao
import com.swparks.data.database.dao.JournalEntryDao
import com.swparks.data.database.dao.UserDao
import com.swparks.data.model.LoginSuccess
import com.swparks.domain.exception.NetworkException
import com.swparks.domain.exception.ServerException
import com.swparks.network.SWApi
import com.swparks.ui.model.RegistrationRequest
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
            mockDialogDao
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
            mockDialogDao
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
            mockDialogDao
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
            mockDialogDao
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
            mockDialogDao
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
        val mockApi = mockk<SWApi>()
        coEvery {
            mockApi.changePassword(any(), any())
        } returns mockk(relaxed = true)

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
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
            mockDialogDao
        )

        // When
        val result = repository.changePassword("current", "new")

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun register_whenApiReturnsSuccess_thenReturnsSuccessAndSavesToken() = runTest {
        // Given
        val mockSuccess = LoginSuccess(userId = 456L)
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.register(any()) } returns mockSuccess

        val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )

        // When
        val result = repository.register(
            RegistrationRequest(
                name = "testuser",
                fullName = "Test User",
                email = "test@example.com",
                password = "password123",
                birthDate = "1990-01-01",
                genderCode = 1
            )
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockSuccess, result.getOrNull())
        coVerify { mockApi.register(any()) }
    }

    @Test
    fun register_whenApiThrowsException_thenReturnsFailure() = runTest {
        // Given
        val mockApi = mockk<SWApi>()
        coEvery { mockApi.register(any()) } throws IOException("Network error")

        val mockDataStore = mockk<DataStore<Preferences>>()
        every { mockDataStore.data } returns flowOf(emptyPreferences())

        val repository = SWRepositoryImp(
            mockApi,
            mockDataStore,
            mockUserDao,
            mockJournalDao,
            mockJournalEntryDao,
            mockDialogDao
        )

        // When
        val result = repository.register(
            RegistrationRequest(
                name = "testuser",
                fullName = "Test User",
                email = "test@example.com",
                password = "password123",
                birthDate = "1990-01-01",
                genderCode = 1
            )
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    // === Дополнительные тесты для handleHttpException ===

    @Test
    fun resetPassword_whenServerErrorWithErrorsField_thenReturnsServerExceptionWithErrorsMessage() =
        runTest {
            // Given - сервер возвращает ошибку с полем errors
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
                mockDialogDao
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
            // Given - сервер возвращает ошибку с полем message
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
                mockDialogDao
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
        // Given - сервер возвращает ошибку с полями message и errors (message имеет приоритет)
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
            mockDialogDao
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
        // Given - сервер возвращает ошибку с несколькими ошибками в массиве errors
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
            mockDialogDao
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
        // Given - сервер возвращает ошибку без тела ответа
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
            mockDialogDao
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
            // Given - сервер возвращает ошибку авторизации с полем errors
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
                mockDialogDao
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
            // Given - сервер возвращает ошибку регистрации с полем errors
            val mockApi = mockk<SWApi>()
            val errorJson = """{"errors":["Email уже занят"]}"""
            val errorResponse = Response.error<Any>(
                400,
                errorJson.toResponseBody(null)
            )
            coEvery { mockApi.register(any()) } throws HttpException(errorResponse)

            val mockDataStore = mockk<DataStore<Preferences>>()
            every { mockDataStore.data } returns flowOf(emptyPreferences())

            val repository = SWRepositoryImp(
                mockApi,
                mockDataStore,
                mockUserDao,
                mockJournalDao,
                mockJournalEntryDao,
                mockDialogDao
            )

            // When
            val result = repository.register(
                RegistrationRequest(
                    name = "testuser",
                    fullName = "Test User",
                    email = "test@example.com",
                    password = "password123",
                    birthDate = "1990-01-01",
                    genderCode = 1
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
        // Given - сервер возвращает ошибку с пустым массивом errors
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
            mockDialogDao
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
