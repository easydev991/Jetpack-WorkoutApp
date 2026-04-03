package com.swparks.data.interceptor

import com.swparks.data.SecureTokenRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Unit тесты для TokenInterceptor */
class TokenInterceptorTest {
    @Before
    fun setup() {
        // Mock не требуется для TokenInterceptor
    }

    @After
    fun tearDown() {
        // Cleanup не требуется
    }

    @Test
    fun intercept_whenTokenExists_thenAddsAuthorizationHeader() {
        // Given
        val mockSecureTokenRepository = mockk<SecureTokenRepository>()
        val interceptor = TokenInterceptor(mockSecureTokenRepository)

        val chain = mockk<Interceptor.Chain>()
        val originalRequest = Request.Builder().url("https://example.com/api/test").build()
        val response = mockk<Response>()

        every { mockSecureTokenRepository.getAuthTokenSync() } returns "my_test_token_12345"
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns response

        // When
        interceptor.intercept(chain)

        // Then
        verify {
            chain.proceed(match { it.header("Authorization") == "Basic my_test_token_12345" })
        }
    }

    @Test
    fun intercept_whenTokenIsEmpty_thenDoesNotAddAuthorizationHeader() {
        // Given
        val mockSecureTokenRepository = mockk<SecureTokenRepository>()
        val interceptor = TokenInterceptor(mockSecureTokenRepository)

        val chain = mockk<Interceptor.Chain>()
        val originalRequest =
            Request
                .Builder()
                .url("https://example.com/api/test")
                .build()
        val response = mockk<Response>()

        every { mockSecureTokenRepository.getAuthTokenSync() } returns ""
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns response

        // When
        interceptor.intercept(chain)

        // Then
        verify {
            chain.proceed(match { it.header("Authorization") == null })
        }
    }

    @Test
    fun intercept_whenTokenIsNull_thenDoesNotAddAuthorizationHeader() {
        // Given
        val mockSecureTokenRepository = mockk<SecureTokenRepository>()
        val interceptor = TokenInterceptor(mockSecureTokenRepository)

        val chain = mockk<Interceptor.Chain>()
        val originalRequest =
            Request
                .Builder()
                .url("https://example.com/api/test")
                .build()
        val response = mockk<Response>()

        every { mockSecureTokenRepository.getAuthTokenSync() } returns null
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns response

        // When
        interceptor.intercept(chain)

        // Then
        verify {
            chain.proceed(match { it.header("Authorization") == null })
        }
    }

    @Test
    fun intercept_whenTokenContainsSpaces_thenTrimsAndAddsAuthorizationHeader() {
        // Given
        val mockSecureTokenRepository = mockk<SecureTokenRepository>()
        val interceptor = TokenInterceptor(mockSecureTokenRepository)

        val chain = mockk<Interceptor.Chain>()
        val originalRequest =
            Request
                .Builder()
                .url("https://example.com/api/test")
                .build()
        val response = mockk<Response>()

        every { mockSecureTokenRepository.getAuthTokenSync() } returns "  my_token_with_spaces  "
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns response

        // When
        interceptor.intercept(chain)

        // Then
        verify {
            chain.proceed(match { it.header("Authorization") == "Basic my_token_with_spaces" })
        }
    }
}
