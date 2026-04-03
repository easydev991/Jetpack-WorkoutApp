package com.swparks.ui.model

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PickedImagesStateTest {
    @Test
    fun canAddMore_whenEmptyList_returnsTrue() {
        val state =
            PickedImagesState(
                images = emptyList(),
                selectionLimit = 5
            )

        assertTrue(state.canAddMore)
    }

    @Test
    fun canAddMore_whenNotFull_returnsTrue() {
        val state =
            PickedImagesState(
                images = createUriList(3),
                selectionLimit = 5
            )

        assertTrue(state.canAddMore)
    }

    @Test
    fun canAddMore_whenFull_returnsFalse() {
        val state =
            PickedImagesState(
                images = createUriList(5),
                selectionLimit = 5
            )

        assertFalse(state.canAddMore)
    }

    @Test
    fun canAddMore_whenOverLimit_returnsFalse() {
        val state =
            PickedImagesState(
                images = createUriList(10),
                selectionLimit = 5
            )

        assertFalse(state.canAddMore)
    }

    @Test
    fun remainingSlots_whenEmpty_returnsLimit() {
        val state =
            PickedImagesState(
                images = emptyList(),
                selectionLimit = 15
            )

        assertEquals(15, state.remainingSlots)
    }

    @Test
    fun remainingSlots_whenHalfFull_returnsHalf() {
        val state =
            PickedImagesState(
                images = createUriList(5),
                selectionLimit = 10
            )

        assertEquals(5, state.remainingSlots)
    }

    @Test
    fun remainingSlots_whenFull_returnsZero() {
        val state =
            PickedImagesState(
                images = createUriList(15),
                selectionLimit = 15
            )

        assertEquals(0, state.remainingSlots)
    }

    @Test
    fun remainingSlots_whenOverLimit_returnsZero() {
        val state =
            PickedImagesState(
                images = createUriList(20),
                selectionLimit = 15
            )

        assertEquals(0, state.remainingSlots)
    }

    @Test
    fun remainingSlots_withOneImage_returnsLimitMinusOne() {
        val state =
            PickedImagesState(
                images = createUriList(1),
                selectionLimit = 15
            )

        assertEquals(14, state.remainingSlots)
    }

    @Test
    fun remainingSlots_whenExactlyOneSlotRemaining_returnsOne() {
        val state =
            PickedImagesState(
                images = createUriList(14),
                selectionLimit = 15
            )

        assertEquals(1, state.remainingSlots)
    }

    @Test
    fun remainingSlots_whenExactlyTwoSlotsRemaining_returnsTwo() {
        val state =
            PickedImagesState(
                images = createUriList(13),
                selectionLimit = 15
            )

        assertEquals(2, state.remainingSlots)
    }

    @Test
    fun canAddMore_whenOneSlotRemaining_returnsTrue() {
        val state =
            PickedImagesState(
                images = createUriList(14),
                selectionLimit = 15
            )

        assertTrue(state.canAddMore)
    }

    @Test
    fun canAddMore_whenTwoSlotsRemaining_returnsTrue() {
        val state =
            PickedImagesState(
                images = createUriList(13),
                selectionLimit = 15
            )

        assertTrue(state.canAddMore)
    }

    @Test
    fun state_defaultValues_areCorrect() {
        val state = PickedImagesState()

        assertTrue(state.images.isEmpty())
        assertEquals(15, state.selectionLimit)
        assertTrue(state.canAddMore)
        assertEquals(15, state.remainingSlots)
    }

    private fun createUriList(count: Int): List<Uri> =
        List(count) { index ->
            Uri.parse("content://media/external/images/media/$index")
        }
}
