package com.swparks.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsEventTest {
    @Test
    fun screenView_whenNoSource_thenSourceIsNull() {
        val event = AnalyticsEvent.ScreenView(AppScreen.LOGIN)

        assertEquals(AppScreen.LOGIN, event.screen)
        assertNull(event.source)
    }

    @Test
    fun screenView_whenSourceProvided_thenSourceIsSet() {
        val event =
            AnalyticsEvent.ScreenView(
                screen = AppScreen.PARK_DETAIL,
                source = AppScreen.PARKS_MAP
            )

        assertEquals(AppScreen.PARK_DETAIL, event.screen)
        assertEquals(AppScreen.PARKS_MAP, event.source)
    }

    @Test
    fun userAction_whenNoParams_thenParamsEmpty() {
        val event = AnalyticsEvent.UserAction(UserActionType.LOGIN)

        assertEquals(UserActionType.LOGIN, event.action)
        assertEquals(emptyMap<String, String>(), event.params)
    }

    @Test
    fun userAction_whenParamsProvided_thenParamsSet() {
        val params = mapOf("key1" to "value1", "key2" to "value2")
        val event = AnalyticsEvent.UserAction(UserActionType.SAVE_PROFILE, params)

        assertEquals(UserActionType.SAVE_PROFILE, event.action)
        assertEquals(params, event.params)
    }

    @Test
    fun appError_whenCreated_thenOperationAndThrowableSet() {
        val throwable = RuntimeException("test error")
        val event = AnalyticsEvent.AppError(AppErrorOperation.LOGIN_FAILED, throwable)

        assertEquals(AppErrorOperation.LOGIN_FAILED, event.operation)
        assertEquals(throwable, event.throwable)
    }

    @Test
    fun appScreen_values_areCorrect() {
        assertEquals("root", AppScreen.ROOT.screenName)
        assertEquals("login", AppScreen.LOGIN.screenName)
        assertEquals("parks_map", AppScreen.PARKS_MAP.screenName)
        assertEquals("parks_map_list", AppScreen.PARKS_MAP_LIST.screenName)
        assertEquals("parks_list_used_by", AppScreen.PARKS_LIST_USED_BY.screenName)
        assertEquals("parks_list_event", AppScreen.PARKS_LIST_EVENT.screenName)
        assertEquals("parks_added_by_user", AppScreen.PARKS_ADDED_BY_USER.screenName)
        assertEquals("park_detail", AppScreen.PARK_DETAIL.screenName)
        assertEquals("park_form", AppScreen.PARK_FORM.screenName)
        assertEquals("park_filter", AppScreen.PARK_FILTER.screenName)
        assertEquals("events_list", AppScreen.EVENTS_LIST.screenName)
        assertEquals("event_detail", AppScreen.EVENT_DETAIL.screenName)
        assertEquals("event_form", AppScreen.EVENT_FORM.screenName)
        assertEquals("dialogs_list", AppScreen.DIALOGS_LIST.screenName)
        assertEquals("dialog", AppScreen.DIALOG.screenName)
        assertEquals("profile_main_user", AppScreen.PROFILE_MAIN_USER.screenName)
        assertEquals("profile_other_user", AppScreen.PROFILE_OTHER_USER.screenName)
        assertEquals("main_user_friends_list", AppScreen.MAIN_USER_FRIENDS_LIST.screenName)
        assertEquals("friends_list", AppScreen.FRIENDS_LIST.screenName)
        assertEquals("edit_profile", AppScreen.EDIT_PROFILE.screenName)
        assertEquals("change_password", AppScreen.CHANGE_PASSWORD.screenName)
        assertEquals("search_users", AppScreen.SEARCH_USERS.screenName)
        assertEquals("black_list", AppScreen.BLACK_LIST.screenName)
        assertEquals("journals_list", AppScreen.JOURNALS_LIST.screenName)
        assertEquals("journal_entries", AppScreen.JOURNAL_ENTRIES.screenName)
        assertEquals("journal_settings", AppScreen.JOURNAL_SETTINGS.screenName)
        assertEquals("country_list", AppScreen.COUNTRY_LIST.screenName)
        assertEquals("city_list", AppScreen.CITY_LIST.screenName)
        assertEquals("more", AppScreen.MORE.screenName)
        assertEquals("theme_icon", AppScreen.THEME_ICON.screenName)
    }

    @Test
    fun userActionType_values_areCorrect() {
        assertEquals("login", UserActionType.LOGIN.value)
        assertEquals("logout", UserActionType.LOGOUT.value)
        assertEquals("reset_password", UserActionType.RESET_PASSWORD.value)
        assertEquals("save_password", UserActionType.SAVE_PASSWORD.value)
        assertEquals("save_profile", UserActionType.SAVE_PROFILE.value)
        assertEquals("search_users", UserActionType.SEARCH_USERS.value)
        assertEquals("select_country", UserActionType.SELECT_COUNTRY.value)
        assertEquals("select_city", UserActionType.SELECT_CITY.value)
        assertEquals("add_friend", UserActionType.ADD_FRIEND.value)
        assertEquals("remove_friend", UserActionType.REMOVE_FRIEND.value)
        assertEquals("respond_friend_request_accept", UserActionType.RESPOND_FRIEND_REQUEST_ACCEPT.value)
        assertEquals("respond_friend_request_decline", UserActionType.RESPOND_FRIEND_REQUEST_DECLINE.value)
        assertEquals("block_user", UserActionType.BLOCK_USER.value)
        assertEquals("unblock_user", UserActionType.UNBLOCK_USER.value)
        assertEquals("send_message", UserActionType.SEND_MESSAGE.value)
        assertEquals("send_feedback", UserActionType.SEND_FEEDBACK.value)
        assertEquals("report_photo", UserActionType.REPORT_PHOTO.value)
        assertEquals("report_comment", UserActionType.REPORT_COMMENT.value)
        assertEquals("open_parks_filter", UserActionType.OPEN_PARKS_FILTER.value)
        assertEquals("refresh_parks", UserActionType.REFRESH_PARKS.value)
        assertEquals("open_city_search", UserActionType.OPEN_CITY_SEARCH.value)
        assertEquals("clear_city_filter", UserActionType.CLEAR_CITY_FILTER.value)
        assertEquals("select_park_filter_city", UserActionType.SELECT_PARK_FILTER_CITY.value)
        assertEquals("select_park_filter_type", UserActionType.SELECT_PARK_FILTER_TYPE.value)
        assertEquals("select_park_filter_size", UserActionType.SELECT_PARK_FILTER_SIZE.value)
        assertEquals("select_park_annotation", UserActionType.SELECT_PARK_ANNOTATION.value)
        assertEquals("open_city_search_empty_state", UserActionType.OPEN_CITY_SEARCH_EMPTY_STATE.value)
        assertEquals("open_filter_empty_state", UserActionType.OPEN_FILTER_EMPTY_STATE.value)
        assertEquals("create_park", UserActionType.CREATE_PARK.value)
        assertEquals("save_park", UserActionType.SAVE_PARK.value)
        assertEquals("delete_park", UserActionType.DELETE_PARK.value)
        assertEquals("create_event", UserActionType.CREATE_EVENT.value)
        assertEquals("save_event", UserActionType.SAVE_EVENT.value)
        assertEquals("delete_event", UserActionType.DELETE_EVENT.value)
        assertEquals("select_event_type", UserActionType.SELECT_EVENT_TYPE.value)
        assertEquals("create_journal", UserActionType.CREATE_JOURNAL.value)
        assertEquals("edit_journal", UserActionType.EDIT_JOURNAL.value)
        assertEquals("delete_journal", UserActionType.DELETE_JOURNAL.value)
        assertEquals("create_journal_entry", UserActionType.CREATE_JOURNAL_ENTRY.value)
        assertEquals("edit_journal_entry", UserActionType.EDIT_JOURNAL_ENTRY.value)
        assertEquals("delete_journal_entry", UserActionType.DELETE_JOURNAL_ENTRY.value)
        assertEquals("select_language", UserActionType.SELECT_LANGUAGE.value)
        assertEquals("select_theme", UserActionType.SELECT_THEME.value)
        assertEquals("select_app_icon", UserActionType.SELECT_APP_ICON.value)
        assertEquals("open_language_settings", UserActionType.OPEN_LANGUAGE_SETTINGS.value)
    }

    @Test
    fun appErrorOperation_values_areCorrect() {
        assertEquals("login_failed", AppErrorOperation.LOGIN_FAILED.value)
        assertEquals("password_reset_failed", AppErrorOperation.PASSWORD_RESET_FAILED.value)
        assertEquals("profile_load_failed", AppErrorOperation.PROFILE_LOAD_FAILED.value)
        assertEquals("profile_save_failed", AppErrorOperation.PROFILE_SAVE_FAILED.value)
        assertEquals("change_password_failed", AppErrorOperation.CHANGE_PASSWORD_FAILED.value)
        assertEquals("search_users_failed", AppErrorOperation.SEARCH_USERS_FAILED.value)
        assertEquals("friend_request_failed", AppErrorOperation.FRIEND_REQUEST_FAILED.value)
        assertEquals("dialogs_load_failed", AppErrorOperation.DIALOGS_LOAD_FAILED.value)
        assertEquals("dialog_delete_failed", AppErrorOperation.DIALOG_DELETE_FAILED.value)
        assertEquals("send_message_failed", AppErrorOperation.SEND_MESSAGE_FAILED.value)
        assertEquals("unblock_failed", AppErrorOperation.UNBLOCK_FAILED.value)
        assertEquals("park_save_failed", AppErrorOperation.PARK_SAVE_FAILED.value)
        assertEquals("park_delete_failed", AppErrorOperation.PARK_DELETE_FAILED.value)
        assertEquals("park_load_failed", AppErrorOperation.PARK_LOAD_FAILED.value)
        assertEquals("event_save_failed", AppErrorOperation.EVENT_SAVE_FAILED.value)
        assertEquals("event_delete_failed", AppErrorOperation.EVENT_DELETE_FAILED.value)
        assertEquals("event_load_failed", AppErrorOperation.EVENT_LOAD_FAILED.value)
        assertEquals("journal_save_failed", AppErrorOperation.JOURNAL_SAVE_FAILED.value)
        assertEquals("journal_delete_failed", AppErrorOperation.JOURNAL_DELETE_FAILED.value)
        assertEquals("journal_load_failed", AppErrorOperation.JOURNAL_LOAD_FAILED.value)
        assertEquals("countries_update_failed", AppErrorOperation.COUNTRIES_UPDATE_FAILED.value)
        assertEquals("select_country_failed", AppErrorOperation.SELECT_COUNTRY_FAILED.value)
        assertEquals("select_city_failed", AppErrorOperation.SELECT_CITY_FAILED.value)
        assertEquals("select_filter_failed", AppErrorOperation.SELECT_FILTER_FAILED.value)
        assertEquals("theme_change_failed", AppErrorOperation.THEME_CHANGE_FAILED.value)
        assertEquals("icon_change_failed", AppErrorOperation.ICON_CHANGE_FAILED.value)
    }
}
