package com.swparks.navigation

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class NavArgsViewModelsFactoryTest {

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var navBackStackEntry: androidx.navigation.NavBackStackEntry

    @Before
    fun setUp() {
        savedStateHandle = SavedStateHandle()
        navBackStackEntry = createNavBackStackEntry(
            Bundle().apply {
                putLong("eventId", 1L)
                putString("source", "test")
            },
            savedStateHandle
        )
    }

    @Test
    fun create_whenExpectedModelClassForEditEvent_thenReturnsEditEventNavArgsViewModel() {
        val factory = EditEventNavArgsViewModel.factory(navBackStackEntry)

        val result = factory.create(EditEventNavArgsViewModel::class.java)

        assertNotNull(result)
        assertEquals(EditEventNavArgsViewModel::class.java, result::class.java)
    }

    @Test
    fun create_whenUnexpectedModelClassForEditEvent_thenThrowsIllegalArgumentException() {
        val factory = EditEventNavArgsViewModel.factory(navBackStackEntry)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnexpectedViewModel::class.java)
        }

        val message = exception.message.orEmpty()
        assertTrue(message.contains("Неизвестный класс ViewModel"))
        assertTrue(message.contains(UnexpectedViewModel::class.java.name))
    }

    @Test
    fun create_whenExpectedModelClassForEventParticipants_thenReturnsEventParticipantsNavArgsViewModel() {
        val factory = EventParticipantsNavArgsViewModel.factory(navBackStackEntry)

        val result = factory.create(EventParticipantsNavArgsViewModel::class.java)

        assertNotNull(result)
        assertEquals(EventParticipantsNavArgsViewModel::class.java, result::class.java)
    }

    @Test
    fun create_whenUnexpectedModelClassForEventParticipants_thenThrowsIllegalArgumentException() {
        val factory = EventParticipantsNavArgsViewModel.factory(navBackStackEntry)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnexpectedViewModel::class.java)
        }

        val message = exception.message.orEmpty()
        assertTrue(message.contains("Неизвестный класс ViewModel"))
        assertTrue(message.contains(UnexpectedViewModel::class.java.name))
    }

    private class UnexpectedViewModel : ViewModel()

    private fun createNavBackStackEntry(
        arguments: Bundle,
        savedStateHandle: SavedStateHandle
    ): androidx.navigation.NavBackStackEntry {
        return mockk {
            every { this@mockk.arguments } returns arguments
            every { this@mockk.savedStateHandle } returns savedStateHandle
        }
    }
}
