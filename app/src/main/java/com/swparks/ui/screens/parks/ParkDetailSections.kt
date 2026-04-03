package com.swparks.ui.screens.parks

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.data.model.Comment
import com.swparks.data.model.Park
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.CommentRowData
import com.swparks.ui.ds.CommentRowView
import com.swparks.ui.ds.FormCardContainer
import com.swparks.ui.ds.FormCardContainerParams
import com.swparks.ui.ds.FormRowView
import com.swparks.ui.ds.LocationInfoConfig
import com.swparks.ui.ds.LocationInfoView
import com.swparks.ui.ds.PhotoSectionConfig
import com.swparks.ui.ds.PhotoSectionView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SectionView
import com.swparks.ui.ds.SwitchFormRowView
import com.swparks.ui.ds.UserRowData
import com.swparks.ui.ds.UserRowView
import com.swparks.ui.screens.parks.map.isValidCoordinates
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.util.DateFormatter

internal sealed class ParkHeaderAction {
    data object OpenMap : ParkHeaderAction()

    data object Route : ParkHeaderAction()

    data object CreateEvent : ParkHeaderAction()
}

internal data class ParkAuthorConfig(
    val isAuthorized: Boolean,
    val isRefreshing: Boolean,
    val isParkAuthor: Boolean
) {
    val isEnabled: Boolean
        get() = isAuthorized && !isRefreshing && !isParkAuthor
}

internal data class CommentItemConfig(
    val enabled: Boolean,
    val currentUserId: Long?,
    val showSectionHeader: Boolean = false
)

internal sealed class CommentItemAction {
    data class AuthorClick(
        val userId: Long
    ) : CommentItemAction()

    data class CommentAction(
        val commentId: Long,
        val action: com.swparks.ui.ds.CommentAction
    ) : CommentItemAction()
}

@Composable
internal fun ParkHeaderMapSection(
    park: Park,
    address: String,
    isAuthorized: Boolean,
    isRefreshing: Boolean,
    onAction: (ParkHeaderAction) -> Unit
) {
    FormCardContainer(
        params =
            FormCardContainerParams(
                Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_regular))
            )
    ) {
        ParkHeaderContent(
            park = park,
            address = address,
            isAuthorized = isAuthorized,
            isRefreshing = isRefreshing,
            onAction = onAction
        )
    }
}

@Composable
private fun ParkHeaderContent(
    park: Park,
    address: String,
    isAuthorized: Boolean,
    isRefreshing: Boolean,
    onAction: (ParkHeaderAction) -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_regular)),
        verticalArrangement =
            Arrangement.spacedBy(
                dimensionResource(R.dimen.spacing_small)
            )
    ) {
        Text(
            text = park.name,
            style = MaterialTheme.typography.titleLarge
        )

        val parkAddress = park.address
        if (!parkAddress.isBlank()) {
            Text(
                text = parkAddress,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        val hasValidCoordinates = isValidCoordinates(park.latitude, park.longitude)
        LocationInfoView(
            config =
                LocationInfoConfig(
                    latitude = park.latitude,
                    longitude = park.longitude,
                    address = address,
                    enabled = !isRefreshing && hasValidCoordinates,
                    onOpenMapClick = { onAction(ParkHeaderAction.OpenMap) },
                    onRouteClick = { onAction(ParkHeaderAction.Route) }
                )
        )

        if (isAuthorized) {
            SWButton(
                config =
                    ButtonConfig(
                        modifier = Modifier.fillMaxWidth(),
                        size = SWButtonSize.LARGE,
                        mode = SWButtonMode.FILLED,
                        text = stringResource(R.string.create_event),
                        enabled = !isRefreshing,
                        onClick = { onAction(ParkHeaderAction.CreateEvent) }
                    )
            )
        }
    }
}

@Composable
internal fun ParkParticipantsSection(
    park: Park,
    isAuthorized: Boolean,
    isRefreshing: Boolean,
    onParticipantToggle: () -> Unit,
    onClickParticipants: () -> Unit
) {
    if (!isAuthorized) return

    Column(
        modifier =
            Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacing_regular)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        val participantsCount = park.trainingUsersCount ?: 0
        val hasParticipantsList = park.trainingUsers.orEmpty().isNotEmpty()
        val participantsEnabled = !isRefreshing && hasParticipantsList
        if (participantsCount > 0) {
            FormRowView(
                modifier = Modifier.fillMaxWidth(),
                leadingText = stringResource(R.string.park_trainees_title),
                trailingText =
                    pluralStringResource(
                        id = R.plurals.peopleCount,
                        count = participantsCount,
                        participantsCount
                    ),
                enabled = participantsEnabled,
                onClick = onClickParticipants
            )
        }
        SwitchFormRowView(
            modifier = Modifier.fillMaxWidth(),
            leadingText = stringResource(R.string.train_here),
            isOn = park.trainHere ?: false,
            isEnabled = !isRefreshing,
            onCheckedChange = { onParticipantToggle() }
        )
    }
}

@Composable
internal fun ParkAuthorSection(
    park: Park,
    address: String,
    config: ParkAuthorConfig,
    onAuthorClick: (Long) -> Unit
) {
    val author = park.author ?: return

    SectionView(
        titleID = R.string.added_by,
        addPaddingToTitle = true,
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_regular))
    ) {
        UserRowView(
            data =
                UserRowData(
                    enabled = config.isEnabled,
                    imageStringURL = author.image,
                    name = author.name,
                    address = address,
                    onClick = { onAuthorClick(author.id) }
                )
        )
    }
}

