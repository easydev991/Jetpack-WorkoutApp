package com.swparks.navigation

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit тесты для Destinations.kt.
 * Проверяют функциональность:
 * - getScreenBySource() - маппинг source параметра в Screen
 * - Screen.findParentTab() - определение родительской вкладки по route и arguments
 */
@RunWith(RobolectricTestRunner::class)
class DestinationsTest {

    // ==================== Тесты getScreenBySource ====================

    @Test
    fun getScreenBySource_whenParks_thenReturnParksScreen() {
        val result = getScreenBySource("parks", Screen.Profile)
        assertEquals(Screen.Parks, result)
    }

    @Test
    fun getScreenBySource_whenEvents_thenReturnEventsScreen() {
        val result = getScreenBySource("events", Screen.Profile)
        assertEquals(Screen.Events, result)
    }

    @Test
    fun getScreenBySource_whenMessages_thenReturnMessagesScreen() {
        val result = getScreenBySource("messages", Screen.Profile)
        assertEquals(Screen.Messages, result)
    }

    @Test
    fun getScreenBySource_whenProfile_thenReturnProfileScreen() {
        val result = getScreenBySource("profile", Screen.Parks)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun getScreenBySource_whenMore_thenReturnMoreScreen() {
        val result = getScreenBySource("more", Screen.Profile)
        assertEquals(Screen.More, result)
    }

    @Test
    fun getScreenBySource_whenLegacyPark_thenReturnParksScreen() {
        val result = getScreenBySource("park", Screen.Profile)
        assertEquals(Screen.Parks, result)
    }

    @Test
    fun getScreenBySource_whenUnknown_thenReturnDefault() {
        val result = getScreenBySource("unknown_source", Screen.Profile)
        assertEquals("При неизвестном source должен возвращаться default", Screen.Profile, result)
    }

    @Test
    fun getScreenBySource_whenEmpty_thenReturnDefault() {
        val result = getScreenBySource("", Screen.Events)
        assertEquals("При пустом source должен возвращаться default", Screen.Events, result)
    }

    @Test
    fun getScreenBySource_whenAllValidSources_thenReturnCorrectScreens() {
        val mappings = listOf(
            "parks" to Screen.Parks,
            "events" to Screen.Events,
            "messages" to Screen.Messages,
            "profile" to Screen.Profile,
            "more" to Screen.More
        )

        mappings.forEach { (source, expectedScreen) ->
            val result = getScreenBySource(source, Screen.Profile)
            assertEquals(
                "getScreenBySource('$source') должен возвращать $expectedScreen",
                expectedScreen,
                result
            )
        }
    }

    // ==================== Тесты Screen.findParentTab (companion object) ====================

    @Test
    fun findParentTab_whenTopLevelRoute_thenReturnNull() {
        // Верхнеуровневые экраны не имеют parentTab
        val topLevelRoutes = listOf("parks", "events", "messages", "profile", "more")

        topLevelRoutes.forEach { route ->
            val result = Screen.findParentTab(route)
            assertEquals(
                "Для верхнеуровневого маршрута $route parentTab должен быть null",
                null,
                result
            )
        }
    }

    // ==================== Тесты OtherUserProfile с source ====================

    @Test
    fun findParentTab_whenOtherUserProfileFromProfile_thenReturnProfile() {
        val arguments = Bundle().apply { putString("source", "profile") }
        val result = Screen.findParentTab("other_user_profile/123", arguments)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenOtherUserProfileFromMessages_thenReturnMessages() {
        val arguments = Bundle().apply { putString("source", "messages") }
        val result = Screen.findParentTab("other_user_profile/456", arguments)
        assertEquals(Screen.Messages, result)
    }

    @Test
    fun findParentTab_whenOtherUserProfileFromParks_thenReturnParks() {
        val arguments = Bundle().apply { putString("source", "parks") }
        val result = Screen.findParentTab("other_user_profile/789", arguments)
        assertEquals(Screen.Parks, result)
    }

    @Test
    fun findParentTab_whenOtherUserProfileFromLegacyPark_thenReturnParks() {
        val arguments = Bundle().apply { putString("source", "park") }
        val result = Screen.findParentTab("other_user_profile/790", arguments)
        assertEquals(Screen.Parks, result)
    }

    @Test
    fun findParentTab_whenOtherUserProfileFromEvents_thenReturnEvents() {
        val arguments = Bundle().apply { putString("source", "events") }
        val result = Screen.findParentTab("other_user_profile/111", arguments)
        assertEquals(Screen.Events, result)
    }

    @Test
    fun findParentTab_whenOtherUserProfileFromMore_thenReturnMore() {
        val arguments = Bundle().apply { putString("source", "more") }
        val result = Screen.findParentTab("other_user_profile/222", arguments)
        assertEquals(Screen.More, result)
    }

    @Test
    fun findParentTab_whenOtherUserProfileWithoutSource_thenReturnProfileDefault() {
        val result = Screen.findParentTab("other_user_profile/333", null)
        assertEquals(
            "При отсутствии source должен возвращаться default Profile",
            Screen.Profile,
            result
        )
    }

    // ==================== Тесты UserSearch с source ====================

    @Test
    fun findParentTab_whenUserSearchFromMessages_thenReturnMessages() {
        val arguments = Bundle().apply { putString("source", "messages") }
        val result = Screen.findParentTab("user_search", arguments)
        assertEquals(Screen.Messages, result)
    }

    @Test
    fun findParentTab_whenUserSearchFromProfile_thenReturnProfile() {
        val arguments = Bundle().apply { putString("source", "profile") }
        val result = Screen.findParentTab("user_search", arguments)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenUserSearchWithoutSource_thenReturnMessagesDefault() {
        val result = Screen.findParentTab("user_search", null)
        assertEquals(
            "При отсутствии source должен возвращаться default Messages",
            Screen.Messages,
            result
        )
    }

    // ==================== Тесты UserFriends с source ====================

    @Test
    fun findParentTab_whenUserFriendsFromProfile_thenReturnProfile() {
        val arguments = Bundle().apply { putString("source", "profile") }
        val result = Screen.findParentTab("user_friends/123", arguments)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenUserFriendsFromMessages_thenReturnMessages() {
        val arguments = Bundle().apply { putString("source", "messages") }
        val result = Screen.findParentTab("user_friends/456", arguments)
        assertEquals(Screen.Messages, result)
    }

    @Test
    fun findParentTab_whenUserFriendsWithoutSource_thenReturnProfileDefault() {
        val result = Screen.findParentTab("user_friends/789", null)
        assertEquals(Screen.Profile, result)
    }

    // ==================== Тесты UserParks с source ====================

    @Test
    fun findParentTab_whenUserParksFromParks_thenReturnParks() {
        val arguments = Bundle().apply { putString("source", "parks") }
        val result = Screen.findParentTab("user_parks/123", arguments)
        assertEquals(Screen.Parks, result)
    }

    @Test
    fun findParentTab_whenUserParksFromProfile_thenReturnProfile() {
        val arguments = Bundle().apply { putString("source", "profile") }
        val result = Screen.findParentTab("user_parks/456", arguments)
        assertEquals(Screen.Profile, result)
    }

    // ==================== Тесты JournalsList с source ====================

    @Test
    fun findParentTab_whenJournalsListFromProfile_thenReturnProfile() {
        val arguments = Bundle().apply { putString("source", "profile") }
        val result = Screen.findParentTab("journals_list/123", arguments)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenJournalsListFromEvents_thenReturnEvents() {
        val arguments = Bundle().apply { putString("source", "events") }
        val result = Screen.findParentTab("journals_list/456", arguments)
        assertEquals(Screen.Events, result)
    }

    // ==================== Тесты JournalEntries с source ====================

    @Test
    fun findParentTab_whenJournalEntriesFromProfile_thenReturnProfile() {
        val arguments = Bundle().apply { putString("source", "profile") }
        val result = Screen.findParentTab("journal_entries/123", arguments)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenJournalEntriesFromMessages_thenReturnMessages() {
        val arguments = Bundle().apply { putString("source", "messages") }
        val result = Screen.findParentTab("journal_entries/456", arguments)
        assertEquals(Screen.Messages, result)
    }

    // ==================== Тесты ParkDetail с source ====================

    @Test
    fun findParentTab_whenParkDetailFromParks_thenReturnParks() {
        val arguments = Bundle().apply { putString("source", "parks") }
        val result = Screen.findParentTab("park_detail/123", arguments)
        assertEquals(Screen.Parks, result)
    }

    @Test
    fun findParentTab_whenParkDetailFromProfile_thenReturnProfile() {
        val arguments = Bundle().apply { putString("source", "profile") }
        val result = Screen.findParentTab("park_detail/456", arguments)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenParkDetailWithoutSource_thenReturnParksDefault() {
        val result = Screen.findParentTab("park_detail/789", null)
        assertEquals(Screen.Parks, result)
    }

    // ==================== Тесты EventDetail с source ====================

    @Test
    fun findParentTab_whenEventDetailFromEvents_thenReturnEvents() {
        val arguments = Bundle().apply { putString("source", "events") }
        val result = Screen.findParentTab("event_detail/123", arguments)
        assertEquals(Screen.Events, result)
    }

    @Test
    fun findParentTab_whenEventDetailFromProfile_thenReturnProfile() {
        val arguments = Bundle().apply { putString("source", "profile") }
        val result = Screen.findParentTab("event_detail/456", arguments)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenEventDetailWithoutSource_thenReturnEventsDefault() {
        val result = Screen.findParentTab("event_detail/789", null)
        assertEquals(Screen.Events, result)
    }

    // ==================== Тесты Chat с source ====================

    @Test
    fun findParentTab_whenChatFromMessages_thenReturnMessages() {
        val arguments = Bundle().apply { putString("source", "messages") }
        val result = Screen.findParentTab("chat/123", arguments)
        assertEquals(Screen.Messages, result)
    }

    @Test
    fun findParentTab_whenChatFromProfile_thenReturnProfile() {
        val arguments = Bundle().apply { putString("source", "profile") }
        val result = Screen.findParentTab("chat/456", arguments)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenChatWithoutSource_thenReturnMessagesDefault() {
        val result = Screen.findParentTab("chat/789", null)
        assertEquals(Screen.Messages, result)
    }

    // ==================== Тесты экранов без source (static parentTab) ====================

    @Test
    fun findParentTab_whenEditProfile_thenReturnProfile() {
        val result = Screen.findParentTab("edit_profile", null)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenBlacklist_thenReturnProfile() {
        val result = Screen.findParentTab("blacklist", null)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenMyFriends_thenReturnProfile() {
        val result = Screen.findParentTab("my_friends", null)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenCreatePark_thenReturnParks() {
        val result = Screen.findParentTab("create_park", null)
        assertEquals(Screen.Parks, result)
    }

    @Test
    fun findParentTab_whenParkFilter_thenReturnParks() {
        val result = Screen.findParentTab("park_filter", null)
        assertEquals(Screen.Parks, result)
    }

    @Test
    fun findParentTab_whenCreateEvent_thenReturnEvents() {
        val result = Screen.findParentTab("create_event", null)
        assertEquals(Screen.Events, result)
    }

    @Test
    fun findParentTab_whenFriends_thenReturnMessages() {
        val result = Screen.findParentTab("friends", null)
        assertEquals(Screen.Messages, result)
    }

    @Test
    fun findParentTab_whenThemeIcon_thenReturnMore() {
        val result = Screen.findParentTab("theme_icon", null)
        assertEquals(Screen.More, result)
    }

    // ==================== Тесты сложных маршрутов с query параметрами ====================

    @Test
    fun findParentTab_whenRouteWithQueryParams_thenExtractsBaseRoute() {
        // UserSearch может быть вызван с query параметрами
        val arguments = Bundle().apply { putString("source", "profile") }
        val result = Screen.findParentTab("user_search?source=profile", arguments)
        assertEquals(Screen.Profile, result)
    }

    @Test
    fun findParentTab_whenJournalEntriesRoute_thenHandlesComplexRoute() {
        val arguments = Bundle().apply {
            putString("source", "events")
            putString("userId", "123")
            putString("journalTitle", "My Journal")
        }
        val result = Screen.findParentTab(
            "journal_entries/456?userId=123&title=My%20Journal&source=events",
            arguments
        )
        assertEquals(Screen.Events, result)
    }

    // ==================== Edge cases ====================

    @Test
    fun findParentTab_whenUnknownRoute_thenReturnNull() {
        val result = Screen.findParentTab("unknown_route", null)
        assertEquals("Для неизвестного маршрута должен возвращаться null", null, result)
    }

    @Test
    fun findParentTab_whenEmptyRoute_thenReturnNull() {
        val result = Screen.findParentTab("", null)
        assertEquals("Для пустого маршрута должен возвращаться null", null, result)
    }

    @Test
    fun findParentTab_whenAllSourceValuesWithAllScreens_thenCorrectlyMaps() {
        // Проверяем все комбинации source для экранов с динамическим parentTab
        val screensWithSource = listOf(
            "other_user_profile",
            "user_search",
            "user_friends",
            "user_parks",
            "user_training_parks",
            "journals_list",
            "journal_entries",
            "park_detail",
            "edit_park",
            "create_event_for_park",
            "park_route",
            "add_park_comment",
            "park_trainees",
            "park_gallery",
            "event_detail",
            "edit_event",
            "event_participants",
            "event_gallery",
            "add_event_comment",
            "chat"
        )

        val sources = listOf(
            "parks" to Screen.Parks,
            "events" to Screen.Events,
            "messages" to Screen.Messages,
            "profile" to Screen.Profile,
            "more" to Screen.More
        )

        screensWithSource.forEach { baseRoute ->
            sources.forEach { (source, expectedScreen) ->
                val arguments = Bundle().apply { putString("source", source) }
                val route = if (baseRoute == "user_search") {
                    baseRoute
                } else {
                    "$baseRoute/123"
                }
                val result = Screen.findParentTab(route, arguments)
                assertEquals(
                    "Route '$route' с source='$source' должен возвращать $expectedScreen",
                    expectedScreen,
                    result
                )
            }
        }
    }
}
