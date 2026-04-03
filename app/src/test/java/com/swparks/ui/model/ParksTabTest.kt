package com.swparks.ui.model

import com.swparks.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ParksTabTest {
    @Test
    fun map_entry_has_correct_string_resource() {
        assertEquals(R.string.parks_map, ParksTab.MAP.description)
    }

    @Test
    fun list_entry_has_correct_string_resource() {
        assertEquals(R.string.parks_list, ParksTab.LIST.description)
    }

    @Test
    fun enum_has_two_entries() {
        assertEquals(2, ParksTab.entries.size)
    }

    @Test
    fun entries_contains_map_and_list() {
        val entries = ParksTab.entries
        assertEquals(ParksTab.MAP, entries[0])
        assertEquals(ParksTab.LIST, entries[1])
    }
}