@Composable
internal fun ParkPhotosSection(
    photos: List<Photo>,
    isRefreshing: Boolean,
    onPhotoClick: (Photo) -> Unit
) {
    PhotoSectionView(
        config =
            PhotoSectionConfig(
                photos = photos,
                enabled = !isRefreshing,
                onPhotoClick = onPhotoClick
            ),
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_regular))
    )
}

@Composable
internal fun ParkCommentItem(
    modifier: Modifier = Modifier,
    comment: Comment,
    config: CommentItemConfig,
    onAction: (CommentItemAction) -> Unit
) {
    val author = comment.user
    val byMainUser = author?.id != null && author.id == config.currentUserId
    val commentContent: @Composable () -> Unit = {
        FormCardContainer(
            params = FormCardContainerParams(modifier)
        ) {
            CommentRowView(
                data =
                    CommentRowData(
                        imageStringURL = author?.image,
                        authorName = author?.name ?: "",
                        dateString =
                            DateFormatter.formatDate(
                                context = LocalContext.current,
                                dateString = comment.date
                            ),
                        bodyText = comment.parsedBody.orEmpty(),
                        enabled = config.enabled,
                        byMainUser = byMainUser,
                        onAuthorClick = {
                            author?.id?.let {
                                onAction(
                                    CommentItemAction.AuthorClick(
                                        it
                                    )
                                )
                            }
                        },
                        onClickAction = { action ->
                            onAction(
                                CommentItemAction.CommentAction(
                                    comment.id,
                                    action
                                )
                            )
                        }
                    )
            )
        }
    }

    if (config.showSectionHeader) {
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
            Text(
                text = stringResource(id = R.string.comments).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        start =
                            dimensionResource(id = R.dimen.spacing_small) +
                                dimensionResource(
                                    R.dimen.spacing_regular
                                )
                    )
            )
            commentContent()
        }
    } else {
        commentContent()
    }
}

@Composable
internal fun ParkAddCommentButton(
    isAuthorized: Boolean,
    isRefreshing: Boolean,
    onAddCommentClick: () -> Unit
) {
    SWButton(
        config =
            ButtonConfig(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_regular))
                        .padding(bottom = dimensionResource(R.dimen.spacing_regular)),
                size = SWButtonSize.LARGE,
                mode = SWButtonMode.FILLED,
                text = stringResource(R.string.add_comment),
                enabled = isAuthorized && !isRefreshing,
                onClick = onAddCommentClick
            )
    )
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
internal fun ParkHeaderMapSectionPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            ParkHeaderMapSection(
                park = previewPark,
                address = "Москва, Парк Горького",
                isAuthorized = true,
                isRefreshing = false,
                onAction = {}
            )
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
internal fun ParkParticipantsSectionPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column {
                ParkParticipantsSection(
                    park =
                        previewPark.copy(
                            trainingUsersCount = 15,
                            trainHere = false
                        ),
                    isAuthorized = true,
                    isRefreshing = false,
                    onParticipantToggle = {},
                    onClickParticipants = {}
                )
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
internal fun ParkAuthorSectionPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            ParkAuthorSection(
                park = previewPark,
                address = "Москва",
                config =
                    ParkAuthorConfig(
                        isAuthorized = true,
                        isRefreshing = false,
                        isParkAuthor = false
                    ),
                onAuthorClick = {}
            )
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
internal fun ParkCommentItemPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                ParkCommentItem(
                    comment = previewComment,
                    config =
                        CommentItemConfig(
                            enabled = true,
                            currentUserId = 999L
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    onAction = {}
                )
                ParkCommentItem(
                    comment =
                        previewComment.copy(
                            user = previewComment.user?.copy(id = 999L)
                        ),
                    config =
                        CommentItemConfig(
                            enabled = true,
                            currentUserId = 999L
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    onAction = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
internal fun ParkAddCommentButtonPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Row {
                ParkAddCommentButton(
                    isAuthorized = true,
                    isRefreshing = false,
                    onAddCommentClick = {}
                )
            }
        }
    }
}

private val previewUser =
    User(
        id = 123L,
        name = "ivan_petrov",
        image = "https://workout.su/img/avatar_default.jpg",
        cityID = 1,
        countryID = 1,
        fullName = "Иван Петров"
    )

private val previewPark =
    Park(
        id = 1L,
        name = "Воркаут площадка в Парке Горького",
        sizeID = 2,
        typeID = 1,
        longitude = "37.6173",
        latitude = "55.7558",
        address = "Москва, Парк Горького, недалеко от главного входа",
        cityID = 1,
        countryID = 1,
        preview = "",
        photos =
            listOf(
                Photo(id = 1L, photo = "https://workout.su/files/areas/photo1.jpg")
            ),
        author = previewUser,
        trainingUsersCount = 10,
        trainHere = true
    )

private val previewComment =
    Comment(
        id = 1L,
        body = "Отличная площадка! Хорошее оборудование.",
        date = "2024-06-10 14:30:00",
        user = previewUser
    )
