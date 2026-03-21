package com.swparks.ui.screens

import android.os.Bundle
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.navigation.AppState
import com.swparks.navigation.BottomNavigationBar
import com.swparks.navigation.Screen
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Инструментальные тесты для проверки стабильности BottomNavigation при source-driven навигации.
 *
 * Гарантирует инвариант: выбранная вкладка BottomNavigation не меняется на дочерних экранах.
 * Контекст вкладки определяется через параметр source и должен сохраняться сквозь переходы.
 *
 * Тесты проверяют, что BottomNavigationBar корректно отображает выбранную вкладку
 * на основе currentTopLevelDestination в AppState.
 */
@RunWith(AndroidJUnit4::class)
class RootScreenBottomNavSourceFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: NavHostController
    private lateinit var appState: AppState
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        io.mockk.unmockkAll()
    }

    @Test
    fun profileFlow_whenNavigatingToUserParks_thenProfileTabIsSelected() {
        composeTestRule.setContent {
            navController = rememberNavController()
            appState = AppState(navController)

            androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column {
                    Text("Test Screen")
                    BottomNavigationBar(appState = appState)
                }
            }
        }

        val arguments = Bundle().apply {
            putString("userId", "1")
            putString("source", "profile")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.UserParks.route, arguments)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Profile.route
        }

        val profileTabText = context.getString(R.string.profile)
        composeTestRule
            .onNodeWithText(profileTabText)
            .assertIsSelected()
    }

    @Test
    fun profileFlow_whenNavigatingToParkDetail_thenProfileTabIsSelected() {
        composeTestRule.setContent {
            navController = rememberNavController()
            appState = AppState(navController)

            androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column {
                    Text("Test Screen")
                    BottomNavigationBar(appState = appState)
                }
            }
        }

        val arguments = Bundle().apply {
            putString("parkId", "1")
            putString("source", "profile")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.ParkDetail.route, arguments)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Profile.route
        }

        val profileTabText = context.getString(R.string.profile)
        composeTestRule
            .onNodeWithText(profileTabText)
            .assertIsSelected()
    }

    @Test
    fun profileFlow_whenNavigatingChain_thenProfileTabIsPreserved() {
        composeTestRule.setContent {
            navController = rememberNavController()
            appState = AppState(navController)

            androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column {
                    Text("Test Screen")
                    BottomNavigationBar(appState = appState)
                }
            }
        }

        val userParksArgs = Bundle().apply {
            putString("userId", "1")
            putString("source", "profile")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.UserParks.route, userParksArgs)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Profile.route
        }

        val parkDetailArgs = Bundle().apply {
            putString("parkId", "1")
            putString("source", "profile")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.ParkDetail.route, parkDetailArgs)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Profile.route
        }

        val profileTabText = context.getString(R.string.profile)
        composeTestRule
            .onNodeWithText(profileTabText)
            .assertIsSelected()
    }

    @Test
    fun eventsFlow_whenNavigatingToJournalsList_thenEventsTabIsSelected() {
        composeTestRule.setContent {
            navController = rememberNavController()
            appState = AppState(navController)

            androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column {
                    Text("Test Screen")
                    BottomNavigationBar(appState = appState)
                }
            }
        }

        val arguments = Bundle().apply {
            putString("userId", "1")
            putString("source", "events")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.JournalsList.route, arguments)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Events.route
        }

        val eventsTabText = context.getString(R.string.events)
        composeTestRule
            .onNodeWithText(eventsTabText)
            .assertIsSelected()
    }

    @Test
    fun eventsFlow_whenNavigatingToJournalEntries_thenEventsTabIsSelected() {
        composeTestRule.setContent {
            navController = rememberNavController()
            appState = AppState(navController)

            androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column {
                    Text("Test Screen")
                    BottomNavigationBar(appState = appState)
                }
            }
        }

        val arguments = Bundle().apply {
            putString("journalId", "1")
            putString("userId", "1")
            putString("title", "Test Journal")
            putString("viewAccess", "public")
            putString("commentAccess", "public")
            putString("source", "events")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.JournalEntries.route, arguments)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Events.route
        }

        val eventsTabText = context.getString(R.string.events)
        composeTestRule
            .onNodeWithText(eventsTabText)
            .assertIsSelected()
    }

    @Test
    fun eventsFlow_whenNavigatingJournalsChain_thenEventsTabIsPreserved() {
        composeTestRule.setContent {
            navController = rememberNavController()
            appState = AppState(navController)

            androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column {
                    Text("Test Screen")
                    BottomNavigationBar(appState = appState)
                }
            }
        }

        val journalsListArgs = Bundle().apply {
            putString("userId", "1")
            putString("source", "events")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.JournalsList.route, journalsListArgs)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Events.route
        }

        val journalEntriesArgs = Bundle().apply {
            putString("journalId", "1")
            putString("userId", "1")
            putString("title", "Test Journal")
            putString("viewAccess", "public")
            putString("commentAccess", "public")
            putString("source", "events")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.JournalEntries.route, journalEntriesArgs)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Events.route
        }

        val eventsTabText = context.getString(R.string.events)
        composeTestRule
            .onNodeWithText(eventsTabText)
            .assertIsSelected()
    }

    @Test
    fun parksFlow_whenNavigatingToParkDetail_thenParksTabIsSelected() {
        composeTestRule.setContent {
            navController = rememberNavController()
            appState = AppState(navController)

            androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column {
                    Text("Test Screen")
                    BottomNavigationBar(appState = appState)
                }
            }
        }

        val arguments = Bundle().apply {
            putString("parkId", "1")
            putString("source", "parks")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.ParkDetail.route, arguments)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Parks.route
        }

        val parksTabText = context.getString(R.string.parks)
        composeTestRule
            .onNodeWithText(parksTabText)
            .assertIsSelected()
    }

    @Test
    fun messagesFlow_whenNavigatingToOtherUserProfile_thenMessagesTabIsSelected() {
        composeTestRule.setContent {
            navController = rememberNavController()
            appState = AppState(navController)

            androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column {
                    Text("Test Screen")
                    BottomNavigationBar(appState = appState)
                }
            }
        }

        val arguments = Bundle().apply {
            putString("userId", "1")
            putString("source", "messages")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.OtherUserProfile.route, arguments)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Messages.route
        }

        val messagesTabText = context.getString(R.string.messages)
        composeTestRule
            .onNodeWithText(messagesTabText)
            .assertIsSelected()
    }

    @Test
    fun complexChain_profileToUserParksToParkDetailToOtherUserProfile_preservesProfileTab() {
        composeTestRule.setContent {
            navController = rememberNavController()
            appState = AppState(navController)

            androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column {
                    Text("Test Screen")
                    BottomNavigationBar(appState = appState)
                }
            }
        }

        val userParksArgs = Bundle().apply {
            putString("userId", "1")
            putString("source", "profile")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.UserParks.route, userParksArgs)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Profile.route
        }

        val parkDetailArgs = Bundle().apply {
            putString("parkId", "1")
            putString("source", "profile")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.ParkDetail.route, parkDetailArgs)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Profile.route
        }

        val otherUserProfileArgs = Bundle().apply {
            putString("userId", "2")
            putString("source", "profile")
        }

        composeTestRule.runOnIdle {
            appState.onDestinationChanged(Screen.OtherUserProfile.route, otherUserProfileArgs)
        }

        composeTestRule.waitUntil(5000) {
            appState.currentTopLevelDestination?.route == Screen.Profile.route
        }

        val profileTabText = context.getString(R.string.profile)
        composeTestRule
            .onNodeWithText(profileTabText)
            .assertIsSelected()
    }
}
