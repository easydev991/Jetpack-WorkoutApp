package com.swparks.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkFilterTest {

    @Test
    fun parkFilter_default_hasAllSizesAndTypes() {
        val filter = ParkFilter()

        assertEquals(ParkSize.entries.toSet(), filter.sizes)
        assertEquals(ParkType.entries.toSet(), filter.types)
    }

    @Test
    fun parkFilter_withSelections_isNotDefault() {
        val filter = ParkFilter(
            sizes = setOf(ParkSize.SMALL),
            types = setOf(ParkType.SOVIET)
        )

        assertFalse(filter.isDefault)
    }

    @Test
    fun parkFilter_equals_whenSameSelections() {
        val filter1 = ParkFilter(
            sizes = setOf(ParkSize.SMALL, ParkSize.MEDIUM),
            types = setOf(ParkType.SOVIET, ParkType.MODERN)
        )
        val filter2 = ParkFilter(
            sizes = setOf(ParkSize.SMALL, ParkSize.MEDIUM),
            types = setOf(ParkType.SOVIET, ParkType.MODERN)
        )

        assertEquals(filter1, filter2)
        assertTrue(filter1.isDefault == filter2.isDefault)
    }
}