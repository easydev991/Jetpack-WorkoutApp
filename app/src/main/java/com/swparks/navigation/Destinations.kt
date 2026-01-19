package com.swparks.navigation

/**
 * Маршруты навигации в приложении
 */
sealed class Screen(
    val route: String,
) {
    // Верхнеуровневые вкладки
    object Parks : Screen("parks")
    object Events : Screen("events")
    object Messages : Screen("messages")
    object Profile : Screen("profile")
    object More : Screen("more")

    // Детальные экраны площадок
    object ParkDetail : Screen("park_detail/{parkId}") {
        fun createRoute(parkId: Long) = "park_detail/$parkId"
    }

    object CreatePark : Screen("create_park")
    object EditPark : Screen("edit_park/{parkId}") {
        fun createRoute(parkId: Long) = "edit_park/$parkId"
    }

    object ParkFilter : Screen("park_filter")

    object CreateEventForPark : Screen("create_event_for_park/{parkId}") {
        fun createRoute(parkId: Long) = "create_event_for_park/$parkId"
    }

    object ParkRoute : Screen("park_route/{parkId}") {
        fun createRoute(parkId: Long) = "park_route/$parkId"
    }

    object AddParkComment : Screen("add_park_comment/{parkId}") {
        fun createRoute(parkId: Long) = "add_park_comment/$parkId"
    }

    object ParkTrainees : Screen("park_trainees/{parkId}") {
        fun createRoute(parkId: Long) = "park_trainees/$parkId"
    }

    object ParkGallery : Screen("park_gallery/{parkId}") {
        fun createRoute(parkId: Long) = "park_gallery/$parkId"
    }

    // Детальные экраны мероприятий
    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: Long) = "event_detail/$eventId"
    }

    object CreateEvent : Screen("create_event")
    object EditEvent : Screen("edit_event/{eventId}") {
        fun createRoute(eventId: Long) = "edit_event/$eventId"
    }

    object EventParticipants : Screen("event_participants/{eventId}") {
        fun createRoute(eventId: Long) = "event_participants/$eventId"
    }

    object EventGallery : Screen("event_gallery/{eventId}") {
        fun createRoute(eventId: Long) = "event_gallery/$eventId"
    }

    object AddEventComment : Screen("add_event_comment/{eventId}") {
        fun createRoute(eventId: Long) = "add_event_comment/$eventId"
    }

    // Экраны сообщений
    object Chat : Screen("chat/{dialogId}") {
        fun createRoute(dialogId: Long) = "chat/$dialogId"
    }

    object Friends : Screen("friends")
    object UserSearch : Screen("user_search")

    // Экраны профиля
    object EditProfile : Screen("edit_profile")

    object UserParks : Screen("user_parks/{userId}") {
        fun createRoute(userId: Long) = "user_parks/$userId"
    }

    object UserTrainingParks : Screen("user_training_parks/{userId}") {
        fun createRoute(userId: Long) = "user_training_parks/$userId"
    }

    object Blacklist : Screen("blacklist")

    object JournalsList : Screen("journals_list/{userId}") {
        fun createRoute(userId: Long) = "journals_list/$userId"
    }

    object JournalDetail : Screen("journal_detail/{journalId}") {
        fun createRoute(journalId: Long) = "journal_detail/$journalId"
    }

    object CreateJournal : Screen("create_journal")
    object EditJournal : Screen("edit_journal/{journalId}") {
        fun createRoute(journalId: Long) = "edit_journal/$journalId"
    }

    object AddJournalEntry : Screen("add_journal_entry/{journalId}") {
        fun createRoute(journalId: Long) = "add_journal_entry/$journalId"
    }

    object ChangePassword : Screen("change_password")
    object SelectCountry : Screen("select_country")
    object SelectCity : Screen("select_city")

    // Экраны настроек
    object ThemeIcon : Screen("theme_icon")

    // Экраны авторизации (модальные окна)
    object Login : Screen("login")
    object Register : Screen("register")
}
