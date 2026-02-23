package com.swparks.ui.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.ui.ds.FormRowView

/**
 * Кнопка перехода к списку друзей.
 *
 * @param friendsCount Количество друзей
 * @param friendRequestsCount Количество запросов в друзья (для чужого профиля всегда передавать 0)
 * @param onClick Callback при нажатии
 */
@Composable
fun FriendsButton(
    friendsCount: Int,
    friendRequestsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
    ) {
        FormRowView(
            leadingText = stringResource(id = R.string.friends),
            trailingText = pluralStringResource(
                id = R.plurals.friendsCount,
                count = friendsCount,
                friendsCount
            ),
            badgeValue = if (friendRequestsCount > 0) friendRequestsCount else null,
            enabled = enabled
        )
    }
}

/**
 * Кнопка перехода к списку площадок, где тренируется пользователь.
 */
@Composable
fun UsedParksButton(
    parksCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
    ) {
        FormRowView(
            leadingText = stringResource(id = R.string.where_trains),
            trailingText = pluralStringResource(
                id = R.plurals.parksCount,
                count = parksCount,
                parksCount
            ),
            enabled = enabled
        )
    }
}

/**
 * Кнопка перехода к списку добавленных пользователем площадок.
 */
@Composable
fun AddedParksButton(
    addedParksCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
    ) {
        FormRowView(
            leadingText = stringResource(id = R.string.male_added_parks),
            trailingText = pluralStringResource(
                id = R.plurals.parksCount,
                count = addedParksCount,
                addedParksCount
            ),
            enabled = enabled
        )
    }
}

/**
 * Кнопка перехода к списку дневников пользователя.
 */
@Composable
fun JournalsButton(
    journalsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
    ) {
        FormRowView(
            leadingText = stringResource(id = R.string.journals),
            trailingText = if (journalsCount > 0) {
                pluralStringResource(
                    id = R.plurals.journalsCount,
                    count = journalsCount,
                    journalsCount
                )
            } else {
                ""
            },
            enabled = enabled
        )
    }
}
