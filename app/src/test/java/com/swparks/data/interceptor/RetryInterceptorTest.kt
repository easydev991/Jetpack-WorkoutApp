package com.swparks.data.interceptor

import com.swparks.util.Logger
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** Unit тесты для RetryInterceptor */
class RetryInterceptorTest {

    private lateinit var logger: Logger
    private lateinit var retryInterceptor: RetryInterceptor

    @Before
    fun setup() {
        logger = mockk(relaxed = true)
        retryInterceptor = RetryInterceptor(logger)
    }

    @Test
    fun test_retry_502_should_call_chain_proceed_exactly_3_times() {
        // Given
        val mockChain = mockk<Interceptor.Chain>()
        val mockRequest = mockk<Request>()
        val mockResponse = mockk<Response>(relaxed = true)

        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(any()) } returns mockResponse
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code } returns 502
        every { mockResponse.close() } just runs

        // When
        try {
            retryInterceptor.intercept(mockChain)
        } catch (e: Exception) {
            // Ожидаем исключение после всех попыток
        }

        // Then
        verify(exactly = 3) { mockChain.proceed(mockRequest) }
        verify(atLeast = 1) { logger.w("RetryInterceptor", any()) }
    }

    @Test
    fun test_retry_503_should_call_chain_proceed_exactly_3_times() {
        // Given
        val mockChain = mockk<Interceptor.Chain>()
        val mockRequest = mockk<Request>()
        val mockResponse = mockk<Response>(relaxed = true)

        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(any()) } returns mockResponse
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code } returns 503
        every { mockResponse.close() } just runs

        // When
        try {
            retryInterceptor.intercept(mockChain)
        } catch (e: Exception) {
            // Ожидаем исключение после всех попыток
        }

        // Then
        verify(exactly = 3) { mockChain.proceed(mockRequest) }
        verify(atLeast = 1) { logger.w("RetryInterceptor", any()) }
    }

    @Test
    fun test_retry_504_should_call_chain_proceed_exactly_3_times() {
        // Given
        val mockChain = mockk<Interceptor.Chain>()
        val mockRequest = mockk<Request>()
        val mockResponse = mockk<Response>(relaxed = true)

        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(any()) } returns mockResponse
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code } returns 504
        every { mockResponse.close() } just runs

        // When
        try {
            retryInterceptor.intercept(mockChain)
        } catch (e: Exception) {
            // Ожидаем исключение после всех попыток
        }

        // Then
        verify(exactly = 3) { mockChain.proceed(mockRequest) }
        verify(atLeast = 1) { logger.w("RetryInterceptor", any()) }
    }

    @Test
    fun test_404_error_should_not_retry() {
        // Given
        val mockChain = mockk<Interceptor.Chain>()
        val mockRequest = mockk<Request>()
        val mockResponse = mockk<Response>(relaxed = true)

        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(any()) } returns mockResponse
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code } returns 404

        // When
        val result = retryInterceptor.intercept(mockChain)

        // Then
        verify(exactly = 1) { mockChain.proceed(mockRequest) } // Только 1 попытка!
        assert(result.code == 404)
    }

    @Test
    fun test_401_error_should_not_retry() {
        // Given
        val mockChain = mockk<Interceptor.Chain>()
        val mockRequest = mockk<Request>()
        val mockResponse = mockk<Response>(relaxed = true)

        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(any()) } returns mockResponse
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code } returns 401

        // When
        val result = retryInterceptor.intercept(mockChain)

        // Then
        verify(exactly = 1) { mockChain.proceed(mockRequest) } // Только 1 попытка!
        assert(result.code == 401)
    }

    @Test
    fun test_500_error_should_not_retry() {
        // Given
        val mockChain = mockk<Interceptor.Chain>()
        val mockRequest = mockk<Request>()
        val mockResponse = mockk<Response>(relaxed = true)

        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(any()) } returns mockResponse
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code } returns 500

        // When
        val result = retryInterceptor.intercept(mockChain)

        // Then
        verify(exactly = 1) { mockChain.proceed(mockRequest) } // Только 1 попытка!
        assert(result.code == 500)
    }

    @Test
    fun test_successful_response_should_not_retry() {
        // Given
        val mockChain = mockk<Interceptor.Chain>()
        val mockRequest = mockk<Request>()
        val mockResponse = mockk<Response>(relaxed = true)

        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(any()) } returns mockResponse
        every { mockResponse.isSuccessful } returns true

        // When
        retryInterceptor.intercept(mockChain)

        // Then
        verify(exactly = 1) { mockChain.proceed(mockRequest) } // Только 1 попытка!
    }

    @Test
    fun test_io_exception_should_not_retry() {
        // Given - IOException НЕ ретраится (только 502/503/504)
        val mockChain = mockk<Interceptor.Chain>()
        val mockRequest = mockk<Request>()
        val exception = IOException("Network error")

        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(mockRequest) } throws exception

        // When
        try {
            retryInterceptor.intercept(mockChain)
        } catch (e: IOException) {
            // Ожидаем исключение сразу без retry
        }

        // Then - только 1 попытка, IOException не ретраится
        verify(exactly = 1) { mockChain.proceed(mockRequest) }
        verify(exactly = 0) { logger.w("RetryInterceptor", any()) }
    }

    @Test
    fun test_retry_on_second_attempt_success_should_return_immediately() {
        // Given
        val mockChain = mockk<Interceptor.Chain>()
        val mockRequest = mockk<Request>()
        val mockError = mockk<Response>(relaxed = true)
        val mockSuccess = mockk<Response>(relaxed = true)

        every { mockChain.request() } returns mockRequest
        every { mockError.isSuccessful } returns false
        every { mockError.code } returns 502
        every { mockError.close() } just runs
        every { mockSuccess.isSuccessful } returns true

        // Первая попытка - ошибка, вторая - успех
        every { mockChain.proceed(mockRequest) } returnsMany listOf(mockError, mockSuccess)

        // When
        retryInterceptor.intercept(mockChain)

        // Then
        verify(exactly = 2) { mockChain.proceed(mockRequest) } // Точно 2 попытки!
    }

    @Test
    fun test_cancellation_exception_should_not_retry() {
        // Given
        val mockChain = mockk<Interceptor.Chain>()
        val mockRequest = mockk<Request>()
        val exception = CancellationException("Coroutine cancelled")

        every { mockChain.request() } returns mockRequest
        every { mockChain.proceed(mockRequest) } throws exception

        // When
        try {
            retryInterceptor.intercept(mockChain)
        } catch (e: CancellationException) {
            // Ожидаем CancellationException
        }

        // Then
        verify(exactly = 1) { mockChain.proceed(mockRequest) } // Только 1 попытка!
        verify(exactly = 0) { logger.w("RetryInterceptor", any()) } // Никаких предупреждений!
    }
}
