package com.swparks.ui.screens.parks.map

import org.junit.Assert.assertEquals
import org.junit.Test

class ParkMapMarkerSizingTest {
    @Test
    fun clusterTextSize_whenOneOrTwoDigits_thenReturnsShortSize() {
        assertEquals(38f, clusterTextSize("7"))
        assertEquals(38f, clusterTextSize("19"))
    }

    @Test
    fun clusterTextSize_whenThreeDigits_thenReturnsMediumSize() {
        assertEquals(34f, clusterTextSize("104"))
    }

    @Test
    fun clusterTextSize_whenFourDigits_thenReturnsLongSize() {
        assertEquals(28f, clusterTextSize("1388"))
    }

    @Test
    fun clusterTextSize_whenFiveOrMoreCharacters_thenReturnsOverflowSize() {
        assertEquals(22f, clusterTextSize("2000+"))
    }
}
