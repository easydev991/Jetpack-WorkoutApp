package com.swparks.viewmodel

import android.util.Log
import com.swparks.data.preferences.AppSettingsDataStore
import com.swparks.domain.model.AppIcon
import com.swparks.domain.model.AppTheme
import com.swparks.domain.usecase.IconManager
import com.swparks.ui.viewmodel.ThemeIconViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Unit-тесты для ThemeIconViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeIconViewModelTest {
    private lateinit var mockDataStore: AppSettingsDataStore
    private lateinit var mockIconManager: IconManager
    private lateinit var viewModel: ThemeIconViewModel
    private lateinit var testDispatcher: TestDispatcher

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupMockk() {
            // Мокируем Log для использования в тестах
            mockkStatic(Log::class)
        }

        @AfterClass
        @JvmStatic
        fun tearDownMockk() {
            // Размокируем Log
            unmockkAll()
        }
    }

    @Before
    fun setUp() {
        mockDataStore = mockk(relaxed = true)
        mockIconManager = mockk()
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Мокируем методы Log
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateTheme_shouldSaveThemeToDataStore() =
        runTest(testDispatcher) {
            // Given
            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)
            every { mockDataStore.useDynamicColors } returns flowOf(false)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager)
            viewModel.updateTheme(AppTheme.LIGHT)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { mockDataStore.setTheme(AppTheme.LIGHT) }
        }

    @Test
    fun updateTheme_shouldUpdateAllThemes() =
        runTest(testDispatcher) {
            // Given
            val themes =
                listOf(
                    AppTheme.LIGHT,
                    AppTheme.DARK,
                    AppTheme.SYSTEM,
                )

            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)
            every { mockDataStore.useDynamicColors } returns flowOf(false)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager)
            themes.forEach { theme ->
                viewModel.updateTheme(theme)
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            themes.forEach { theme ->
                coVerify { mockDataStore.setTheme(theme) }
            }
        }

    @Test
    fun updateIcon_shouldSaveIconToDataStoreAndCallIconManager() =
        runTest(testDispatcher) {
            // Given
            every { mockIconManager.changeIcon(any()) } just Runs
            coEvery { mockDataStore.setIcon(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)
            every { mockDataStore.useDynamicColors } returns flowOf(false)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager)
            viewModel.updateIcon(AppIcon.ICON_2)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { mockDataStore.setIcon(AppIcon.ICON_2) }
            verify(atLeast = 1) { mockIconManager.changeIcon(AppIcon.ICON_2) }
        }

    @Test
    fun updateIcon_shouldUpdateAllIcons() =
        runTest(testDispatcher) {
            // Given
            every { mockIconManager.changeIcon(any()) } just Runs
            val icons =
                listOf(
                    AppIcon.DEFAULT,
                    AppIcon.ICON_2,
                    AppIcon.ICON_3,
                    AppIcon.ICON_4,
                    AppIcon.ICON_5,
                    AppIcon.ICON_6,
                    AppIcon.ICON_7,
                    AppIcon.ICON_8,
                    AppIcon.ICON_9,
                    AppIcon.ICON_10,
                    AppIcon.ICON_11,
                )

            coEvery { mockDataStore.setIcon(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)
            every { mockDataStore.useDynamicColors } returns flowOf(false)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager)
            icons.forEach { icon ->
                viewModel.updateIcon(icon)
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            icons.forEach { icon ->
                coVerify { mockDataStore.setIcon(icon) }
                verify(atLeast = 1) { mockIconManager.changeIcon(icon) }
            }
        }

    @Test
    fun updateTheme_shouldCallSetThemeForLight() =
        runTest(testDispatcher) {
            // Given
            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)
            every { mockDataStore.useDynamicColors } returns flowOf(false)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager)
            viewModel.updateTheme(AppTheme.LIGHT)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { mockDataStore.setTheme(AppTheme.LIGHT) }
        }

    @Test
    fun updateTheme_shouldCallSetThemeForDark() =
        runTest(testDispatcher) {
            // Given
            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)
            every { mockDataStore.useDynamicColors } returns flowOf(false)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager)
            viewModel.updateTheme(AppTheme.DARK)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { mockDataStore.setTheme(AppTheme.DARK) }
        }

    @Test
    fun updateTheme_shouldCallSetThemeForSystem() =
        runTest(testDispatcher) {
            // Given
            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)
            every { mockDataStore.useDynamicColors } returns flowOf(false)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager)
            viewModel.updateTheme(AppTheme.SYSTEM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { mockDataStore.setTheme(AppTheme.SYSTEM) }
        }

    @Test
    fun updateDynamicColors_shouldSaveSettingToDataStore() =
        runTest(testDispatcher) {
            // Given
            coEvery { mockDataStore.setUseDynamicColors(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)
            every { mockDataStore.useDynamicColors } returns flowOf(false)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager)
            viewModel.updateDynamicColors(true)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { mockDataStore.setUseDynamicColors(true) }
        }

    @Test
    fun updateMethods_shouldWorkCorrectly() =
        runTest(testDispatcher) {
            // Given
            every { mockIconManager.changeIcon(any()) } just Runs
            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.setIcon(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)
            every { mockDataStore.useDynamicColors } returns flowOf(false)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager)
            viewModel.updateTheme(AppTheme.LIGHT)
            viewModel.updateIcon(AppIcon.ICON_3)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { mockDataStore.setTheme(AppTheme.LIGHT) }
            coVerify { mockDataStore.setIcon(AppIcon.ICON_3) }
            verify(atLeast = 1) { mockIconManager.changeIcon(AppIcon.ICON_3) }
        }

    @Test
    fun updateIcon_shouldSaveIconToDataStore() =
        runTest(testDispatcher) {
            // Given
            every { mockIconManager.changeIcon(any()) } just Runs
            coEvery { mockDataStore.setIcon(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)
            every { mockDataStore.useDynamicColors } returns flowOf(false)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager)
            viewModel.updateIcon(AppIcon.ICON_2)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { mockDataStore.setIcon(AppIcon.ICON_2) }
            verify(atLeast = 1) { mockIconManager.changeIcon(any()) }
        }
}
