package com.swparks.navigation

import android.os.Bundle

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
    object ParkDetail : Screen("park_detail/{parkId}?source={source}", parentTab = Parks) {
        fun createRoute(
            parkId: Long,
            source: String = "parks"
        ) = "park_detail/$parkId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "parks"
            return getScreenBySource(source, default = Parks)
        }
    }

    object CreatePark : Screen("create_park?source={source}", parentTab = Parks) {
        fun createRoute(source: String = "parks") = "create_park?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "parks"
            return getScreenBySource(source, default = Parks)
        }
    }

    object EditPark : Screen("edit_park/{parkId}?source={source}", parentTab = Parks) {
        fun createRoute(
            parkId: Long,
            source: String = "parks"
        ) = "edit_park/$parkId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "parks"
            return getScreenBySource(source, default = Parks)
        }
    }

    object ParkFilter : Screen("park_filter", parentTab = Parks)

    object CreateEventForPark :
        Screen(
            "create_event_for_park/{parkId}?parkName={parkName}&source={source}",
            parentTab = Parks
        ) {
        fun createRoute(
            parkId: Long,
            parkName: String,
            source: String = "parks"
        ) = "create_event_for_park/$parkId?parkName=${android.net.Uri.encode(parkName)}&source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "parks"
            return getScreenBySource(source, default = Parks)
        }
    }

    object ParkRoute : Screen("park_route/{parkId}?source={source}", parentTab = Parks) {
        fun createRoute(
            parkId: Long,
            source: String = "parks"
        ) = "park_route/$parkId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "parks"
            return getScreenBySource(source, default = Parks)
        }
    }

    object AddParkComment : Screen("add_park_comment/{parkId}?source={source}", parentTab = Parks) {
        fun createRoute(
            parkId: Long,
            source: String = "parks"
        ) = "add_park_comment/$parkId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "parks"
            return getScreenBySource(source, default = Parks)
        }
    }

    object ParkTrainees : Screen("park_trainees/{parkId}?source={source}", parentTab = Parks) {
        fun createRoute(
            parkId: Long,
            source: String = "parks"
        ) = "park_trainees/$parkId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "parks"
            return getScreenBySource(source, default = Parks)
        }
    }

    object ParkGallery : Screen("park_gallery/{parkId}?source={source}", parentTab = Parks) {
        fun createRoute(
            parkId: Long,
            source: String = "parks"
        ) = "park_gallery/$parkId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "parks"
            return getScreenBySource(source, default = Parks)
        }
    }

    // Детальные экраны мероприятий
    object EventDetail : Screen("event_detail/{eventId}?source={source}", parentTab = Events) {
        fun createRoute(
            eventId: Long,
            source: String = "events"
        ) = "event_detail/$eventId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "events"
            return getScreenBySource(source, default = Events)
        }
    }

    object CreateEvent : Screen("create_event", parentTab = Events)

    object EditEvent : Screen("edit_event/{eventId}?source={source}", parentTab = Events) {
        fun createRoute(
            eventId: Long,
            source: String = "events"
        ) = "edit_event/$eventId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "events"
            return getScreenBySource(source, default = Events)
        }
    }

    object EventParticipants :
        Screen("event_participants/{eventId}?source={source}", parentTab = Events) {
        fun createRoute(
            eventId: Long,
            source: String = "events"
        ) = "event_participants/$eventId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "events"
            return getScreenBySource(source, default = Events)
        }
    }

    object EventGallery : Screen("event_gallery/{eventId}?source={source}", parentTab = Events) {
        fun createRoute(
            eventId: Long,
            source: String = "events"
        ) = "event_gallery/$eventId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "events"
            return getScreenBySource(source, default = Events)
        }
    }

    object AddEventComment :
        Screen("add_event_comment/{eventId}?source={source}", parentTab = Events) {
        fun createRoute(
            eventId: Long,
            source: String = "events"
        ) = "add_event_comment/$eventId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "events"
            return getScreenBySource(source, default = Events)
        }
    }

    object SelectParkForEvent :
        Screen("select_park_for_event/{userId}?source={source}", parentTab = Events) {
        fun createRoute(
            userId: Long,
            source: String = "events"
        ) = "select_park_for_event/$userId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "events"
            return getScreenBySource(source, default = Events)
        }
    }

    // Экраны сообщений
    object Chat : Screen(
        "chat/{dialogId}?userId={userId}&userName={userName}&userImage={userImage}&source={source}",
        parentTab = Messages
    ) {
        fun createRoute(
            dialogId: Long,
            userId: Int,
            userName: String,
            userImage: String?,
            source: String = "messages"
        ): String {
            val encodedName = android.net.Uri.encode(userName)
            val encodedImage = userImage?.let { android.net.Uri.encode(it) } ?: ""
            return "chat/$dialogId?userId=$userId&userName=$encodedName&userImage=$encodedImage&source=$source"
        }

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "messages"
            return getScreenBySource(source, default = Messages)
        }
    }

    object Friends : Screen("friends", parentTab = Messages)

    object FriendsForDialog : Screen("friends_for_dialog", parentTab = Messages)

    object MyFriends : Screen("my_friends", parentTab = Profile)

    object UserSearch : Screen("user_search?source={source}", parentTab = Messages) {
        fun createRoute(source: String = "messages") = "user_search?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "messages"
            return getScreenBySource(source, default = Messages)
        }
    }

    // Экраны профиля
    object EditProfile : Screen("edit_profile", parentTab = Profile)

    object UserParks : Screen("user_parks/{userId}?source={source}", parentTab = Profile) {
        fun createRoute(
            userId: Long,
            source: String = "profile"
        ) = "user_parks/$userId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "profile"
            return getScreenBySource(source, default = Profile)
        }
    }

    object UserTrainingParks :
        Screen("user_training_parks/{userId}?source={source}", parentTab = Profile) {
        fun createRoute(
            userId: Long,
            source: String = "profile"
        ) = "user_training_parks/$userId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "profile"
            return getScreenBySource(source, default = Profile)
        }
    }

    object UserFriends : Screen("user_friends/{userId}?source={source}", parentTab = Profile) {
        fun createRoute(
            userId: Long,
            source: String = "profile"
        ) = "user_friends/$userId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "profile"
            return getScreenBySource(source, default = Profile)
        }
    }

    object OtherUserProfile :
        Screen("other_user_profile/{userId}?source={source}", parentTab = Profile) {
        fun createRoute(
            userId: Long,
            source: String = "profile"
        ) = "other_user_profile/$userId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "profile"
            return getScreenBySource(source, default = Profile)
        }
    }

    object Blacklist : Screen("blacklist", parentTab = Profile)

    object JournalsList : Screen("journals_list/{userId}?source={source}", parentTab = Profile) {
        fun createRoute(
            userId: Long,
            source: String = "profile"
        ) = "journals_list/$userId?source=$source"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "profile"
            return getScreenBySource(source, default = Profile)
        }
    }

    object JournalEntries :
        Screen(
            "journal_entries/{journalId}?userId={userId}&title={journalTitle}" +
                "&viewAccess={viewAccess}&commentAccess={commentAccess}&source={source}",
            parentTab = Profile
        ) {
        data class JournalEntriesRoute(
            val journalId: Long,
            val userId: Long,
            val journalTitle: String,
            val viewAccess: String,
            val commentAccess: String,
            val source: String = "profile"
        )

        fun createRoute(route: JournalEntriesRoute): String =
            "journal_entries/${route.journalId}?userId=${route.userId}" +
                "&title=${android.net.Uri.encode(route.journalTitle)}" +
                "&viewAccess=${route.viewAccess}" +
                "&commentAccess=${route.commentAccess}" +
                "&source=${route.source}"

        fun findParentTab(arguments: Bundle?): Screen {
            val source = arguments?.getString("source") ?: "profile"
            return getScreenBySource(source, default = Profile)
        }
    }

    object ChangePassword : Screen("change_password", parentTab = Profile)

    object SelectCountry : Screen("select_country", parentTab = Profile)

    object SelectCity : Screen("select_city", parentTab = Profile)

    object SelectCityForFilter : Screen("select_city_for_filter", parentTab = Parks)

    // Экраны настроек
    object ThemeIcon : Screen("theme_icon", parentTab = More)

    companion object {
        val allScreens by lazy {
            listOf(
                // Верхнеуровневые вкладки
                Parks,
                Events,
                Messages,
                Profile,
                More,
                // Детальные экраны площадок
                ParkDetail,
                CreatePark,
                EditPark,
                ParkFilter,
                CreateEventForPark,
                ParkRoute,
                AddParkComment,
                ParkTrainees,
                ParkGallery,
                SelectCityForFilter,
                // Детальные экраны мероприятий
                EventDetail,
                CreateEvent,
                EditEvent,
                EventParticipants,
                EventGallery,
                AddEventComment,
                SelectParkForEvent,
                // Экраны сообщений
                Chat,
                Friends,
                FriendsForDialog,
                MyFriends,
                UserSearch,
                // Экраны профиля
                EditProfile,
                UserParks,
                UserTrainingParks,
                UserFriends,
                OtherUserProfile,
                Blacklist,
                JournalsList,
                JournalEntries,
                ChangePassword,
                SelectCountry,
                SelectCity,
                // Экраны настроек
                ThemeIcon
            )
        }

        /**
         * Находит родительскую вкладку для маршрута.
         * Для экранов с source параметром вызывает findParentTab с аргументами.
         *
         * @param route строка маршрута (может содержать placeholder-ы)
         * @param arguments Bundle с фактическими значениями аргументов навигации (включая source)
         */
        fun findParentTab(
            route: String,
            arguments: Bundle? = null
        ): Screen? {
            // Special cases: UserSearch и OtherUserProfile с source параметром
            return when (val baseRoute = route.substringBefore("/").substringBefore("?")) {
                "user_search" -> UserSearch.findParentTab(arguments)
                "other_user_profile" -> OtherUserProfile.findParentTab(arguments)

                // Экраны пользователей с source параметром
                "user_friends" -> UserFriends.findParentTab(arguments)
                "user_parks" -> UserParks.findParentTab(arguments)
                "user_training_parks" -> UserTrainingParks.findParentTab(arguments)
                "journals_list" -> JournalsList.findParentTab(arguments)
                "journal_entries" -> JournalEntries.findParentTab(arguments)

                // Экраны площадок с source параметром
                "park_detail" -> ParkDetail.findParentTab(arguments)
                "edit_park" -> EditPark.findParentTab(arguments)
                "create_event_for_park" -> CreateEventForPark.findParentTab(arguments)
                "park_route" -> ParkRoute.findParentTab(arguments)
                "add_park_comment" -> AddParkComment.findParentTab(arguments)
                "park_trainees" -> ParkTrainees.findParentTab(arguments)
                "park_gallery" -> ParkGallery.findParentTab(arguments)

                // Экраны мероприятий с source параметром
                "event_detail" -> EventDetail.findParentTab(arguments)
                "edit_event" -> EditEvent.findParentTab(arguments)
                "event_participants" -> EventParticipants.findParentTab(arguments)
                "event_gallery" -> EventGallery.findParentTab(arguments)
                "add_event_comment" -> AddEventComment.findParentTab(arguments)
                "select_park_for_event" -> SelectParkForEvent.findParentTab(arguments)

                // Экраны сообщений с source параметром
                "chat" -> Chat.findParentTab(arguments)

                // Остальные экраны - используем parentTab из определения
                else ->
                    allScreens
                        .find {
                            it.route.substringBefore("/").substringBefore("?") == baseRoute
                        }?.parentTab
            }
        }
    }
}

/**
 * Определяет Screen по значению source параметра.
 *
 * @param source значение source параметра
 * @param default Screen по умолчанию, если source не распознан
 * @return Screen соответствующий source или default
 */
fun getScreenBySource(
    source: String,
    default: Screen
): Screen =
    when (source) {
        "parks", "park" -> Screen.Parks
        "events" -> Screen.Events
        "messages" -> Screen.Messages
        "profile" -> Screen.Profile
        "more" -> Screen.More
        else -> default
    }
