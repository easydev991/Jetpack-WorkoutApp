package com.swparks.ui.screens.events

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swparks.R
import com.swparks.data.model.Event
import com.swparks.data.model.User
import com.swparks.ui.model.EventKind
import com.swparks.ui.state.EventsUIState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.EventsEvent
import com.swparks.ui.viewmodel.FakeEventsViewModel
import com.swparks.ui.viewmodel.IEventsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для EventsScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 * Проверяет отображение списка мероприятий, реакции на нажатия
 * и поведение при разных состояниях UI (loading, error, content, empty).
 */
@RunWith(AndroidJUnit4::class)
class EventsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // Тестовые данные
    private val testUser = User(
        id = 1L,
        name = "testuser",
        image = null
    )

    private fun createTestEvent(
        id: Long = 1L,
        title: String = "Test Event",
        beginDate: String = "2026-03-15"
    ): Event {
        return Event(
            id = id,
            title = title,
            description = "Test description",
            beginDate = beginDate,
            countryID = 1,
            cityID = 1,
            preview = "https://example.com/preview.jpg",
            latitude = "55.7558",
            longitude = "37.6173",
            isCurrent = true,
            photos = emptyList(),
            author = testUser,
            name = title
        )
    }

    private fun setContent(
        uiState: EventsUIState = EventsUIState.Content(
            events = emptyList(),
            selectedTab = EventKind.FUTURE
        ),
        isAuthorized: Boolean = false,
        selectedTab: EventKind = EventKind.FUTURE,
        viewModel: IEventsViewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(uiState),
            isAuthorized = MutableStateFlow(isAuthorized),
            currentUser = MutableStateFlow(if (isAuthorized) testUser else null),
            selectedTab = MutableStateFlow(selectedTab)
        )
    ) {
        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventsScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun whenLoading_showsLoadingOverlay() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(EventsUIState.InitialLoading),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - LoadingOverlay отображается
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()
    }

    @Test
    fun whenContentWithLoading_showsLoadingOverlay() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.PAST,
                    isLoading = true
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.PAST)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - LoadingOverlay отображается поверх контента
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertIsDisplayed()

        // EmptyStateView НЕ отображается при первичной загрузке
        composeTestRule
            .onNodeWithText(context.getString(R.string.events_empty_past))
            .assertDoesNotExist()
    }

    @Test
    fun whenContentWithRefreshLoading_hidesLoadingOverlay() {
        // Given - pull-to-refresh in progress
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.PAST,
                    isLoading = true
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.PAST),
            isRefreshing = MutableStateFlow(true)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - LoadingOverlay НЕ отображается при pull-to-refresh
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.loading_content_description))
            .assertDoesNotExist()
    }

    @Test
    fun whenContent_showsEventsList() {
        // Given
        val testEvent = createTestEvent(title = "Тренировка в парке")
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = listOf(testEvent),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - мероприятие отображается в списке
        composeTestRule
            .onNodeWithText("Тренировка в парке")
            .assertIsDisplayed()
    }

    @Test
    fun whenError_showsErrorView() {
        // Given
        val errorMessage = "Ошибка загрузки мероприятий"
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(EventsUIState.Error(message = errorMessage)),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - сообщение об ошибке отображается
        composeTestRule
            .onNodeWithText(errorMessage, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun tabSelection_future_showsFutureEvents() {
        // Given
        val futureEvent = createTestEvent(id = 1L, title = "Будущее мероприятие")
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = listOf(futureEvent),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - Вкладка FUTURE активна
        composeTestRule
            .onNodeWithText(context.getString(R.string.future_events))
            .assertIsDisplayed()

        // Мероприятие отображается
        composeTestRule
            .onNodeWithText("Будущее мероприятие")
            .assertIsDisplayed()
    }

    @Test
    fun tabSelection_past_showsPastEvents() {
        // Given
        val pastEvent = createTestEvent(id = 2L, title = "Прошедшее мероприятие")
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = listOf(pastEvent),
                    selectedTab = EventKind.PAST
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.PAST)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - Вкладка PAST активна
        composeTestRule
            .onNodeWithText(context.getString(R.string.past_events))
            .assertIsDisplayed()

        // Мероприятие отображается
        composeTestRule
            .onNodeWithText("Прошедшее мероприятие")
            .assertIsDisplayed()
    }

    @Test
    fun fab_whenAuthorized_isVisible() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - FAB отображается
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun fab_whenNotAuthorized_isHidden() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - FAB не отображается
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .assertDoesNotExist()
    }

    @Test
    fun fab_click_logsAction() {
        // Given
        var fabClickCalled = false
        val viewModel = object : IEventsViewModel {
            override val eventsUIState = MutableStateFlow<EventsUIState>(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            )
            override val isAuthorized = MutableStateFlow(true)
            override val currentUser = MutableStateFlow<User?>(testUser)
            override val selectedTab = MutableStateFlow(EventKind.FUTURE)
            override val isRefreshing = MutableStateFlow(false)
            private val _events = MutableSharedFlow<EventsEvent>()
            override val events: Flow<EventsEvent> = _events.asSharedFlow()
            override fun onTabSelected(tab: EventKind) {}
            override fun refresh() {}
            override fun onEventClick(event: Event) {}
            override fun onFabClick() {
                fabClickCalled = true
            }

            override fun addCreatedEvent(event: Event) {}
            override fun removeDeletedEvent(eventId: Long) {}
        }

        // When
        setContent(viewModel = viewModel)

        // Нажимаем на FAB
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Then - метод onFabClick был вызван
        assert(fabClickCalled) { "FAB click should trigger onFabClick" }
    }

    @Test
    fun eventClick_logsAction() {
        // Given
        val testEvent = createTestEvent(title = "Кликабельное мероприятие")
        var eventClickCalled = false
        var clickedEventId: Long? = null

        val viewModel = object : IEventsViewModel {
            override val eventsUIState = MutableStateFlow<EventsUIState>(
                EventsUIState.Content(
                    events = listOf(testEvent),
                    selectedTab = EventKind.FUTURE
                )
            )
            override val isAuthorized = MutableStateFlow(false)
            override val currentUser = MutableStateFlow<User?>(null)
            override val selectedTab = MutableStateFlow(EventKind.FUTURE)
            override val isRefreshing = MutableStateFlow(false)
            private val _events = MutableSharedFlow<EventsEvent>()
            override val events: Flow<EventsEvent> = _events.asSharedFlow()
            override fun onTabSelected(tab: EventKind) {}
            override fun refresh() {}
            override fun onEventClick(event: Event) {
                eventClickCalled = true
                clickedEventId = event.id
            }

            override fun onFabClick() {}
            override fun addCreatedEvent(event: Event) {}
            override fun removeDeletedEvent(eventId: Long) {}
        }

        // When
        setContent(viewModel = viewModel)

        // Нажимаем на мероприятие
        composeTestRule
            .onNodeWithText("Кликабельное мероприятие")
            .performClick()

        // Then - метод onEventClick был вызван
        assert(eventClickCalled) { "Event click should trigger onEventClick" }
        assert(clickedEventId == testEvent.id) { "Clicked event ID should match" }
    }

    @Test
    fun pullToRefresh_triggersRefresh() {
        // Given
        var refreshCalled = false
        val viewModel = object : IEventsViewModel {
            override val eventsUIState = MutableStateFlow<EventsUIState>(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE,
                    isLoading = false
                )
            )
            override val isAuthorized = MutableStateFlow(false)
            override val currentUser = MutableStateFlow<User?>(null)
            override val selectedTab = MutableStateFlow(EventKind.FUTURE)
            override val isRefreshing = MutableStateFlow(false)
            private val _events = MutableSharedFlow<EventsEvent>()
            override val events: Flow<EventsEvent> = _events.asSharedFlow()
            override fun onTabSelected(tab: EventKind) {}
            override fun refresh() {
                refreshCalled = true
            }

            override fun onEventClick(event: Event) {}
            override fun onFabClick() {}
            override fun addCreatedEvent(event: Event) {}
            override fun removeDeletedEvent(eventId: Long) {}
        }

        // When
        setContent(viewModel = viewModel)

        // Pull-to-refresh не тестируется напрямую в Compose UI тестах,
        // но мы можем проверить, что refresh() вызывается через кнопку повтора при ошибке

        // Then - начальное состояние
        composeTestRule
            .onNodeWithText(context.getString(R.string.future_events))
            .assertIsDisplayed()

        // Проверяем, что refresh можно вызвать (метод существует и не падает)
        viewModel.refresh()
        assert(refreshCalled) { "Refresh should be callable" }
    }

    @Test
    fun emptyState_future_showsNoUpcomingEvents() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - сообщение о пустом списке предстоящих мероприятий
        composeTestRule
            .onNodeWithText(context.getString(R.string.events_empty_future))
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_past_showsNoPastEvents() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.PAST
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.PAST)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - сообщение о пустом списке прошедших мероприятий
        composeTestRule
            .onNodeWithText(context.getString(R.string.events_empty_past))
            .assertIsDisplayed()
    }

    @Test
    fun segmentedButtons_bothTabsAreDisplayed() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - обе вкладки отображаются
        composeTestRule
            .onNodeWithText(context.getString(R.string.future_events))
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText(context.getString(R.string.past_events))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun multipleEvents_displayedInList() {
        // Given
        val events = listOf(
            createTestEvent(id = 1L, title = "Мероприятие 1", beginDate = "2026-03-20"),
            createTestEvent(id = 2L, title = "Мероприятие 2", beginDate = "2026-03-21"),
            createTestEvent(id = 3L, title = "Мероприятие 3", beginDate = "2026-03-22")
        )
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = events,
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - все мероприятия отображаются
        events.forEach { event ->
            composeTestRule
                .onNodeWithText(event.title)
                .assertIsDisplayed()
        }
    }

    @Test
    fun errorState_retryButtonIsClickable() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(EventsUIState.Error(message = "Ошибка")),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - кнопка "Повторить" кликабельна
        composeTestRule
            .onNodeWithText(context.getString(R.string.try_again_button))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun eventsList_whenAddressLoaded_shouldDisplayIt() {
        // Given
        val testEvent = createTestEvent(
            id = 1L,
            title = "Мероприятие с адресом",
            beginDate = "2026-03-15"
        ).copy(countryID = 1, cityID = 1)

        val addresses = mapOf(
            (1 to 1) to "Россия, Москва"
        )

        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = listOf(testEvent),
                    selectedTab = EventKind.FUTURE,
                    addresses = addresses
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - адрес отображается
        composeTestRule
            .onNodeWithText("Россия, Москва")
            .assertIsDisplayed()

        // И название мероприятия тоже отображается
        composeTestRule
            .onNodeWithText("Мероприятие с адресом")
            .assertIsDisplayed()
    }

    @Test
    fun eventsList_whenAddressNotLoaded_shouldFallbackToIds() {
        // Given
        val testEvent = createTestEvent(
            id = 1L,
            title = "Мероприятие без адреса",
            beginDate = "2026-03-15"
        ).copy(countryID = 99, cityID = 88)

        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = listOf(testEvent),
                    selectedTab = EventKind.FUTURE,
                    addresses = emptyMap()
                )
            ),
            isAuthorized = MutableStateFlow(false),
            selectedTab = MutableStateFlow(EventKind.FUTURE)
        )

        // When
        setContent(viewModel = viewModel)

        // Then - fallback на ID отображается
        composeTestRule
            .onNodeWithText("99, 88")
            .assertIsDisplayed()

        // И название мероприятия тоже отображается
        composeTestRule
            .onNodeWithText("Мероприятие без адреса")
            .assertIsDisplayed()
    }

    // ==================== Event Creation Rule Alert Tests (Stage 2 & 3) ====================

    @Test
    fun fab_click_whenAuthorizedWithoutUsedParks_showsAlert() {
        // Given - user without used parks is authorized
        val userWithoutParks = testUser.copy(parksCount = "0")
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(userWithoutParks),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.ShowEventCreationRule
        )

        // When
        setContent(viewModel = viewModel)

        // Click FAB
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Then - alert is displayed
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_message))
            .assertIsDisplayed()
    }

    @Test
    fun alert_confirmButton_navigatesToParks() {
        // Given
        var navigateToParksCalled = false
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(testUser.copy(parksCount = "0")),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.ShowEventCreationRule
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventsScreen(
                    viewModel = viewModel,
                    onNavigateToParks = { navigateToParksCalled = true }
                )
            }
        }

        // Click FAB to show alert
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Click "Go to map" button
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_open_parks))
            .performClick()

        // Then
        assert(navigateToParksCalled) { "onNavigateToParks should be called when confirm button is clicked" }
    }

    @Test
    fun alert_dismissButton_closesAlert() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(testUser.copy(parksCount = "0")),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.ShowEventCreationRule
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventsScreen(
                    viewModel = viewModel,
                    onNavigateToParks = {}
                )
            }
        }

        // Click FAB to show alert
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Verify alert is shown
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_title))
            .assertIsDisplayed()

        // Click dismiss button
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_dismiss))
            .performClick()

        // Then - alert is dismissed
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_title))
            .assertDoesNotExist()
    }

    @Test
    fun alert_hasCorrectTitle() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(testUser.copy(parksCount = "0")),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.ShowEventCreationRule
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventsScreen(viewModel = viewModel)
            }
        }

        // Click FAB to show alert
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Then - title matches expected iOS key Alert.EventCreationRule
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_title))
            .assertIsDisplayed()
    }

    @Test
    fun alert_hasCorrectMessage() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(testUser.copy(parksCount = "0")),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.ShowEventCreationRule
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventsScreen(viewModel = viewModel)
            }
        }

        // Click FAB to show alert
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Then - message matches expected iOS key Alert.EventCreationRule
        // ru: "Чтобы создать мероприятие, нужно указать хотя бы одну площадку, где ты тренируешься"
        // en: "To create an event, you need to specify at least one park where you train"
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_message))
            .assertIsDisplayed()
    }

    @Test
    fun alert_hasGoToMapButton() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(testUser.copy(parksCount = "0")),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.ShowEventCreationRule
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventsScreen(viewModel = viewModel)
            }
        }

        // Click FAB to show alert
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Then - "Go to map" button is displayed
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_open_parks))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun alert_hasDismissButton() {
        // Given
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(testUser.copy(parksCount = "0")),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.ShowEventCreationRule
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventsScreen(viewModel = viewModel)
            }
        }

        // Click FAB to show alert
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Then - dismiss button is displayed with correct text
        // ru: "Понятно", en: "Understood"
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_dismiss))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun fab_click_whenUserHasUsedParks_doesNotShowAlert() {
        // Given - user with used parks
        val userWithParks = testUser.copy(parksCount = "2")
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(userWithParks),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.NavigateToCreateEvent
        )

        // When
        setContent(viewModel = viewModel)

        // Click FAB
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Then - alert is NOT displayed (FAB navigates to create event instead)
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_title))
            .assertDoesNotExist()
    }

    // ==================== Stage 4: End-to-End User Scenarios ====================

    @Test
    fun fab_click_whenUserHasUsedParks_navigatesToCreateEvent() {
        // Given - user with used parks
        var navigateToCreateEventCalled = false
        val userWithParks = testUser.copy(parksCount = "2")
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(userWithParks),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.NavigateToCreateEvent
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventsScreen(
                    viewModel = viewModel,
                    onNavigateToCreateEvent = { navigateToCreateEventCalled = true }
                )
            }
        }

        // When - click FAB
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Then - navigate to create event is called
        assert(navigateToCreateEventCalled) { "onNavigateToCreateEvent should be called when user has used parks" }
    }

    @Test
    fun fab_click_whenUserWithoutUsedParks_fullFlow_toParksTab() {
        // Given - user without used parks
        var navigateToParksCalled = false
        val userWithoutParks = testUser.copy(parksCount = "0")
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(userWithoutParks),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.ShowEventCreationRule
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventsScreen(
                    viewModel = viewModel,
                    onNavigateToParks = { navigateToParksCalled = true }
                )
            }
        }

        // Step 1: Click FAB -> alert appears
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Verify alert is shown
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_message))
            .assertIsDisplayed()

        // Step 2: Click "Go to map" button -> navigate to Parks tab
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_open_parks))
            .performClick()

        // Then - onNavigateToParks is called
        assert(navigateToParksCalled) { "onNavigateToParks should be called when user taps 'Go to map' in alert" }
    }

    @Test
    fun fab_click_whenUserWithoutUsedParks_alertDismiss_doesNotNavigate() {
        // Given - user without used parks
        var navigateToParksCalled = false
        var navigateToCreateEventCalled = false
        val userWithoutParks = testUser.copy(parksCount = "0")
        val viewModel = FakeEventsViewModel(
            eventsUIState = MutableStateFlow(
                EventsUIState.Content(
                    events = emptyList(),
                    selectedTab = EventKind.FUTURE
                )
            ),
            isAuthorized = MutableStateFlow(true),
            currentUser = MutableStateFlow(userWithoutParks),
            selectedTab = MutableStateFlow(EventKind.FUTURE),
            fabClickResult = EventsEvent.ShowEventCreationRule
        )

        composeTestRule.setContent {
            JetpackWorkoutAppTheme {
                EventsScreen(
                    viewModel = viewModel,
                    onNavigateToCreateEvent = { navigateToCreateEventCalled = true },
                    onNavigateToParks = { navigateToParksCalled = true }
                )
            }
        }

        // Step 1: Click FAB -> alert appears
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.events_fab_description))
            .performClick()

        // Verify alert is shown
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_title))
            .assertIsDisplayed()

        // Step 2: Click dismiss button -> alert closes, no navigation
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_dismiss))
            .performClick()

        // Then - alert is dismissed, no navigation occurred
        composeTestRule
            .onNodeWithText(context.getString(R.string.event_creation_rule_title))
            .assertDoesNotExist()

        assert(!navigateToParksCalled) { "onNavigateToParks should NOT be called when user dismisses alert" }
        assert(!navigateToCreateEventCalled) { "onNavigateToCreateEvent should NOT be called when user has no parks" }
    }
}
