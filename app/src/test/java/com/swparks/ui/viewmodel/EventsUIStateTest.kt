package com.swparks.ui.viewmodel

import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.ui.model.EventKind
import com.swparks.ui.state.EventsUIState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsUIStateTest {
    private fun createTestEvent(id: Long = 1L): Event =
        Event(
            id = id,
            title = "Test Event $id",
            description = "Test Description",
            beginDate = "2024-01-01",
            countryID = 1,
            cityID = 1,
            preview = "",
            latitude = "0.0",
            longitude = "0.0",
            isCurrent = false,
            photos = emptyList(),
            author = User(id = 1L, name = "testuser", image = ""),
            name = "Test Event $id"
        )

    @Test
    fun Content_withAddresses_shouldStoreThem() {
        val addresses =
            mapOf(
                (1 to 1) to "Россия, Москва",
                (2 to 3) to "США, Нью-Йорк"
            )

        val state =
            EventsUIState.Content(
                events = listOf(createTestEvent()),
                selectedTab = EventKind.FUTURE,
                addresses = addresses
            )

        assertEquals(addresses, state.addresses)
        assertEquals("Россия, Москва", state.addresses[1 to 1])
        assertEquals("США, Нью-Йорк", state.addresses[2 to 3])
    }

    @Test
    fun Content_defaultAddresses_shouldBeEmpty() {
        val state =
            EventsUIState.Content(
                events = listOf(createTestEvent()),
                selectedTab = EventKind.FUTURE
            )

        assertTrue(state.addresses.isEmpty())
    }

    @Test
    fun Error_withAddresses_shouldStoreThem() {
        val addresses =
            mapOf(
                (1 to 1) to "Россия, Москва"
            )

        val state =
            EventsUIState.Error(
                message = "Ошибка загрузки",
                addresses = addresses
            )

        assertEquals(addresses, state.addresses)
        assertEquals("Россия, Москва", state.addresses[1 to 1])
    }

    @Test
    fun Error_defaultAddresses_shouldBeEmpty() {
        val state = EventsUIState.Error(message = "Ошибка загрузки")

        assertTrue(state.addresses.isEmpty())
    }
}
