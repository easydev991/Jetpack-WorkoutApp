package com.swparks.navigation

/**
 * Маршруты навигации в приложении
 */
sealed class Screen(
    val route: String,
    val parentTab: Screen? = null
) {
    // Верхнеуровневые вкладки
    object Parks : Screen("parks")
    object Events : Screen("events")
    object Messages : Screen("messages")
    object Profile : Screen("profile")
    object More : Screen("more")

    // Детальные экраны площадок
    object ParkDetail : Screen("park_detail/{parkId}", parentTab = Parks) {
        fun createRoute(parkId: Long) = "park_detail/$parkId"
    }

    object CreatePark : Screen("create_park", parentTab = Parks)
    object EditPark : Screen("edit_park/{parkId}", parentTab = Parks) {
        fun createRoute(parkId: Long) = "edit_park/$parkId"
    }

    object ParkFilter : Screen("park_filter", parentTab = Parks)

    object CreateEventForPark : Screen("create_event_for_park/{parkId}", parentTab = Parks) {
        fun createRoute(parkId: Long) = "create_event_for_park/$parkId"
    }

    object ParkRoute : Screen("park_route/{parkId}", parentTab = Parks) {
        fun createRoute(parkId: Long) = "park_route/$parkId"
    }

    object AddParkComment : Screen("add_park_comment/{parkId}", parentTab = Parks) {
        fun createRoute(parkId: Long) = "add_park_comment/$parkId"
    }

    object ParkTrainees : Screen("park_trainees/{parkId}", parentTab = Parks) {
        fun createRoute(parkId: Long) = "park_trainees/$parkId"
    }

    object ParkGallery : Screen("park_gallery/{parkId}", parentTab = Parks) {
        fun createRoute(parkId: Long) = "park_gallery/$parkId"
    }

    // Детальные экраны мероприятий
    object EventDetail : Screen("event_detail/{eventId}", parentTab = Events) {
        fun createRoute(eventId: Long) = "event_detail/$eventId"
    }

    object CreateEvent : Screen("create_event", parentTab = Events)
    object EditEvent : Screen("edit_event/{eventId}", parentTab = Events) {
        fun createRoute(eventId: Long) = "edit_event/$eventId"
    }

    object EventParticipants : Screen("event_participants/{eventId}", parentTab = Events) {
        fun createRoute(eventId: Long) = "event_participants/$eventId"
    }

    object EventGallery : Screen("event_gallery/{eventId}", parentTab = Events) {
        fun createRoute(eventId: Long) = "event_gallery/$eventId"
    }

    object AddEventComment : Screen("add_event_comment/{eventId}", parentTab = Events) {
        fun createRoute(eventId: Long) = "add_event_comment/$eventId"
    }

    // Экраны сообщений
    object Chat : Screen("chat/{dialogId}", parentTab = Messages) {
        fun createRoute(dialogId: Long) = "chat/$dialogId"
    }

    object Friends : Screen("friends", parentTab = Messages)
    object MyFriends : Screen("my_friends", parentTab = Profile)
    object UserSearch : Screen("user_search", parentTab = Messages)

    // Экраны профиля
    object EditProfile : Screen("edit_profile", parentTab = Profile)

    object UserParks : Screen("user_parks/{userId}", parentTab = Profile) {
        fun createRoute(userId: Long) = "user_parks/$userId"
    }

    object UserTrainingParks : Screen("user_training_parks/{userId}", parentTab = Profile) {
        fun createRoute(userId: Long) = "user_training_parks/$userId"
    }

    object Blacklist : Screen("blacklist", parentTab = Profile)

    object JournalsList : Screen("journals_list/{userId}", parentTab = Profile) {
        fun createRoute(userId: Long) = "journals_list/$userId"
    }

    object JournalDetail : Screen("journal_detail/{journalId}", parentTab = Profile) {
        fun createRoute(journalId: Long) = "journal_detail/$journalId"
    }

    object CreateJournal : Screen("create_journal", parentTab = Profile)
    object EditJournal : Screen("edit_journal/{journalId}", parentTab = Profile) {
        fun createRoute(journalId: Long) = "edit_journal/$journalId"
    }

    object AddJournalEntry : Screen("add_journal_entry/{journalId}", parentTab = Profile) {
        fun createRoute(journalId: Long) = "add_journal_entry/$journalId"
    }

    object ChangePassword : Screen("change_password", parentTab = Profile)
    object SelectCountry : Screen("select_country", parentTab = Profile)
    object SelectCity : Screen("select_city", parentTab = Profile)

    // Экраны настроек
    object ThemeIcon : Screen("theme_icon", parentTab = More)

    companion object {
        val allScreens by lazy {
            listOf(
                // Верхнеуровневые вкладки
                Parks, Events, Messages, Profile, More,

                // Детальные экраны площадок
                ParkDetail, CreatePark, EditPark, ParkFilter,
                CreateEventForPark, ParkRoute, AddParkComment,
                ParkTrainees, ParkGallery,

                // Детальные экраны мероприятий
                EventDetail, CreateEvent, EditEvent, EventParticipants,
                EventGallery, AddEventComment,

                // Экраны сообщений
                Chat, Friends, MyFriends, UserSearch,

                // Экраны профиля
                EditProfile, UserParks, UserTrainingParks, Blacklist,
                JournalsList, JournalDetail, CreateJournal, EditJournal,
                AddJournalEntry, ChangePassword, SelectCountry, SelectCity,

                // Экраны настроек
                ThemeIcon
            )
        }

        /**
         * Находит родительскую вкладку для маршрута
         */
        fun findParentTab(route: String): Screen? {
            val baseRoute = route.substringBefore("/")
            return allScreens.find {
                it.route.substringBefore("/") == baseRoute
            }?.parentTab
        }
    }
}
