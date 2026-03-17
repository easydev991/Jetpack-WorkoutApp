package com.swparks.navigation

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SelectParkResultTest {

    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        savedStateHandle = SavedStateHandle()
    }

    @Test
    fun setSelectedParkResult_whenSet_thenValuesStoredInSavedStateHandle() {
        val navBackStackEntry = createNavBackStackEntry(savedStateHandle)
        navBackStackEntry.setSelectedParkResult(
            parkId = 123L,
            parkName = "Central Park"
        )

        assertEquals(123L, savedStateHandle.get<Long>(SELECTED_PARK_ID_KEY))
        assertEquals("Central Park", savedStateHandle.get<String>(SELECTED_PARK_NAME_KEY))
    }

    @Test
    fun consumeSelectedParkResult_whenValidData_thenReturnsResult() {
        savedStateHandle[SELECTED_PARK_ID_KEY] = 456L
        savedStateHandle[SELECTED_PARK_NAME_KEY] = "Gorky Park"

        val navBackStackEntry = createNavBackStackEntry(savedStateHandle)
        val result = navBackStackEntry.consumeSelectedParkResult()

        assertNotNull(result)
        assertEquals(456L, result!!.parkId)
        assertEquals("Gorky Park", result.parkName)
    }

    @Test
    fun consumeSelectedParkResult_whenMissingParkId_thenReturnsNull() {
        savedStateHandle[SELECTED_PARK_NAME_KEY] = "Gorky Park"

        val navBackStackEntry = createNavBackStackEntry(savedStateHandle)
        val result = navBackStackEntry.consumeSelectedParkResult()

        assertNull(result)
    }

    @Test
    fun consumeSelectedParkResult_whenMissingParkName_thenReturnsNull() {
        savedStateHandle[SELECTED_PARK_ID_KEY] = 456L

        val navBackStackEntry = createNavBackStackEntry(savedStateHandle)
        val result = navBackStackEntry.consumeSelectedParkResult()

        assertNull(result)
    }

    @Test
    fun consumeSelectedParkResult_whenConsumed_thenKeysRemovedFromSavedStateHandle() {
        savedStateHandle[SELECTED_PARK_ID_KEY] = 789L
        savedStateHandle[SELECTED_PARK_NAME_KEY] = "Test Park"

        val navBackStackEntry = createNavBackStackEntry(savedStateHandle)
        navBackStackEntry.consumeSelectedParkResult()

        assertNull(savedStateHandle.get<Long>(SELECTED_PARK_ID_KEY))
        assertNull(savedStateHandle.get<String>(SELECTED_PARK_NAME_KEY))
    }

    @Test
    fun consumeSelectedParkResult_whenCalledTwice_thenSecondReturnsNull() {
        savedStateHandle[SELECTED_PARK_ID_KEY] = 100L
        savedStateHandle[SELECTED_PARK_NAME_KEY] = "Park 100"

        val navBackStackEntry = createNavBackStackEntry(savedStateHandle)
        val firstResult = navBackStackEntry.consumeSelectedParkResult()
        val secondResult = navBackStackEntry.consumeSelectedParkResult()

        assertNotNull(firstResult)
        assertNull(secondResult)
    }

    private fun createNavBackStackEntry(
        savedStateHandle: SavedStateHandle
    ): androidx.navigation.NavBackStackEntry {
        return mockk {
            every { this@mockk.savedStateHandle } returns savedStateHandle
        }
    }
}
