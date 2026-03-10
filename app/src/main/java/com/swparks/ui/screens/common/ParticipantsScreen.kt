package com.swparks.ui.screens.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.ds.EmptyStateView
import com.swparks.ui.ds.UserRowData
import com.swparks.ui.ds.UserRowView
import com.swparks.ui.model.ParticipantsMode
import com.swparks.ui.model.getTitleResId
import com.swparks.ui.theme.JetpackWorkoutAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsScreen(
    modifier: Modifier = Modifier,
    mode: ParticipantsMode,
    users: List<User>,
    currentUserId: Long?,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(mode.getTitleResId()))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (users.isEmpty()) {
                EmptyStateView(
                    text = stringResource(R.string.no_participants)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = dimensionResource(R.dimen.spacing_regular),
                        top = dimensionResource(R.dimen.spacing_small),
                        end = dimensionResource(R.dimen.spacing_regular),
                        bottom = dimensionResource(R.dimen.spacing_regular)
                    ),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                ) {
                    items(
                        items = users,
                        key = { it.id }
                    ) { user ->
                        val isCurrentUser = user.id == currentUserId
                        UserRowView(
                            data = UserRowData(
                                modifier = Modifier,
                                enabled = !isCurrentUser,
                                imageStringURL = user.image,
                                name = user.name,
                                address = null,
                                onClick = { onUserClick(user.id) }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
private fun ParticipantsScreenWithUsersPreview() {
    JetpackWorkoutAppTheme {
        ParticipantsScreen(
            mode = ParticipantsMode.Event,
            users = listOf(
                User(id = 1L, name = "CurrentUser", image = null),
                User(id = 2L, name = "StreetAthlete", image = null),
                User(id = 3L, name = "WorkoutPro", image = null)
            ),
            currentUserId = 1L,
            onBack = {},
            onUserClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
private fun ParticipantsScreenEmptyPreview() {
    JetpackWorkoutAppTheme {
        ParticipantsScreen(
            mode = ParticipantsMode.Park,
            users = emptyList(),
            currentUserId = null,
            onBack = {},
            onUserClick = {}
        )
    }
}
