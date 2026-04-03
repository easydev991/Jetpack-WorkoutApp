package com.swparks.ui.screens

import androidx.lifecycle.Lifecycle
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppForegroundSyncHandlerTest {
    @Test
    fun onStart_callsSyncCountries() =
        runTest {
            var callCount = 0
            val observer =
                createAppForegroundSyncObserver(this) {
                    callCount += 1
                    Result.success(Unit)
                }

            observer.onStateChanged(mockk(relaxed = true), Lifecycle.Event.ON_START)
            advanceUntilIdle()

            assertEquals(1, callCount)
        }

    @Test
    fun otherLifecycleEvents_doNotCallSyncCountries() =
        runTest {
            var callCount = 0
            val observer =
                createAppForegroundSyncObserver(this) {
                    callCount += 1
                    Result.success(Unit)
                }

            observer.onStateChanged(mockk(relaxed = true), Lifecycle.Event.ON_STOP)
            observer.onStateChanged(mockk(relaxed = true), Lifecycle.Event.ON_PAUSE)
            observer.onStateChanged(mockk(relaxed = true), Lifecycle.Event.ON_RESUME)
            observer.onStateChanged(mockk(relaxed = true), Lifecycle.Event.ON_DESTROY)
            advanceUntilIdle()

            assertEquals(0, callCount)
        }

    @Test
    fun syncError_doesNotPropagateFromObserver() =
        runTest {
            var callCount = 0
            val observer =
                createAppForegroundSyncObserver(this) {
                    callCount += 1
                    error("boom")
                }

            observer.onStateChanged(mockk(relaxed = true), Lifecycle.Event.ON_START)
            advanceUntilIdle()

            assertEquals(1, callCount)
        }

    @Test
    fun repeatedOnStart_callsSyncCountriesEachTime_andDelegatesNeedUpdateToUseCase() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        var callCount = 0
        val observer =
            createAppForegroundSyncObserver(scope) {
                callCount += 1
                Result.success(Unit)
            }

        observer.onStateChanged(mockk(relaxed = true), Lifecycle.Event.ON_START)
        observer.onStateChanged(mockk(relaxed = true), Lifecycle.Event.ON_START)
        scope.advanceUntilIdle()

        assertEquals(2, callCount)
    }
}
