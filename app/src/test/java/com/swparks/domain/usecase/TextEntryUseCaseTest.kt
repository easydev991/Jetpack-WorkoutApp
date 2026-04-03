package com.swparks.domain.usecase

import android.util.Log
import com.swparks.data.repository.SWRepository
import com.swparks.domain.event.MessageSentNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit тесты для TextEntryUseCase */
class TextEntryUseCaseTest {
    private lateinit var swRepository: SWRepository
    private lateinit var createJournalUseCase: ICreateJournalUseCase
    private lateinit var messageSentNotifier: MessageSentNotifier
    private lateinit var textEntryUseCase: TextEntryUseCase

    private val testOwnerId = 123L
    private val testJournalId = 456L
    private val testEntryId = 789L
    private val testParkId = 101L
    private val testEventId = 202L
    private val testCommentId = 303L
    private val testText = "Test comment"

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        swRepository = mockk()
        createJournalUseCase = mockk(relaxed = true)
        messageSentNotifier = mockk(relaxed = true)
        textEntryUseCase = TextEntryUseCase(swRepository, createJournalUseCase, messageSentNotifier)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun addJournalEntry_whenSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            coEvery { swRepository.addComment(any(), any()) } returns Result.success(Unit)

            // When
            val result = textEntryUseCase.addJournalEntry(testOwnerId, testJournalId, testText)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                swRepository.addComment(
                    match { it is com.swparks.ui.model.TextEntryOption.Journal },
                    testText
                )
            }
        }

    @Test
    fun addJournalEntry_whenError_thenReturnsError() =
        runTest {
            // Given
            val exception = Exception("Network error")
            coEvery { swRepository.addComment(any(), any()) } returns Result.failure(exception)

            // When
            val result = textEntryUseCase.addJournalEntry(testOwnerId, testJournalId, testText)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 1) {
                swRepository.addComment(
                    match { it is com.swparks.ui.model.TextEntryOption.Journal },
                    testText
                )
            }
        }

    @Test
    fun editJournalEntry_whenSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            coEvery { swRepository.editComment(any(), any(), any()) } returns Result.success(Unit)

            // When
            val result =
                textEntryUseCase.editJournalEntry(testOwnerId, testJournalId, testEntryId, testText)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                swRepository.editComment(
                    match { it is com.swparks.ui.model.TextEntryOption.Journal },
                    testEntryId,
                    testText
                )
            }
        }

    @Test
    fun addParkComment_whenSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            coEvery { swRepository.addComment(any(), any()) } returns Result.success(Unit)

            // When
            val result = textEntryUseCase.addParkComment(testParkId, testText)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                swRepository.addComment(
                    match { it is com.swparks.ui.model.TextEntryOption.Park },
                    testText
                )
            }
        }

    @Test
    fun editParkComment_whenSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            coEvery { swRepository.editComment(any(), any(), any()) } returns Result.success(Unit)

            // When
            val result = textEntryUseCase.editParkComment(testParkId, testCommentId, testText)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                swRepository.editComment(
                    match { it is com.swparks.ui.model.TextEntryOption.Park },
                    testCommentId,
                    testText
                )
            }
        }

    @Test
    fun addEventComment_whenSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            coEvery { swRepository.addComment(any(), any()) } returns Result.success(Unit)

            // When
            val result = textEntryUseCase.addEventComment(testEventId, testText)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                swRepository.addComment(
                    match { it is com.swparks.ui.model.TextEntryOption.Event },
                    testText
                )
            }
        }

    @Test
    fun editEventComment_whenSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            coEvery { swRepository.editComment(any(), any(), any()) } returns Result.success(Unit)

            // When
            val result = textEntryUseCase.editEventComment(testEventId, testCommentId, testText)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                swRepository.editComment(
                    match { it is com.swparks.ui.model.TextEntryOption.Event },
                    testCommentId,
                    testText
                )
            }
        }

    @Test
    fun sendMessageTo_whenSuccess_thenReturnsSuccess() =
        runTest {
            // Given
            val userId = 123L
            val message = "Hello!"
            coEvery { swRepository.sendMessage(message, userId) } returns Result.success(Unit)

            // When
            val result = textEntryUseCase.sendMessageTo(userId, message)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { swRepository.sendMessage(message, userId) }
        }

    @Test
    fun sendMessageTo_whenError_thenReturnsError() =
        runTest {
            // Given
            val userId = 123L
            val message = "Hello!"
            val exception = Exception("Network error")
            coEvery { swRepository.sendMessage(message, userId) } returns Result.failure(exception)

            // When
            val result = textEntryUseCase.sendMessageTo(userId, message)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 1) { swRepository.sendMessage(message, userId) }
        }

    @Test
    fun sendMessageTo_whenSuccess_notifiesMessageSent() =
        runTest {
            // Given
            val userId = 123L
            val message = "Hello!"
            coEvery { swRepository.sendMessage(message, userId) } returns Result.success(Unit)

            // When
            textEntryUseCase.sendMessageTo(userId, message)

            // Then
            verify { messageSentNotifier.notifyMessageSent(userId) }
        }

    @Test
    fun sendMessageTo_whenError_doesNotNotifyMessageSent() =
        runTest {
            // Given
            val userId = 123L
            val message = "Hello!"
            coEvery {
                swRepository.sendMessage(
                    message,
                    userId
                )
            } returns Result.failure(Exception("Error"))

            // When
            textEntryUseCase.sendMessageTo(userId, message)

            // Then
            verify(exactly = 0) { messageSentNotifier.notifyMessageSent(any()) }
        }
}
