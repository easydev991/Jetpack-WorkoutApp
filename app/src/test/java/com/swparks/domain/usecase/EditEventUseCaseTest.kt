package com.swparks.domain.usecase

import com.swparks.data.model.Event
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

class EditEventUseCaseTest {
    private lateinit var mockRepository: SWRepository
    private lateinit var editEventUseCase: EditEventUseCase

    private val testEventId = 123L
    private val testForm =
        EventForm(
            title = "Обновлённая тренировка",
            description = "Новое описание",
            date = "2026-03-21 19:00:00",
            parkId = 2L
        )

    private val testEvent = createTestEvent(testEventId)

    @Before
    fun setup() {
        mockRepository = mockk()
        editEventUseCase = EditEventUseCase(mockRepository)
    }

    @Test
    fun invoke_success_returnsEvent() =
        runTest {
            coEvery {
                mockRepository.saveEvent(testEventId, testForm, null)
            } returns Result.success(testEvent)

            val result = editEventUseCase(testEventId, testForm)

            assertTrue(result.isSuccess)
            assertEquals(testEvent, result.getOrNull())
            coVerify(exactly = 1) {
                mockRepository.saveEvent(id = testEventId, form = testForm, photos = null)
            }
        }

    @Test
    fun invoke_withPhotos_returnsEvent() =
        runTest {
            val photos = listOf(byteArrayOf(4, 5, 6))
            coEvery {
                mockRepository.saveEvent(testEventId, testForm, photos)
            } returns Result.success(testEvent)

            val result = editEventUseCase(testEventId, testForm, photos)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.saveEvent(id = testEventId, form = testForm, photos = photos)
            }
        }

    @Test
    fun invoke_failure_returnsFailure() =
        runTest {
            val exception = RuntimeException("Network error")
            coEvery {
                mockRepository.saveEvent(testEventId, testForm, null)
            } returns Result.failure(exception)

            val result = editEventUseCase(testEventId, testForm)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun invoke_differentEventId_passesCorrectId() =
        runTest {
            val differentEventId = 999L
            coEvery {
                mockRepository.saveEvent(differentEventId, testForm, null)
            } returns Result.success(createTestEvent(differentEventId))

            val result = editEventUseCase(differentEventId, testForm)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                mockRepository.saveEvent(id = differentEventId, form = testForm, photos = null)
            }
        }

    private fun createTestEvent(id: Long): Event =
        Event(
            id = id,
            title = "Обновлённая тренировка",
            description = "Новое описание",
            beginDate = "2026-03-21 19:00:00",
            countryID = 1,
            cityID = 1,
            preview = "",
            latitude = "55.751244",
            longitude = "37.618423",
            isCurrent = true,
            photos = emptyList(),
            author =
                com.swparks.data.model
                    .User(id = 1L, name = "Автор", image = null),
            name = "event"
        )
}
