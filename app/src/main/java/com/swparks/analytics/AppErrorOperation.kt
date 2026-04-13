package com.swparks.analytics

enum class AppErrorOperation(
    val value: String
) {
    LOGIN_FAILED("login_failed"),
    PASSWORD_RESET_FAILED("password_reset_failed"),

    PROFILE_LOAD_FAILED("profile_load_failed"),
    PROFILE_SAVE_FAILED("profile_save_failed"),
    CHANGE_PASSWORD_FAILED("change_password_failed"),

    SEARCH_USERS_FAILED("search_users_failed"),
    FRIEND_REQUEST_FAILED("friend_request_failed"),

    DIALOGS_LOAD_FAILED("dialogs_load_failed"),
    DIALOG_DELETE_FAILED("dialog_delete_failed"),
    SEND_MESSAGE_FAILED("send_message_failed"),
    UNBLOCK_FAILED("unblock_failed"),

    PARK_SAVE_FAILED("park_save_failed"),
    PARK_DELETE_FAILED("park_delete_failed"),
    PARK_LOAD_FAILED("park_load_failed"),

    EVENT_SAVE_FAILED("event_save_failed"),
    EVENT_DELETE_FAILED("event_delete_failed"),
    EVENT_LOAD_FAILED("event_load_failed"),

    JOURNAL_SAVE_FAILED("journal_save_failed"),
    JOURNAL_DELETE_FAILED("journal_delete_failed"),
    JOURNAL_LOAD_FAILED("journal_load_failed"),

    COUNTRIES_UPDATE_FAILED("countries_update_failed"),

    SELECT_COUNTRY_FAILED("select_country_failed"),
    SELECT_CITY_FAILED("select_city_failed"),
    SELECT_FILTER_FAILED("select_filter_failed"),
    THEME_CHANGE_FAILED("theme_change_failed"),
    ICON_CHANGE_FAILED("icon_change_failed")
}
