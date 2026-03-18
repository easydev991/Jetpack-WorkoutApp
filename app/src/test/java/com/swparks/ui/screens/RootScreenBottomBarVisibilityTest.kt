package com.swparks.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootScreenBottomBarVisibilityTest {

    @Test
    fun shouldShowBottomBar_whenCreateEventRoute_thenReturnFalse() {
        assertFalse(shouldShowBottomBar("create_event"))
    }

    @Test
    fun shouldShowBottomBar_whenEditEventRouteWithArgs_thenReturnFalse() {
        assertFalse(shouldShowBottomBar("edit_event/42?source=events"))
    }

    @Test
    fun shouldShowBottomBar_whenCreateEventForParkRouteWithArgs_thenReturnFalse() {
        assertFalse(shouldShowBottomBar("create_event_for_park/10?parkName=Test&source=parks"))
    }

    @Test
    fun shouldShowBottomBar_whenSelectParkForEventRouteWithArgs_thenReturnFalse() {
        assertFalse(shouldShowBottomBar("select_park_for_event/99?source=events"))
    }

    @Test
    fun shouldShowBottomBar_whenTopLevelRoute_thenReturnTrue() {
        assertTrue(shouldShowBottomBar("events"))
    }

    @Test
    fun shouldShowBottomBar_whenUnknownRoute_thenReturnTrue() {
        assertTrue(shouldShowBottomBar("some_new_route/123"))
    }

    @Test
    fun shouldShowBottomBar_whenFriendsForDialogRoute_thenReturnFalse() {
        assertFalse(shouldShowBottomBar("friends_for_dialog"))
    }

    @Test
    fun shouldShowBottomBar_whenMyFriendsRoute_thenReturnTrue() {
        assertTrue(shouldShowBottomBar("my_friends"))
    }
}
