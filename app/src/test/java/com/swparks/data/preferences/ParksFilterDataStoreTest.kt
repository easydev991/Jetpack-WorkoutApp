package com.swparks.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParksFilterDataStoreTest {

    private fun createDataStoreWithPreferences(
        sizesStr: String? = null,
        typesStr: String? = null
    ): DataStore<Preferences> {
        val mockDataStore = mockk<DataStore<Preferences>>(relaxed = true)
        val preferences = mockk<Preferences>()

        every { preferences[ParksFilterPreferencesKeys.SIZES_KEY] } returns sizesStr
        every { preferences[ParksFilterPreferencesKeys.TYPES_KEY] } returns typesStr
        every { mockDataStore.data } returns flowOf(preferences)

        return mockDataStore
    }

    @Test
    fun filter_withEmptyPreferences_returnsDefaultFilter() = runTest {
        val dataStore = createDataStoreWithPreferences()
        val parksFilterDataStore = ParksFilterDataStore(dataStore)

        val filter = parksFilterDataStore.filter.first()

        assertEquals(ParkSize.entries.toSet(), filter.sizes)
        assertEquals(ParkType.entries.toSet(), filter.types)
        assertTrue(filter.isDefault)
    }

    @Test
    fun filter_withCustomSizesAndTypes_returnsCustomFilter() = runTest {
        val dataStore = createDataStoreWithPreferences(
            sizesStr = "1,2",
            typesStr = "1,6"
        )
        val parksFilterDataStore = ParksFilterDataStore(dataStore)

        val filter = parksFilterDataStore.filter.first()

        assertEquals(setOf(ParkSize.SMALL, ParkSize.MEDIUM), filter.sizes)
        assertEquals(setOf(ParkType.SOVIET, ParkType.LEGENDARY), filter.types)
    }

    @Test
    fun filter_withInvalidRawValues_fallsBackToDefaults() = runTest {
        val dataStore = createDataStoreWithPreferences(
            sizesStr = "999,1",
            typesStr = "999,1"
        )
        val parksFilterDataStore = ParksFilterDataStore(dataStore)

        val filter = parksFilterDataStore.filter.first()

        assertEquals(setOf(ParkSize.SMALL), filter.sizes)
        assertEquals(setOf(ParkType.SOVIET), filter.types)
    }

    @Test
    fun filter_withEmptyString_fallsBackToDefaults() = runTest {
        val dataStore = createDataStoreWithPreferences(
            sizesStr = "",
            typesStr = ""
        )
        val parksFilterDataStore = ParksFilterDataStore(dataStore)

        val filter = parksFilterDataStore.filter.first()

        assertEquals(ParkSize.entries.toSet(), filter.sizes)
        assertEquals(ParkType.entries.toSet(), filter.types)
    }
}
