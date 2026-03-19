package com.swparks.ui.ds

import org.junit.Assert.assertEquals
import org.junit.Test

class PickedImagesControllerTest {

    @Test
    fun resolvePickerLaunchMode_whenNoSlots_returnsDisabled() {
        assertEquals(PickerLaunchMode.Disabled, resolvePickerLaunchMode(0))
    }

    @Test
    fun resolvePickerLaunchMode_whenNegativeSlots_returnsDisabled() {
        assertEquals(PickerLaunchMode.Disabled, resolvePickerLaunchMode(-1))
    }

    @Test
    fun resolvePickerLaunchMode_whenOneSlot_returnsSingle() {
        assertEquals(PickerLaunchMode.Single, resolvePickerLaunchMode(1))
    }

    @Test
    fun resolvePickerLaunchMode_whenTwoSlots_returnsMultiple() {
        assertEquals(PickerLaunchMode.Multiple, resolvePickerLaunchMode(2))
    }
}
