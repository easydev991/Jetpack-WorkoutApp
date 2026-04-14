package com.swparks.navigation

import android.util.Log
import androidx.navigation.NavHostController
import com.swparks.analytics.AnalyticsService
import com.swparks.data.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit тесты для AppState.
 * Проверяют функциональность:
 * - Состояния авторизации и обновления текущего пользователя
 * - Навигации и определения активной вкладки (включая parentTab для дочерних экранов)
 */
class AppStateTest {
    private lateinit var navController: NavHostController
    private lateinit var analyticsService: AnalyticsService
    private lateinit var appState: AppState

    // Вспомогательный метод для создания тестового пользователя
    private fun createTestUser(id: Long = 1L) =
        User(
            id = id,
            name = "testuser",
            image = "https://example.com/image.jpg",
            cityID = null,
            countryID = null,
            birthDate = null,
            email = null,
            fullName = null,
            genderCode = null,
            friendRequestCount = null,
            friendsCount = null,
            parksCount = null,
            addedParks = null,
            journalCount = null
        )

    @Before
    fun setup() {
        // Мокаем Android Log для тестов навигации
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        navController = mockk(relaxed = true)
        analyticsService = AnalyticsService(emptyList(), mockk(relaxed = true))
        appState = AppState(navController, analyticsService)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun currentUser_whenInitialized_thenReturnsNull() {
        // Given - AppState только что создан

        // When
        val currentUser = appState.currentUser

        // Then
        assertNull("Текущий пользователь должен быть null после инициализации", currentUser)
    }

    @Test
    fun isAuthorized_whenCurrentUserIsNull_thenReturnsFalse() {
        // Given - AppState только что создан, currentUser = null

        // When
        val isAuthorized = appState.isAuthorized

        // Then
        assertFalse("Пользователь не должен быть авторизован при null currentUser", isAuthorized)
    }

    @Test
    fun updateCurrentUser_whenCalledWithUser_thenUpdatesCurrentUser() {
        // Given
        val testUser = createTestUser()

        // When
        appState.updateCurrentUser(testUser)

        // Then
        val currentUser = appState.currentUser
        assertTrue("Текущий пользователь должен быть обновлен", currentUser != null)
        assertTrue("ID пользователя должен совпадать", currentUser?.id == testUser.id)
        assertTrue("Имя пользователя должно совпадать", currentUser?.name == testUser.name)
    }

    @Test
    fun updateCurrentUser_whenCalledWithUser_thenReturnsTrueIsAuthorized() {
        // Given
        val testUser = createTestUser()

        // When
        appState.updateCurrentUser(testUser)

        // Then
        assertTrue("Пользователь должен быть авторизован после обновления", appState.isAuthorized)
    }

    @Test
    fun updateCurrentUser_whenCalledWithNull_thenClearsCurrentUser() {
        // Given
        val testUser = createTestUser()
        appState.updateCurrentUser(testUser)
        assertTrue(
            "Предварительная проверка: пользователь должен быть авторизован",
            appState.isAuthorized
        )

        // When
        appState.updateCurrentUser(null)

        // Then
        assertNull("Текущий пользователь должен быть null после очистки", appState.currentUser)
        assertFalse("Пользователь не должен быть авторизован после очистки", appState.isAuthorized)
    }

    @Test
    fun updateCurrentUser_whenCalledMultipleTimes_thenReflectsLatestValue() {
        // Given
        val firstUser = createTestUser(id = 1L)
        val secondUser = createTestUser(id = 2L)
        val thirdUser = createTestUser(id = 3L)

        // When - обновляем пользователя несколько раз
        appState.updateCurrentUser(firstUser)
        val afterFirstUpdate = appState.currentUser

        appState.updateCurrentUser(secondUser)
        val afterSecondUpdate = appState.currentUser

        appState.updateCurrentUser(thirdUser)
        val afterThirdUpdate = appState.currentUser

        // Then - проверяем что каждый раз сохраняется последнее значение
        assertTrue("После первого обновления ID должен быть 1", afterFirstUpdate?.id == 1L)
        assertTrue("После второго обновления ID должен быть 2", afterSecondUpdate?.id == 2L)
        assertTrue("После третьего обновления ID должен быть 3", afterThirdUpdate?.id == 3L)
        assertTrue(
            "Финальный currentUser должен быть третьим пользователем",
            appState.currentUser?.id == 3L
        )
    }

    @Test
    fun isAuthorized_whenUserSet_thenReturnsTrue() {
        // Given
        val testUser = createTestUser()

        // When
        appState.updateCurrentUser(testUser)

        // Then
        assertTrue(
            "isAuthorized должен возвращать true при наличии пользователя",
            appState.isAuthorized
        )
    }

    @Test
    fun updateCurrentUser_whenCalledWithSameUser_multipleTimes_thenRemainsConsistent() {
        // Given
        val testUser = createTestUser(id = 5L)

        // When - обновляем одним и тем же пользователем несколько раз
        appState.updateCurrentUser(testUser)
        val firstValue = appState.currentUser

        appState.updateCurrentUser(testUser)
        val secondValue = appState.currentUser

        appState.updateCurrentUser(testUser)
        val thirdValue = appState.currentUser

        // Then - все значения должны быть одинаковыми
        assertTrue("Первое обновление должно сохранить пользователя", firstValue?.id == 5L)
        assertTrue("Второе обновление должно сохранить пользователя", secondValue?.id == 5L)
        assertTrue("Третье обновление должно сохранить пользователя", thirdValue?.id == 5L)
        assertTrue(
            "Пользователь должен быть авторизован после всех обновлений",
            appState.isAuthorized
        )
    }

    @Test
    fun updateCurrentUser_whenCalledWithNull_thenNullThenUser_thenTransitionsCorrectly() {
        // Given
        val testUser = createTestUser()

        // When - последовательность: null -> user -> null -> user
        appState.updateCurrentUser(null)
        val afterFirstNull = appState.currentUser
        val authorizedAfterFirstNull = appState.isAuthorized

        appState.updateCurrentUser(testUser)
        val afterUser = appState.currentUser
        val authorizedAfterUser = appState.isAuthorized

        appState.updateCurrentUser(null)
        val afterSecondNull = appState.currentUser
        val authorizedAfterSecondNull = appState.isAuthorized

        appState.updateCurrentUser(testUser)
        val afterSecondUser = appState.currentUser
        val authorizedAfterSecondUser = appState.isAuthorized

        // Then - проверяем переходы состояний
        assertNull("После первого null currentUser должен быть null", afterFirstNull)
        assertFalse(
            "После первого null пользователь не должен быть авторизован",
            authorizedAfterFirstNull
        )

        assertTrue(
            "После установки пользователя currentUser должен быть не null",
            afterUser != null
        )
        assertTrue("После установки пользователь должен быть авторизован", authorizedAfterUser)

        assertNull("После второго null currentUser должен быть null", afterSecondNull)
        assertFalse(
            "После второго null пользователь не должен быть авторизован",
            authorizedAfterSecondNull
        )

        assertTrue(
            "После повторной установки пользователя currentUser должен быть не null",
            afterSecondUser != null
        )
        assertTrue(
            "После повторной установки пользователь должен быть авторизован",
            authorizedAfterSecondUser
        )
    }

    // ==================== Тесты навигации ====================

    @Test
    fun onDestinationChanged_whenRootScreen_thenUpdatesTopLevelDestination() {
        // When - переход на корневой экран Profile
        appState.onDestinationChanged("profile")

        // Then
        assertEquals(
            "При переходе на корневой экран Profile currentTopLevelDestination должен быть PROFILE",
            TopLevelDestinations.PROFILE,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenChildScreenWithParentTab_thenUpdatesToParentTab() {
        // Given - сначала перейдем на Profile
        appState.onDestinationChanged("profile")
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)

        // When - переход на дочерний экран edit_profile (parentTab = Profile)
        appState.onDestinationChanged("edit_profile")

        // Then - должен определить Profile как активную вкладку
        assertEquals(
            "При переходе на дочерний экран edit_profile currentTopLevelDestination должен остаться PROFILE",
            TopLevelDestinations.PROFILE,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenUnknownRoute_thenDoesNotChangeTopLevelDestination() {
        // Given - сначала установим вкладку Profile
        appState.onDestinationChanged("profile")
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)

        // When - переход на неизвестный маршрут
        appState.onDestinationChanged("unknown_route")

        // Then - currentTopLevelDestination не должен измениться
        assertEquals(
            "При переходе на неизвестный маршрут currentTopLevelDestination должен остаться прежним",
            TopLevelDestinations.PROFILE,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenSwitchingTabs_thenUpdatesTopLevelDestination() {
        // When - переход на Parks
        appState.onDestinationChanged("parks")
        assertEquals(TopLevelDestinations.PARKS, appState.currentTopLevelDestination)

        // When - переход на Events
        appState.onDestinationChanged("events")
        assertEquals(TopLevelDestinations.EVENTS, appState.currentTopLevelDestination)

        // When - переход на Messages
        appState.onDestinationChanged("messages")
        assertEquals(TopLevelDestinations.MESSAGES, appState.currentTopLevelDestination)

        // When - переход на More
        appState.onDestinationChanged("more")
        assertEquals(TopLevelDestinations.MORE, appState.currentTopLevelDestination)

        // When - возврат на Profile
        appState.onDestinationChanged("profile")
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)
    }

    @Test
    fun onDestinationChanged_whenRestoringStackWithChildScreen_thenParentTabIsSelected() {
        // Сценарий: Profile -> EditProfile -> More -> Profile (restoreState восстанавливает стек с EditProfile)

        // Step 1: Открываем Profile
        appState.onDestinationChanged("profile")
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)

        // Step 2: Переходим в EditProfile (дочерний экран)
        appState.onDestinationChanged("edit_profile")
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)

        // Step 3: Переключаемся на More
        appState.onDestinationChanged("more")
        assertEquals(TopLevelDestinations.MORE, appState.currentTopLevelDestination)

        // Step 4: Возвращаемся на Profile с restoreState=true
        // Navigation Component восстанавливает стек и сразу вызывает onDestinationChanged с edit_profile
        appState.onDestinationChanged("edit_profile")

        // Then - Profile должен быть выбран как активная вкладка (благодаря parentTab)
        assertEquals(
            "При восстановлении стека с дочерним экраном edit_profile " +
                "currentTopLevelDestination должен быть PROFILE (родительская вкладка)",
            TopLevelDestinations.PROFILE,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenAllTopLevelDestinations_thenEachIsSelected() {
        // Проверяем, что все 5 вкладок корректно определяются
        val destinations =
            listOf(
                "parks" to TopLevelDestinations.PARKS,
                "events" to TopLevelDestinations.EVENTS,
                "messages" to TopLevelDestinations.MESSAGES,
                "profile" to TopLevelDestinations.PROFILE,
                "more" to TopLevelDestinations.MORE
            )

        destinations.forEach { (route, expectedDestination) ->
            // When
            appState.onDestinationChanged(route)

            // Then
            assertEquals(
                "При переходе на $route currentTopLevelDestination должен быть $expectedDestination",
                expectedDestination,
                appState.currentTopLevelDestination
            )
        }
    }

    @Test
    fun onDestinationChanged_whenChildScreens_thenParentTabIsSelected() {
        // Проверяем различные дочерние экраны и их родительские вкладки
        val childScreens =
            listOf(
                // Дочерние экраны Profile
                "edit_profile" to TopLevelDestinations.PROFILE,
                "user_parks/123" to TopLevelDestinations.PROFILE,
                "my_friends" to TopLevelDestinations.PROFILE,
                "blacklist" to TopLevelDestinations.PROFILE,
                // Дочерние экраны Parks
                "park_detail/456" to TopLevelDestinations.PARKS,
                "create_park" to TopLevelDestinations.PARKS,
                "park_filter" to TopLevelDestinations.PARKS,
                // Дочерние экраны Events
                "event_detail/789" to TopLevelDestinations.EVENTS,
                "create_event" to TopLevelDestinations.EVENTS,
                // Дочерние экраны Messages
                "chat/123" to TopLevelDestinations.MESSAGES,
                "friends" to TopLevelDestinations.MESSAGES,
                // Дочерние экраны More
                "theme_icon" to TopLevelDestinations.MORE
            )

        childScreens.forEach { (route, expectedDestination) ->
            // When
            appState.onDestinationChanged(route)

            // Then
            assertEquals(
                "При переходе на дочерний экран $route " +
                    "currentTopLevelDestination должен быть $expectedDestination (родительская вкладка)",
                expectedDestination,
                appState.currentTopLevelDestination
            )
        }
    }

    // ==================== Тесты динамического определения parentTab ====================

    @Test
    fun onDestinationChanged_whenUserSearchFromMessages_thenMessagesTabIsSelected() {
        // When - UserSearch открыт из Messages
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "messages"
            }
        appState.onDestinationChanged("user_search", arguments)

        // Then
        assertEquals(
            "При переходе на UserSearch из Messages currentTopLevelDestination должен быть MESSAGES",
            TopLevelDestinations.MESSAGES,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenUserSearchFromProfile_thenProfileTabIsSelected() {
        // When - UserSearch открыт из Profile
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "profile"
            }
        appState.onDestinationChanged("user_search", arguments)

        // Then
        assertEquals(
            "При переходе на UserSearch из Profile currentTopLevelDestination должен быть PROFILE",
            TopLevelDestinations.PROFILE,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenUserSearchWithoutSource_thenMessagesTabIsSelected() {
        // When - UserSearch открыт без source (default)
        appState.onDestinationChanged("user_search")

        // Then - по умолчанию должен быть Messages
        assertEquals(
            "При переходе на UserSearch без source currentTopLevelDestination должен быть MESSAGES",
            TopLevelDestinations.MESSAGES,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenOtherUserProfileFromMessages_thenMessagesTabIsSelected() {
        // When - OtherUserProfile открыт из Messages (через UserSearch)
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "messages"
            }
        appState.onDestinationChanged("other_user_profile/123", arguments)

        // Then
        assertEquals(
            "При переходе на OtherUserProfile из Messages currentTopLevelDestination должен быть MESSAGES",
            TopLevelDestinations.MESSAGES,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenOtherUserProfileFromProfile_thenProfileTabIsSelected() {
        // When - OtherUserProfile открыт из Profile
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "profile"
            }
        appState.onDestinationChanged("other_user_profile/123", arguments)

        // Then
        assertEquals(
            "При переходе на OtherUserProfile из Profile currentTopLevelDestination должен быть PROFILE",
            TopLevelDestinations.PROFILE,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenOtherUserProfileWithoutSource_thenProfileTabIsSelected() {
        // When - OtherUserProfile открыт без source (default)
        appState.onDestinationChanged("other_user_profile/123")

        // Then - по умолчанию должен быть Profile
        assertEquals(
            "При переходе на OtherUserProfile без source currentTopLevelDestination должен быть PROFILE",
            TopLevelDestinations.PROFILE,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenOtherUserProfileFromParks_thenParksTabIsSelected() {
        // When - OtherUserProfile открыт из Parks
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "parks"
            }
        appState.onDestinationChanged("other_user_profile/123", arguments)

        // Then
        assertEquals(
            "При переходе на OtherUserProfile из Parks currentTopLevelDestination должен быть PARKS",
            TopLevelDestinations.PARKS,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenOtherUserProfileFromLegacyPark_thenParksTabIsSelected() {
        // When - OtherUserProfile открыт из legacy source=park
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "park"
            }
        appState.onDestinationChanged("other_user_profile/123", arguments)

        // Then
        assertEquals(
            "При переходе на OtherUserProfile из legacy source=park currentTopLevelDestination должен быть PARKS",
            TopLevelDestinations.PARKS,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenOtherUserProfileFromEvents_thenEventsTabIsSelected() {
        // When - OtherUserProfile открыт из Events
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "events"
            }
        appState.onDestinationChanged("other_user_profile/123", arguments)

        // Then
        assertEquals(
            "При переходе на OtherUserProfile из Events currentTopLevelDestination должен быть EVENTS",
            TopLevelDestinations.EVENTS,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenOtherUserProfileFromMore_thenMoreTabIsSelected() {
        // When - OtherUserProfile открыт из More
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "more"
            }
        appState.onDestinationChanged("other_user_profile/123", arguments)

        // Then
        assertEquals(
            "При переходе на OtherUserProfile из More currentTopLevelDestination должен быть MORE",
            TopLevelDestinations.MORE,
            appState.currentTopLevelDestination
        )
    }

    @Test
    fun onDestinationChanged_whenParkFlowFromProfile_thenProfileTabIsPreservedForChildScreens() {
        val profileArguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "profile"
            }

        appState.onDestinationChanged("park_detail/10", profileArguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)

        appState.onDestinationChanged("park_trainees/10", profileArguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)

        appState.onDestinationChanged("other_user_profile/20", profileArguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)
    }

    // ==================== Этап 1: Тесты сохранения source для дочерних экранов ====================

    @Test
    fun onDestinationChanged_whenUserParksFromProfile_thenProfileTabIsSelected() {
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "profile"
            }
        appState.onDestinationChanged("user_parks/123", arguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)
    }

    @Test
    fun onDestinationChanged_whenUserParksFromParks_thenParksTabIsSelected() {
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "parks"
            }
        appState.onDestinationChanged("user_parks/456", arguments)
        assertEquals(TopLevelDestinations.PARKS, appState.currentTopLevelDestination)
    }

    @Test
    fun onDestinationChanged_whenUserTrainingParksFromProfile_thenProfileTabIsSelected() {
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "profile"
            }
        appState.onDestinationChanged("user_training_parks/123", arguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)
    }

    @Test
    fun onDestinationChanged_whenJournalsListFromProfile_thenProfileTabIsSelected() {
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "profile"
            }
        appState.onDestinationChanged("journals_list/123", arguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)
    }

    @Test
    fun onDestinationChanged_whenJournalsListFromEvents_thenEventsTabIsSelected() {
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "events"
            }
        appState.onDestinationChanged("journals_list/456", arguments)
        assertEquals(TopLevelDestinations.EVENTS, appState.currentTopLevelDestination)
    }

    @Test
    fun onDestinationChanged_whenJournalEntriesFromProfile_thenProfileTabIsSelected() {
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "profile"
            }
        appState.onDestinationChanged("journal_entries/123", arguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)
    }

    @Test
    fun onDestinationChanged_whenJournalEntriesFromMessages_thenMessagesTabIsSelected() {
        val arguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "messages"
            }
        appState.onDestinationChanged("journal_entries/456", arguments)
        assertEquals(TopLevelDestinations.MESSAGES, appState.currentTopLevelDestination)
    }

    // ==================== Цепочные сценарии ====================

    @Test
    fun onDestinationChanged_whenProfileChain_thenProfileTabIsPreserved() {
        val profileArguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "profile"
            }

        appState.onDestinationChanged("user_training_parks/1", profileArguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)

        appState.onDestinationChanged("park_detail/10", profileArguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)

        appState.onDestinationChanged("park_trainees/10", profileArguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)

        appState.onDestinationChanged("other_user_profile/20", profileArguments)
        assertEquals(TopLevelDestinations.PROFILE, appState.currentTopLevelDestination)
    }

    @Test
    fun onDestinationChanged_whenParksChain_thenParksTabIsPreserved() {
        val parksArguments =
            mockk<android.os.Bundle>(relaxed = true) {
                every { getString("source") } returns "parks"
            }

        appState.onDestinationChanged("park_detail/10", parksArguments)
        assertEquals(TopLevelDestinations.PARKS, appState.currentTopLevelDestination)

        appState.onDestinationChanged("park_trainees/10", parksArguments)
        assertEquals(TopLevelDestinations.PARKS, appState.currentTopLevelDestination)

        appState.onDestinationChanged("other_user_profile/20", parksArguments)
        assertEquals(TopLevelDestinations.PARKS, appState.currentTopLevelDestination)
    }
}
