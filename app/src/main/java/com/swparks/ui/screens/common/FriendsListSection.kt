package com.swparks.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.ds.SectionView
import com.swparks.ui.ds.UserRowData
import com.swparks.ui.ds.UserRowView

data class FriendsListConfig(
    val friends: List<User>,
    val currentUserId: Long?,
    val enabled: Boolean,
    val showTitle: Boolean,
    val onFriendClick: (Long) -> Unit,
    val modifier: Modifier = Modifier
)

@Composable
fun FriendsListSection(config: FriendsListConfig) {
    SectionView(
        titleID = if (config.showTitle) R.string.friends else null,
        titleBottomPadding = dimensionResource(R.dimen.spacing_xsmall),
        modifier = config.modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
            config.friends.forEach { user ->
                val isDisabled = user.id == config.currentUserId || !config.enabled
                UserRowView(
                    data =
                        UserRowData(
                            modifier = Modifier,
                            enabled = !isDisabled,
                            imageStringURL = user.image,
                            name = user.name,
                            address = null,
                            onClick = { config.onFriendClick(user.id) }
                        )
                )
            }
        }
    }
}

@Composable
fun FriendsListSectionWithUserData(
    friends: List<User>,
    currentUserId: Long?,
    enabled: Boolean,
    onFriendClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionView(
        titleID = null,
        titleBottomPadding = dimensionResource(R.dimen.spacing_xsmall),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
            friends.forEach { user ->
                val isDisabled = user.id == currentUserId || !enabled
                UserRowView(
                    data =
                        UserRowData(
                            modifier = Modifier,
                            enabled = !isDisabled,
                            imageStringURL = user.image,
                            name = user.name,
                            address = null,
                            onClick = { onFriendClick(user.id, user.name) }
                        )
                )
            }
        }
    }
}

@Composable
fun EmptyFriendsContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(dimensionResource(R.dimen.spacing_large)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_friends_yet),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
