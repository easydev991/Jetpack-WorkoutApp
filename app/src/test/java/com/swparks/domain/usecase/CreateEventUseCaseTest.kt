package com.swparks.domain.usecase

import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.data.repository.SWRepository
import com.swparks.ui.model.EventForm
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateEventUseCaseTest {
    private lateinit var mockRepository: SWRepository
    private lateinit var createEventUseCase: CreateEventUseCase

    private val testForm =
        EventForm(
            title = "Тренировка",
            description = "Описание",
            date = "2026-03-20 18:00:00",
            parkId = 1L
        )

    private val testEvent = createTestEvent()

    @Before
    fun setup() {
        mockRepository = mockk()
        createEventUseCase = CreateEventUseCase(mockRepository)
    }

    @Test
    fun invoke_success_returnsEvent() =
        runTest {
            coEvery {
                mockRepository.saveEvent(null, testForm, null)
            } returns Result.success(testEvent)

            val result = createEventUseCase(testForm)

            assertTrue(result.isSuccess)
            assertEquals(testEvent, result.getOrNull())
            coVerify(exactly = 1) {
                mockRepository.saveEvent(id = null, form = testForm, photos = null)
            }
        }

    @Test
    fun invoke_withPhotos_returnsEvent() =
        runTest {
            val photos = listOf(byteArrayOf(1, 2, 3))
            coEvery {
                mockRepository.saveEvent(null, testForm, photos)
            } returns Result.success(testEvent)

            val result = createEventUseCase(testForm, photos)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.saveEvent(id = null, form = testForm, photos = photos)
            }
        }

    @Test
    fun invoke_failure_returnsFailure() =
        runTest {
            val exception = RuntimeException("Network error")
            coEvery {
                mockRepository.saveEvent(null, testForm, null)
            } returns Result.failure(exception)

            val result = createEventUseCase(testForm)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    private fun createTestEvent(): Event =
        Event(
            id = 1L,
            title = "Тренировка",
            description = "Описание",
            beginDate = "2026-03-20 18:00:00",
            countryID = 1,
            cityID = 1,
            preview = "",
            latitude = "55.751244",
            longitude = "37.618423",
            isCurrent = true,
            photos = emptyList(),
            author = User(id = 1L, name = "Автор", image = null),
            name = "event"
        )
}
