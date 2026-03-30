package com.swparks.ui.viewmodel

import app.cash.turbine.test
import com.swparks.data.model.City
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.data.model.ParkFilter
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.data.preferences.ParksFilterDataStore
import com.swparks.data.repository.SWRepository
import com.swparks.domain.model.LocationCoordinates
import com.swparks.domain.provider.LocationService
import com.swparks.domain.repository.CountriesRepository
import com.swparks.domain.usecase.FilterParksUseCase
import com.swparks.domain.usecase.ICreateParkLocationHandler
import com.swparks.domain.usecase.IFilterParksUseCase
import com.swparks.domain.usecase.IInitializeParksUseCase
import com.swparks.domain.usecase.SyncParksUseCase
import com.swparks.ui.model.ParksTab
import com.swparks.util.Logger
import com.swparks.util.UserNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var countriesRepository: CountriesRepository
    private lateinit var swRepository: SWRepository
    private lateinit var initializeParksUseCase: IInitializeParksUseCase
    private lateinit var userNotifier: UserNotifier
    private lateinit var locationService: LocationService
    private lateinit var syncParksUseCase: SyncParksUseCase
    private lateinit var viewModel: ParksRootViewModel
    private lateinit var parksFlow: MutableStateFlow<List<Park>>

    private fun createPark(
        id: Long,
        cityID: Int,
        sizeID: Int,
        typeID: Int,
        latitude: String = "0.0",
        longitude: String = "0.0"
    ) = Park(
        id = id,
        name = "Park$id",
        cityID = cityID,
        sizeID = sizeID,
        typeID = typeID,
        longitude = longitude,
        latitude = latitude,
        address = "Address$id",
        countryID = 1,
        preview = "preview$id"
    )

    @Before
    fun setup() {
        createParkLocationHandler = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        filterParksUseCase = mockk(relaxed = true)
        parksFilterDataStore = mockk(relaxed = true) {
            coEvery { filter } returns emptyFlow()
        }
        countriesRepository = mockk(relaxed = true)
        coEvery { countriesRepository.getAllCities() } returns emptyList()
        parksFlow = MutableStateFlow(emptyList())
        swRepository = mockk(relaxed = true) {
            every { getParksFlow() } returns parksFlow
        }
        initializeParksUseCase = mockk(relaxed = true) {
            coEvery { this@mockk.invoke() } returns Result.success(Unit)
        }
        userNotifier = mockk(relaxed = true)
        locationService = mockk(relaxed = true)
        syncParksUseCase = mockk(relaxed = true) {
            coEvery { this@mockk.invoke() } returns Result.success(Unit)
            coEvery { this@mockk.invoke(force = true) } returns Result.success(Unit)
        }
        viewModel = ParksRootViewModel(
            createParkLocationHandler = createParkLocationHandler,
            logger = logger,
            filterParksUseCase = filterParksUseCase,
            parksFilterDataStore = parksFilterDataStore,
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            initializeParksUseCase = initializeParksUseCase,
            userNotifier = userNotifier,
            locationService = locationService,
            syncParksUseCase = syncParksUseCase
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
    fun onMapEvent_clusterClick_updatesCameraPosition() {
        val target = com.swparks.ui.state.UiCoordinates(latitude = 55.75, longitude = 37.61)

        viewModel.onMapEvent(com.swparks.ui.state.MapEvent.ClusterClick(target, 12))

        val cameraPosition = viewModel.uiState.value.mapState.cameraPosition
        assertEquals(target, cameraPosition?.target)
        assertEquals(12.0, cameraPosition?.zoom ?: 0.0, 0.0)
    }

    @Test
    fun onCitySelected_updatesCameraPositionToSelectedCity() = runTest {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")
        coEvery { countriesRepository.getAllCities() } returns listOf(city)
        coEvery { countriesRepository.getCityById(any()) } returns city
        every { filterParksUseCase.invoke(any(), any()) } returns emptyList()

        val viewModel = ParksRootViewModel(
            createParkLocationHandler = createParkLocationHandler,
            logger = logger,
            filterParksUseCase = filterParksUseCase,
            parksFilterDataStore = parksFilterDataStore,
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            initializeParksUseCase = initializeParksUseCase,
            userNotifier = userNotifier,
            locationService = locationService,
            syncParksUseCase = syncParksUseCase
        )
        advanceUntilIdle()

        viewModel.onMapEvent(
            com.swparks.ui.state.MapEvent.OnCameraIdle(
                com.swparks.ui.state.MapCameraPosition(
                    target = com.swparks.ui.state.UiCoordinates(64.0, 94.0),
                    zoom = 3.5
                )
            )
        )
        viewModel.onCitySelected("Moscow")

        val cameraPosition = viewModel.uiState.value.mapState.cameraPosition
        assertEquals(55.75, cameraPosition?.target?.latitude ?: 0.0, 0.0)
        assertEquals(37.61, cameraPosition?.target?.longitude ?: 0.0, 0.0)
        assertEquals(11.0, cameraPosition?.zoom ?: 0.0, 0.0)
    }

    @Test
    fun onClearCityFilter_preservesCurrentCameraPosition() = runTest {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")
        coEvery { countriesRepository.getAllCities() } returns listOf(city)
        coEvery { countriesRepository.getCityById(any()) } returns city
        every { filterParksUseCase.invoke(any(), any()) } returns emptyList()

        val viewModel = ParksRootViewModel(
            createParkLocationHandler = createParkLocationHandler,
            logger = logger,
            filterParksUseCase = filterParksUseCase,
            parksFilterDataStore = parksFilterDataStore,
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            initializeParksUseCase = initializeParksUseCase,
            userNotifier = userNotifier,
            locationService = locationService,
            syncParksUseCase = syncParksUseCase
        )
        advanceUntilIdle()

        viewModel.onCitySelected("Moscow")
        val cityCamera = viewModel.uiState.value.mapState.cameraPosition
        viewModel.onClearCityFilter()

        assertEquals(cityCamera, viewModel.uiState.value.mapState.cameraPosition)
        assertNull(viewModel.uiState.value.selectedCity)
    }

    @Test
    fun onLocationSettingsResolutionResult_success_fetchesCurrentLocation() = runTest {
        val coordinates = LocationCoordinates(latitude = 55.75, longitude = 37.61)
        coEvery { locationService.getCurrentLocation() } returns Result.success(coordinates)
        viewModel.onMapEvent(com.swparks.ui.state.MapEvent.OnLocationPermissionResult(true))

        viewModel.onLocationSettingsResolutionResult(true)
        advanceUntilIdle()

        val userLocation = viewModel.uiState.value.mapState.userLocation
        assertEquals(55.75, userLocation?.latitude ?: 0.0, 0.0)
        assertEquals(37.61, userLocation?.longitude ?: 0.0, 0.0)
        val cameraPosition = viewModel.uiState.value.mapState.cameraPosition
        assertEquals(55.75, cameraPosition?.target?.latitude ?: 0.0, 0.0)
        assertEquals(37.61, cameraPosition?.target?.longitude ?: 0.0, 0.0)
        assertEquals(15.0, cameraPosition?.zoom ?: 0.0, 0.0)
        assertFalse(viewModel.uiState.value.mapState.isFollowingUser)
        assertFalse(viewModel.uiState.value.mapState.isLoadingLocation)
    }

    @Test
    fun onLocationSettingsResolutionResult_cancel_stopsLoading() {
        viewModel.onMapEvent(com.swparks.ui.state.MapEvent.OnLocationPermissionResult(true))

        viewModel.onLocationSettingsResolutionResult(false)

        assertFalse(viewModel.uiState.value.mapState.isLoadingLocation)
    }

    @Test
    fun onMapEvent_centerOnUser_withoutPermission_doesNotStartLoading() {
        viewModel.onMapEvent(com.swparks.ui.state.MapEvent.OnLocationPermissionResult(false))

        viewModel.onMapEvent(com.swparks.ui.state.MapEvent.CenterOnUser)

        assertFalse(viewModel.uiState.value.mapState.isLoadingLocation)
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
        val onlySize = ParkSize.SMALL
        viewModel.onLocalFilterChange(
            ParkFilter(
                sizes = setOf(onlySize),
                types = ParkType.entries.toSet()
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
        val onlyType = ParkType.SOVIET
        viewModel.onLocalFilterChange(
            ParkFilter(
                sizes = ParkSize.entries.toSet(),
                types = setOf(onlyType)
            )
        )

        viewModel.onFilterToggleType(onlyType)

        assertTrue(viewModel.uiState.value.localFilter.types.contains(onlyType))
    }

    @Test
    fun onFilterReset_resetsToDefaultFilter() {
        val nonDefaultFilter = ParkFilter(
            sizes = setOf(ParkSize.SMALL),
            types = setOf(ParkType.SOVIET)
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

    @Test
    fun onFilterApply_whenCitySelected_savesFilterWithSelectedCityId() = runTest {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")
        coEvery { countriesRepository.getAllCities() } returns listOf(city)
        coEvery { countriesRepository.getCityById(any()) } returns city

        every { filterParksUseCase.invoke(any(), any()) } returns emptyList()
        val savedFilterSlot = slot<ParkFilter>()
        coEvery { parksFilterDataStore.saveFilter(capture(savedFilterSlot)) } returns Unit

        val viewModel = ParksRootViewModel(
            createParkLocationHandler = createParkLocationHandler,
            logger = logger,
            filterParksUseCase = filterParksUseCase,
            parksFilterDataStore = parksFilterDataStore,
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            initializeParksUseCase = initializeParksUseCase,
            userNotifier = userNotifier,
            locationService = locationService,
            syncParksUseCase = syncParksUseCase
        )
        advanceUntilIdle()

        viewModel.onShowFilterDialog()
        viewModel.onLocalFilterChange(
            ParkFilter(
                sizes = setOf(ParkSize.SMALL, ParkSize.LARGE),
                types = ParkType.entries.toSet()
            )
        )
        viewModel.onCitySelected("Moscow")
        viewModel.onLocalFilterChange(
            ParkFilter(
                sizes = setOf(ParkSize.SMALL, ParkSize.LARGE),
                types = ParkType.entries.toSet()
            )
        )
        viewModel.onFilterApply()
        advanceUntilIdle()

        assertEquals(1, savedFilterSlot.captured.selectedCityId)
        assertEquals(
            setOf(ParkSize.SMALL, ParkSize.LARGE),
            savedFilterSlot.captured.sizes
        )
    }

    @Test
    fun onFilterApply_whenCitySelectedAndSizeFilterApplied_filtersParksByBoth() = runTest {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")
        val park1 = createPark(id = 1L, cityID = 1, sizeID = 1, typeID = 1)
        val park2 = createPark(id = 2L, cityID = 1, sizeID = 3, typeID = 1)
        val park3 = createPark(id = 3L, cityID = 2, sizeID = 1, typeID = 1)
        val allParks = listOf(park1, park2, park3)

        coEvery { countriesRepository.getAllCities() } returns listOf(city)
        coEvery { countriesRepository.getCityById(any()) } returns city
        coEvery { parksFilterDataStore.saveFilter(any()) } returns Unit

        val realFilterParksUseCase = FilterParksUseCase()

        val viewModel = ParksRootViewModel(
            createParkLocationHandler = createParkLocationHandler,
            logger = logger,
            filterParksUseCase = realFilterParksUseCase,
            parksFilterDataStore = parksFilterDataStore,
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            initializeParksUseCase = initializeParksUseCase,
            userNotifier = userNotifier,
            locationService = locationService,
            syncParksUseCase = syncParksUseCase
        )
        advanceUntilIdle()

        viewModel.onCitySelected("Moscow")
        advanceUntilIdle()

        viewModel.onShowFilterDialog()
        viewModel.onLocalFilterChange(
            ParkFilter(
                sizes = setOf(ParkSize.LARGE),
                types = ParkType.entries.toSet()
            )
        )
        viewModel.onFilterApply()
        advanceUntilIdle()

        coVerify { parksFilterDataStore.saveFilter(any()) }

        viewModel.updateParks(allParks)
        advanceUntilIdle()

        val combinedFilter = ParkFilter(
            sizes = setOf(ParkSize.LARGE),
            types = ParkType.entries.toSet(),
            selectedCityId = 1
        )
        val expectedFiltered = realFilterParksUseCase(allParks, combinedFilter)

        val state = viewModel.uiState.value
        assertEquals(expectedFiltered.size, state.filteredParks.size)
        assertEquals(expectedFiltered.first().id, state.filteredParks.first().id)
    }

    @Test
    fun onCitySelected_whenCityIdMatchesRemoteParks_normalizesByDistanceToSelectedCity() = runTest {
        val city = City(id = "1", name = "Moscow", lat = "55.753215", lon = "37.622504")
        val nearCenterPark = createPark(
            id = 1L,
            cityID = 1,
            sizeID = 1,
            typeID = 1,
            latitude = "55.751244",
            longitude = "37.618423"
        )
        val secondNearPark = createPark(
            id = 2L,
            cityID = 1,
            sizeID = 1,
            typeID = 1,
            latitude = "55.765000",
            longitude = "37.605000"
        )
        val farAwayPark = createPark(
            id = 3L,
            cityID = 1,
            sizeID = 1,
            typeID = 1,
            latitude = "48.856613",
            longitude = "2.352222"
        )
        val allParks = listOf(nearCenterPark, secondNearPark, farAwayPark)
        val realFilterParksUseCase = FilterParksUseCase()

        coEvery { countriesRepository.getAllCities() } returns listOf(city)
        coEvery { countriesRepository.getCityById("1") } returns city

        val viewModel = ParksRootViewModel(
            createParkLocationHandler = createParkLocationHandler,
            logger = logger,
            filterParksUseCase = realFilterParksUseCase,
            parksFilterDataStore = parksFilterDataStore,
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            initializeParksUseCase = initializeParksUseCase,
            userNotifier = userNotifier,
            locationService = locationService,
            syncParksUseCase = syncParksUseCase
        )
        advanceUntilIdle()

        viewModel.updateParks(allParks)
        viewModel.onCitySelected("Moscow")
        advanceUntilIdle()

        val filteredIds = viewModel.uiState.value.filteredParks.map { it.id }
        assertEquals(listOf(1L, 2L), filteredIds)
    }

    @Test
    fun init_whenFilterRestored_restoresSelectedCityAndNormalizesFilteredParks() = runTest {
        val city = City(id = "1", name = "Moscow", lat = "55.753215", lon = "37.622504")
        val nearCenterPark = createPark(
            id = 1L,
            cityID = 1,
            sizeID = 1,
            typeID = 1,
            latitude = "55.751244",
            longitude = "37.618423"
        )
        val farAwayPark = createPark(
            id = 2L,
            cityID = 1,
            sizeID = 1,
            typeID = 1,
            latitude = "64.540100",
            longitude = "40.543300"
        )
        val allParks = listOf(nearCenterPark, farAwayPark)
        val realFilterParksUseCase = FilterParksUseCase()
        val restoredFilter = ParkFilter(selectedCityId = 1)
        val restoringDataStore = mockk<ParksFilterDataStore>(relaxed = true) {
            coEvery { filter } returns flowOf(restoredFilter)
        }

        coEvery { countriesRepository.getAllCities() } returns listOf(city)
        coEvery { countriesRepository.getCityById("1") } returns city

        val viewModel = ParksRootViewModel(
            createParkLocationHandler = createParkLocationHandler,
            logger = logger,
            filterParksUseCase = realFilterParksUseCase,
            parksFilterDataStore = restoringDataStore,
            countriesRepository = countriesRepository,
            swRepository = swRepository,
            initializeParksUseCase = initializeParksUseCase,
            userNotifier = userNotifier,
            locationService = locationService,
            syncParksUseCase = syncParksUseCase
        )

        viewModel.updateParks(allParks)
        advanceUntilIdle()

        assertEquals("Moscow", viewModel.uiState.value.selectedCity?.name)
        assertEquals(listOf(1L), viewModel.uiState.value.filteredParks.map { it.id })
    }

    @Test
    fun init_importsSeedAndCallsSyncParksUseCaseWithoutForce() = runTest {
        advanceUntilIdle()

        coVerify(exactly = 1) { initializeParksUseCase.invoke() }
        coVerify(exactly = 1) { syncParksUseCase.invoke() }
        coVerify(exactly = 0) { syncParksUseCase.invoke(force = true) }
    }

    @Test
    fun init_whenParksFlowEmits_updatesUiStateFromLocalStorage() = runTest {
        val parks = listOf(
            createPark(id = 1L, cityID = 1, sizeID = 1, typeID = 1),
            createPark(id = 2L, cityID = 2, sizeID = 2, typeID = 2)
        )
        every { filterParksUseCase.invoke(parks, any()) } returns parks

        parksFlow.value = parks
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasParks)
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.filteredParks.map { it.id })
    }

    @Test
    fun refresh_callsSyncParksUseCaseWithForceTrue() = runTest {
        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 1) { syncParksUseCase.invoke(force = true) }
    }

    @Test
    fun refresh_updatesRefreshingStateWhileSyncInProgress() = runTest {
        val refreshGate = CompletableDeferred<Unit>()
        coEvery { syncParksUseCase.invoke(force = true) } coAnswers {
            refreshGate.await()
            Result.success(Unit)
        }

        viewModel.refresh()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRefreshing)

        refreshGate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun refresh_whenAlreadyRefreshing_doesNotLaunchAnotherCoroutines() = runTest {
        val refreshGate = CompletableDeferred<Unit>()
        coEvery { syncParksUseCase.invoke(force = true) } coAnswers {
            refreshGate.await()
            Result.success(Unit)
        }

        viewModel.refresh()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRefreshing)

        viewModel.refresh()
        viewModel.refresh()

        refreshGate.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { syncParksUseCase.invoke(force = true) }
    }

    @Test
    fun showNoParksFound_whenFilteredParksEmptyAndCitySelectedAndParksLoadedAndNotLoading_showsNoParksFound() {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")

        val state = ParksRootUiState(
            selectedCity = city,
            filteredParks = emptyList(),
            hasParks = true,
            isLoadingFilter = false,
            isLoadingCities = false
        )

        assertTrue(state.showNoParksFound)
    }

    @Test
    fun showNoParksFound_whenNoCitySelected_doesNotShowNoParksFound() {
        val state = ParksRootUiState(
            selectedCity = null,
            filteredParks = emptyList(),
            hasParks = true,
            isLoadingFilter = false,
            isLoadingCities = false
        )

        assertFalse(state.showNoParksFound)
    }

    @Test
    fun showNoParksFound_whenParksNotYetLoaded_doesNotShowNoParksFound() {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")

        val state = ParksRootUiState(
            selectedCity = city,
            filteredParks = emptyList(),
            hasParks = false,
            isLoadingFilter = false,
            isLoadingCities = false
        )

        assertFalse(state.showNoParksFound)
    }

    @Test
    fun showNoParksFound_whenLoadingCities_doesNotShowNoParksFound() {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")

        val state = ParksRootUiState(
            selectedCity = city,
            filteredParks = emptyList(),
            hasParks = true,
            isLoadingFilter = false,
            isLoadingCities = true
        )

        assertFalse(state.showNoParksFound)
    }

    @Test
    fun isSizeTypeFilterEdited_whenSizeFilterChanged_returnsTrue() {
        val state = ParksRootUiState(
            localFilter = ParkFilter(
                sizes = setOf(ParkSize.SMALL),
                types = ParkType.entries.toSet()
            )
        )

        assertTrue(state.isSizeTypeFilterEdited)
    }

    @Test
    fun isSizeTypeFilterEdited_whenOnlyCitySelected_returnsFalse() {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")

        val state = ParksRootUiState(
            selectedCity = city,
            localFilter = ParkFilter()
        )

        assertFalse(state.isSizeTypeFilterEdited)
    }

    @Test
    fun toItemListUiState_whenCitySelected_setsSelectedItemToCityName() {
        val city = City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61")
        val cities = listOf(city)

        val state = ParksRootUiState(
            selectedCity = city,
            cities = cities
        )

        val result = state.toItemListUiState()

        assertEquals("Moscow", result.selectedItem)
    }

    @Test
    fun toItemListUiState_whenNoCitySelected_setsSelectedItemToNull() {
        val cities = listOf(
            City(id = "1", name = "Moscow", lat = "55.75", lon = "37.61"),
            City(id = "2", name = "Saint Petersburg", lat = "59.93", lon = "30.33")
        )

        val state = ParksRootUiState(
            selectedCity = null,
            cities = cities
        )

        val result = state.toItemListUiState()

        assertNull(result.selectedItem)
    }

    @Test
    fun selectedTab_initial_shouldBeList() {
        val state = viewModel.selectedTab.value
        assertEquals(ParksTab.LIST, state)
    }

    @Test
    fun onTabSelected_whenMap_thenSelectedTabIsMap() {
        viewModel.onTabSelected(ParksTab.MAP)
        assertEquals(ParksTab.MAP, viewModel.selectedTab.value)
    }

    @Test
    fun onTabSelected_whenList_thenSelectedTabIsList() {
        viewModel.onTabSelected(ParksTab.MAP)
        viewModel.onTabSelected(ParksTab.LIST)
        assertEquals(ParksTab.LIST, viewModel.selectedTab.value)
    }
}
