package com.swparks.analytics

enum class UserActionType(
    val value: String
) {
    LOGIN("login"),
    LOGOUT("logout"),
    RESET_PASSWORD("reset_password"),
    SAVE_PASSWORD("save_password"),

    SAVE_PROFILE("save_profile"),
    SEARCH_USERS("search_users"),
    SELECT_COUNTRY("select_country"),
    SELECT_CITY("select_city"),

    ADD_FRIEND("add_friend"),
    REMOVE_FRIEND("remove_friend"),
    RESPOND_FRIEND_REQUEST_ACCEPT("respond_friend_request_accept"),
    RESPOND_FRIEND_REQUEST_DECLINE("respond_friend_request_decline"),
    BLOCK_USER("block_user"),
    UNBLOCK_USER("unblock_user"),

    SEND_MESSAGE("send_message"),

    SEND_FEEDBACK("send_feedback"),
    REPORT_PHOTO("report_photo"),
    REPORT_COMMENT("report_comment"),

    OPEN_PARKS_FILTER("open_parks_filter"),
    REFRESH_PARKS("refresh_parks"),
    OPEN_CITY_SEARCH("open_city_search"),
    CLEAR_CITY_FILTER("clear_city_filter"),
    SELECT_PARK_FILTER_CITY("select_park_filter_city"),
    SELECT_PARK_FILTER_TYPE("select_park_filter_type"),
    SELECT_PARK_FILTER_SIZE("select_park_filter_size"),
    SELECT_PARK_ANNOTATION("select_park_annotation"),
    OPEN_CITY_SEARCH_EMPTY_STATE("open_city_search_empty_state"),
    OPEN_FILTER_EMPTY_STATE("open_filter_empty_state"),
    CREATE_PARK("create_park"),
    SAVE_PARK("save_park"),
    DELETE_PARK("delete_park"),

    CREATE_EVENT("create_event"),
    SAVE_EVENT("save_event"),
    DELETE_EVENT("delete_event"),
    SELECT_EVENT_TYPE("select_event_type"),

    CREATE_JOURNAL("create_journal"),
    EDIT_JOURNAL("edit_journal"),
    DELETE_JOURNAL("delete_journal"),
    CREATE_JOURNAL_ENTRY("create_journal_entry"),
    EDIT_JOURNAL_ENTRY("edit_journal_entry"),
    DELETE_JOURNAL_ENTRY("delete_journal_entry"),

    SELECT_LANGUAGE("select_language"),
    SELECT_THEME("select_theme"),
    SELECT_APP_ICON("select_app_icon"),
    OPEN_LANGUAGE_SETTINGS("open_language_settings")
}
