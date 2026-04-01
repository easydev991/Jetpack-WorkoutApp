package com.swparks.screenshots

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.swparks.MainActivity
import com.swparks.ui.testtags.ScreenshotTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

@RunWith(AndroidJUnit4::class)
class WorkoutAppScreenshotsTest {
    private companion object {
        private const val PARK_MAP_TEST_TAG = "park_map"
    }

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @get:Rule(order = 1)
    val localeTestRule = LocaleTestRule()

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        grantLocationPermissions()
    }

    @Test
    fun testMakeScreenshots() {
        checkMap()
        checkParks()
        checkParkDetails()
        checkEvents()
        checkEventDetails()
        checkProfile()
    }

    private fun checkMap() {
        openTopLevelTab(ScreenshotTestTags.BOTTOM_NAV_PARKS)
        openParksTab(isMap = true)
        waitForTag(PARK_MAP_TEST_TAG, timeoutMillis = 20_000)
        pauseForUi(4_000)
        repeat(2) {
            clickByTag(ScreenshotTestTags.MAP_MY_LOCATION_FAB)
            pauseForUi(3_000)
        }
        Screengrab.screenshot("0-parksMap")
    }

    private fun checkParks() {
        openTopLevelTab(ScreenshotTestTags.BOTTOM_NAV_PARKS)
        openParksTab(isMap = false)
        waitForTag(parkRowTag(ScreenshotScenarioState.PARK_DETAIL_ID))
        pauseForUi(6_000)
        Screengrab.screenshot("1-parksList")
    }

    private fun checkParkDetails() {
        openParksTab(isMap = false)
        clickByTag(parkRowTag(ScreenshotScenarioState.PARK_DETAIL_ID))
        waitForTag(ScreenshotTestTags.PARK_DETAIL_SCREEN)
        pauseForUi(8_000)
        Screengrab.screenshot("2-parkDetails")
        androidx.test.espresso.Espresso.pressBack()
        waitForTag(parkRowTag(ScreenshotScenarioState.PARK_DETAIL_ID))
    }

    private fun checkEvents() {
        openTopLevelTab(ScreenshotTestTags.BOTTOM_NAV_EVENTS)
        waitForTag(eventRowTag(ScreenshotScenarioState.EVENT_DETAIL_ID), timeoutMillis = 20_000)
        tapNeutralArea()
        clearCurrentFocus()
        pauseForUi(3_000)
        Screengrab.screenshot("3-pastEvents")
        pauseForUi(3_000)
    }

    private fun checkEventDetails() {
        pauseForUi(1_200)
        clickByTag(eventRowTag(ScreenshotScenarioState.EVENT_DETAIL_ID))
        waitForTag(ScreenshotTestTags.EVENT_DETAIL_SCREEN)
        pauseForUi(6_000)
        Screengrab.screenshot("4-eventDetails")
        androidx.test.espresso.Espresso.pressBack()
    }

    private fun checkProfile() {
        openTopLevelTab(ScreenshotTestTags.BOTTOM_NAV_PROFILE)
        waitForTag(ScreenshotTestTags.PROFILE_AUTH_BUTTON)
        clickByTag(ScreenshotTestTags.PROFILE_AUTH_BUTTON)

        waitForTag(ScreenshotTestTags.LOGIN_FIELD)
        composeTestRule
            .onNodeWithTag(ScreenshotTestTags.LOGIN_FIELD, useUnmergedTree = true)
            .performTextInput(DemoData.screenshotLogin)

        waitForTag(ScreenshotTestTags.PASSWORD_FIELD)
        composeTestRule
            .onNodeWithTag(ScreenshotTestTags.PASSWORD_FIELD, useUnmergedTree = true)
            .performTextInput(DemoData.screenshotPassword)

        clickByTag(ScreenshotTestTags.LOGIN_BUTTON)

        waitForTag(ScreenshotTestTags.PROFILE_SEARCH_USERS_BUTTON, timeoutMillis = 20_000)
        clickByTag(ScreenshotTestTags.PROFILE_SEARCH_USERS_BUTTON)

        waitForTag(ScreenshotTestTags.SEARCH_USER_FIELD)
        composeTestRule
            .onNodeWithTag(ScreenshotTestTags.SEARCH_USER_FIELD, useUnmergedTree = true)
            .performTextInput(DemoData.screenshotSearchQuery)
        pauseForUi(800)
        composeTestRule
            .onNodeWithTag(ScreenshotTestTags.SEARCH_USER_FIELD, useUnmergedTree = true)
            .performImeAction()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressEnter()
        pauseForUi(800)

        waitForTag(searchUserRowTag(DemoData.demoSearchUser.id), timeoutMillis = 30_000)
        clickByTag(searchUserRowTag(DemoData.demoSearchUser.id), timeoutMillis = 30_000)
        waitForTag(ScreenshotTestTags.OTHER_USER_PROFILE_SCREEN, timeoutMillis = 20_000)
        pauseForUi(4_000)
        Screengrab.screenshot("5-profile")
    }

    private fun openTopLevelTab(tabTag: String) {
        clickByTag(tabTag)
    }

    private fun openParksTab(isMap: Boolean) {
        clickByTag(
            if (isMap) {
                ScreenshotTestTags.PARKS_TAB_MAP
            } else {
                ScreenshotTestTags.PARKS_TAB_LIST
            }
        )
        composeTestRule.waitForIdle()
    }

    private fun clickByTag(tag: String, timeoutMillis: Long = 10_000) {
        waitForTag(tag, timeoutMillis)
        composeTestRule
            .onAllNodesWithTag(tag, useUnmergedTree = true)
            .onFirst()
            .performClick()
    }

    private fun waitForTag(tag: String, timeoutMillis: Long = 10_000) {
        composeTestRule.waitUntil(timeoutMillis) {
            runCatching {
                composeTestRule
                    .onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }.getOrElse { false }
        }
    }

    private fun pauseForUi(millis: Long) {
        Thread.sleep(millis)
    }

    private fun clearCurrentFocus() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.currentFocus?.clearFocus()
        }
        composeTestRule.waitForIdle()
    }

    private fun tapNeutralArea() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val x = (device.displayWidth * 0.5f).toInt()
        val y = (device.displayHeight * 0.12f).toInt().coerceAtLeast(80)
        device.click(x, y)
        composeTestRule.waitForIdle()
    }

    private fun grantLocationPermissions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val packageNames = setOf(
            "com.swparks",
            instrumentation.targetContext.packageName,
            instrumentation.context.packageName
        )
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        packageNames.forEach { packageName ->
            permissions.forEach { permission ->
                runCatching {
                    device.executeShellCommand("pm grant $packageName $permission")
                }
            }
            runCatching {
                device.executeShellCommand(
                    "appops set $packageName ACCESS_FINE_LOCATION allow"
                )
            }
            runCatching {
                device.executeShellCommand(
                    "appops set $packageName ACCESS_COARSE_LOCATION allow"
                )
            }
        }
    }

    private fun parkRowTag(parkId: Long): String = "${ScreenshotTestTags.PARK_ROW_PREFIX}$parkId"
    private fun eventRowTag(eventId: Long): String = "${ScreenshotTestTags.EVENT_ROW_PREFIX}$eventId"
    private fun searchUserRowTag(userId: Long): String =
        "${ScreenshotTestTags.SEARCH_USER_ROW_PREFIX}$userId"
}
