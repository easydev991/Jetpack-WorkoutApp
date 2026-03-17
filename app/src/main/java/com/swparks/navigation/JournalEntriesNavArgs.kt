package com.swparks.navigation

import android.net.Uri
import androidx.navigation.NavBackStackEntry
import com.swparks.ui.model.JournalAccess

internal data class JournalEntriesNavArgs(
    val journalId: Long,
    val journalOwnerId: Long,
    val journalTitle: String,
    val viewAccess: String,
    val commentAccess: String,
    val source: String
)

internal fun NavBackStackEntry.consumeJournalEntriesArgs(
    defaultSource: String = "profile"
): JournalEntriesNavArgs? {
    val journalId = arguments?.getString("journalId")?.toLongOrNull()
    val journalOwnerId = arguments?.getString("userId")?.toLongOrNull()

    if (journalId == null || journalOwnerId == null) {
        return null
    }

    val journalTitle = arguments?.getString("journalTitle")?.let(Uri::decode).orEmpty()
    val viewAccess = parseJournalAccess(arguments?.getString("viewAccess"))
    val commentAccess = parseJournalAccess(arguments?.getString("commentAccess"))
    val source = arguments?.getString("source") ?: defaultSource

    return JournalEntriesNavArgs(
        journalId = journalId,
        journalOwnerId = journalOwnerId,
        journalTitle = journalTitle,
        viewAccess = viewAccess,
        commentAccess = commentAccess,
        source = source
    )
}

private fun parseJournalAccess(rawValue: String?): String {
    if (rawValue == null) return JournalAccess.NOBODY.name
    return runCatching { JournalAccess.valueOf(rawValue) }
        .map { it.name }
        .getOrElse { JournalAccess.NOBODY.name }
}
