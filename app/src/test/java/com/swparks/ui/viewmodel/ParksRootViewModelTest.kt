package com.swparks.ui.viewmodel

import app.cash.turbine.test
import com.swparks.data.model.NewParkDraft
import com.swparks.data.preferences.ParksFilterDataStore
import com.swparks.domain.usecase.ICreateParkLocationHandler
import com.swparks.domain.usecase.IFilterParksUseCase
import com.swparks.util.Logger
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ParksRootViewModelTest {

    private lateinit var createParkLocationHandler: ICreateParkLocationHandler
    private lateinit var logger: Logger
    private lateinit var filterParksUseCase: IFilterParksUseCase
    private lateinit var parksFilterDataStore: ParksFilterDataStore
    private lateinit var viewModel: ParksRootViewModel

    @Before
    fun setup() {
        createParkLocationHandler = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        filterParksUseCase = mockk(relaxed = true)
        parksFilterDataStore = mockk(relaxed = true) {
            coEvery { filter } returns emptyFlow()
        }
        viewModel = ParksRootViewModel(
            createParkLocationHandler = createParkLocationHandler,
            logger = logger,
            filterParksUseCase = filterParksUseCase,
            parksFilterDataStore = parksFilterDataStore
        )
    }

    @Test
    fun onPermissionGranted_withSuccess_emitsNavigateWithDraft() = runTest {
        val draft = NewParkDraft(latitude = 55.751244, longitude = 37.618423, cityId = 1)
        coEvery { createParkLocationHandler() } returns Result.success(draft)

        viewModel.events.test {
            viewModel.onPermissionGranted()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ParksRootEvent.NavigateToCreatePark)
            assertEquals(draft, (event as ParksRootEvent.NavigateToCreatePark).draft)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onPermissionGranted_withEmptyDraft_emitsNavigateWithEmptyDraft() = runTest {
        coEvery { createParkLocationHandler() } returns Result.success(NewParkDraft.EMPTY)

        viewModel.events.test {
            viewModel.onPermissionGranted()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ParksRootEvent.NavigateToCreatePark)
            assertEquals(NewParkDraft.EMPTY, (event as ParksRootEvent.NavigateToCreatePark).draft)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onPermissionGranted_whenHandlerFails_emitsNavigateWithEmptyDraft() = runTest {
        coEvery { createParkLocationHandler() } returns Result.failure(Exception("Location failed"))

        viewModel.events.test {
            viewModel.onPermissionGranted()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ParksRootEvent.NavigateToCreatePark)
            assertEquals(NewParkDraft.EMPTY, (event as ParksRootEvent.NavigateToCreatePark).draft)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onPermissionDenied_withRationale_showsDialogWithCauseDenied() {
        viewModel.onPermissionDenied(shouldShowRationale = true)

        val state = viewModel.uiState.value
        assertTrue(state.showPermissionDialog)
        assertEquals(PermissionDialogCause.DENIED, state.permissionDialogCause)
    }

    @Test
    fun onPermissionDenied_withoutRationale_requestsPermission() {
        var permissionRequested = false
        viewModel.permissionLauncher = { _ -> permissionRequested = true }

        viewModel.onPermissionDenied(shouldShowRationale = false)

        assertTrue(permissionRequested)
        assertFalse(viewModel.uiState.value.showPermissionDialog)
    }

    @Test
    fun onPermissionResult_whenGranted_emitsNavigate() = runTest {
        val draft = NewParkDraft(latitude = 55.751244, longitude = 37.618423, cityId = 1)
        coEvery { createParkLocationHandler() } returns Result.success(draft)
        val permissions = mapOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION to true,
            android.Manifest.permission.ACCESS_COARSE_LOCATION to true
        )

        viewModel.events.test {
            viewModel.onPermissionResult(permissions)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ParksRootEvent.NavigateToCreatePark)
            assertEquals(draft, (event as ParksRootEvent.NavigateToCreatePark).draft)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onPermissionResult_whenDenied_showsDialogWithCauseDenied() {
        val permissions = mapOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION to false,
            android.Manifest.permission.ACCESS_COARSE_LOCATION to false
        )

        viewModel.onPermissionResult(permissions)

        val state = viewModel.uiState.value
        assertTrue(state.showPermissionDialog)
        assertEquals(PermissionDialogCause.FOREVER_DENIED, state.permissionDialogCause)
    }

    @Test
    fun onDismissDialog_hidesDialog() {
        viewModel.onPermissionDenied(shouldShowRationale = true)
        assertTrue(viewModel.uiState.value.showPermissionDialog)

        viewModel.onDismissDialog()

        assertFalse(viewModel.uiState.value.showPermissionDialog)
        assertNull(viewModel.uiState.value.permissionDialogCause)
    }

    @Test
    fun onConfirmDialog_clearsDialogAndRequestsPermission() {
        var permissionRequested = false
        viewModel.permissionLauncher = { _ -> permissionRequested = true }

        viewModel.onConfirmDialog()

        assertTrue(permissionRequested)
        assertFalse(viewModel.uiState.value.showPermissionDialog)
    }

    @Test
    fun onOpenSettings_clearsDialogAndEmitsOpenSettings() = runTest {
        viewModel.onPermissionDenied(shouldShowRationale = true)
        assertTrue(viewModel.uiState.value.showPermissionDialog)

        val mockIntent = mockk<android.content.Intent>(relaxed = true)
        var settingsOpened = false
        viewModel.openSettingsLauncher = { _ -> settingsOpened = true }

        viewModel.events.test {
            viewModel.onOpenSettings(mockIntent)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showPermissionDialog)
            assertNull(viewModel.uiState.value.permissionDialogCause)
            assertTrue(settingsOpened)

            val event = awaitItem()
            assertTrue(event is ParksRootEvent.OpenSettings)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onShowFilterDialog_showsDialogWithCurrentFilter() {
        viewModel.onShowFilterDialog()

        val state = viewModel.uiState.value
        assertTrue(state.showFilterDialog)
    }

    @Test
    fun onDismissFilterDialog_hidesFilterDialog() {
        viewModel.onShowFilterDialog()
        assertTrue(viewModel.uiState.value.showFilterDialog)

        viewModel.onDismissFilterDialog()

        assertFalse(viewModel.uiState.value.showFilterDialog)
    }

    @Test
    fun onFilterToggleSize_whenMoreThanOneSize_removesSize() {
        val initialSizes = viewModel.uiState.value.localFilter.sizes
        assertTrue("Setup: need at least 2 sizes", initialSizes.size > 1)
        val sizeToRemove = initialSizes.first()

        viewModel.onFilterToggleSize(sizeToRemove)

        val newSizes = viewModel.uiState.value.localFilter.sizes
        assertEquals(initialSizes - sizeToRemove, newSizes)
    }

    @Test
    fun onFilterToggleSize_whenOnlyOneSize_doesNotRemove() {
        val onlySize = com.swparks.data.model.ParkSize.SMALL
        viewModel.onLocalFilterChange(
            com.swparks.data.model.ParkFilter(
                sizes = setOf(onlySize),
                types = com.swparks.data.model.ParkType.entries.toSet()
            )
        )

        viewModel.onFilterToggleSize(onlySize)

        assertTrue(viewModel.uiState.value.localFilter.sizes.contains(onlySize))
    }

    @Test
    fun onFilterToggleType_whenMoreThanOneType_removesType() {
        val initialTypes = viewModel.uiState.value.localFilter.types
        assertTrue("Setup: need at least 2 types", initialTypes.size > 1)
        val typeToRemove = initialTypes.first()

        viewModel.onFilterToggleType(typeToRemove)

        val newTypes = viewModel.uiState.value.localFilter.types
        assertEquals(initialTypes - typeToRemove, newTypes)
    }

    @Test
    fun onFilterToggleType_whenOnlyOneType_doesNotRemove() {
        val onlyType = com.swparks.data.model.ParkType.SOVIET
        viewModel.onLocalFilterChange(
            com.swparks.data.model.ParkFilter(
                sizes = com.swparks.data.model.ParkSize.entries.toSet(),
                types = setOf(onlyType)
            )
        )

        viewModel.onFilterToggleType(onlyType)

        assertTrue(viewModel.uiState.value.localFilter.types.contains(onlyType))
    }

    @Test
    fun onFilterReset_resetsToDefaultFilter() {
        val nonDefaultFilter = com.swparks.data.model.ParkFilter(
            sizes = setOf(com.swparks.data.model.ParkSize.SMALL),
            types = setOf(com.swparks.data.model.ParkType.SOVIET)
        )
        viewModel.onLocalFilterChange(nonDefaultFilter)
        assertFalse(viewModel.uiState.value.localFilter.isDefault)

        viewModel.onFilterReset()

        val resetFilter = viewModel.uiState.value.localFilter
        assertTrue(resetFilter.isDefault)
    }

    @Test
    fun onFilterApply_closesFilterDialog() {
        viewModel.onShowFilterDialog()
        assertTrue(viewModel.uiState.value.showFilterDialog)

        viewModel.onFilterApply()

        assertFalse(viewModel.uiState.value.showFilterDialog)
    }
}
