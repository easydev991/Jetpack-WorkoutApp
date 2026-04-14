package com.swparks.analytics

enum class AppScreen(
    val screenName: String
) {
    ROOT("root"),
    LOGIN("login"),
    PARKS_MAP("parks_map"),
    PARKS_MAP_LIST("parks_map_list"),
    PARKS_LIST_USED_BY("parks_list_used_by"),
    PARKS_LIST_EVENT("parks_list_event"),
    PARKS_ADDED_BY_USER("parks_added_by_user"),
    PARK_DETAIL("park_detail"),
    PARK_FORM("park_form"),
    PARK_FILTER("park_filter"),
    EVENTS_LIST("events_list"),
    EVENT_DETAIL("event_detail"),
    EVENT_FORM("event_form"),
    DIALOGS_LIST("dialogs_list"),
    DIALOG("dialog"),
    PROFILE_MAIN_USER("profile_main_user"),
    PROFILE_OTHER_USER("profile_other_user"),
    MAIN_USER_FRIENDS_LIST("main_user_friends_list"),
    FRIENDS_LIST("friends_list"),
    EDIT_PROFILE("edit_profile"),
    CHANGE_PASSWORD("change_password"),
    SEARCH_USERS("search_users"),
    BLACK_LIST("black_list"),
    JOURNALS_LIST("journals_list"),
    JOURNAL_ENTRIES("journal_entries"),
    JOURNAL_SETTINGS("journal_settings"),
    COUNTRY_LIST("country_list"),
    CITY_LIST("city_list"),
    MORE("more"),
    THEME_ICON("theme_icon")
}
