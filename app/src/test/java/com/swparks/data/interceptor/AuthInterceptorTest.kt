package com.swparks.data.interceptor

import android.util.Log
import com.swparks.data.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Unit тесты для AuthInterceptor */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthInterceptorTest {
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun intercept_whenResponseCodeIs401_thenLogsErrorAndCallsClearAllUserData() = runTest {
        // Given
        val mockPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
        val interceptor = AuthInterceptor(mockPreferencesRepository)

        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url("https://example.com/api/test").build()
        val response = mockk<Response>()

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response
        every { response.code } returns 401

        // When
        interceptor.intercept(chain)

        // Then
        verify {
            Log.e("AuthInterceptor", match { it.contains("Ошибка авторизации (401)") })
            Log.i("AuthInterceptor", "Данные авторизации очищены (isAuthorized + currentUserId)")
        }
        verify { mockPreferencesRepository.clearAllUserData() }
    }

    @Test
    fun intercept_whenResponseCodeIs401_onLoginEndpoint_thenDoesNotClearAllUserData() = runTest {
        // Given
        val mockPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
        val interceptor = AuthInterceptor(mockPreferencesRepository)

        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url("https://example.com/api/login").build()
        val response = mockk<Response>()

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response
        every { response.code } returns 401

        // When
        interceptor.intercept(chain)

        // Then
        verify(exactly = 0) { mockPreferencesRepository.clearAllUserData() }
    }

    @Test
    fun intercept_whenResponseCodeIs401_onRegisterEndpoint_thenDoesNotClearAllUserData() = runTest {
        // Given
        val mockPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
        val interceptor = AuthInterceptor(mockPreferencesRepository)

        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url("https://example.com/api/register").build()
        val response = mockk<Response>()

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response
        every { response.code } returns 401

        // When
        interceptor.intercept(chain)

        // Then
        verify(exactly = 0) { mockPreferencesRepository.clearAllUserData() }
    }

    @Test
    fun intercept_whenResponseCodeIs401_onResetPasswordEndpoint_thenDoesNotClearAllUserData() =
        runTest {
            // Given
            val mockPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
            val interceptor = AuthInterceptor(mockPreferencesRepository)

            val chain = mockk<Interceptor.Chain>()
            val request = Request.Builder().url("https://example.com/api/reset-password").build()
            val response = mockk<Response>()

            every { chain.request() } returns request
            every { chain.proceed(request) } returns response
            every { response.code } returns 401

            // When
            interceptor.intercept(chain)

            // Then
            verify(exactly = 0) { mockPreferencesRepository.clearAllUserData() }
        }

    @Test
    fun intercept_whenResponseCodeIsNot401_thenDoesNotClearAllUserData() = runTest {
        // Given
        val mockPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
        val interceptor = AuthInterceptor(mockPreferencesRepository)

        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url("https://example.com/api/test").build()
        val response = mockk<Response>()

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response
        every { response.code } returns 200

        // When
        interceptor.intercept(chain)

        // Then
        verify(exactly = 0) { mockPreferencesRepository.clearAllUserData() }
    }

    @Test
    fun intercept_whenResponseCodeIs500_thenDoesNotClearAllUserData() = runTest {
        // Given
        val mockPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
        val interceptor = AuthInterceptor(mockPreferencesRepository)

        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url("https://example.com/api/test").build()
        val response = mockk<Response>()

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response
        every { response.code } returns 500

        // When
        interceptor.intercept(chain)

        // Then
        verify(exactly = 0) { mockPreferencesRepository.clearAllUserData() }
    }
}
