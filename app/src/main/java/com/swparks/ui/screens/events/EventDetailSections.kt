@file:Suppress("TooManyFunctions")

package com.swparks.ui.screens.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.data.model.Comment
import com.swparks.data.model.Event
import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.CommentAction
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
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.util.DateFormatter
import com.swparks.util.parseHtmlOrNull

internal data class EventHeaderCallbacks(
    val onOpenMapClick: () -> Unit,
    val onRouteClick: () -> Unit,
    val onAddToCalendarClick: () -> Unit
)

@Composable
internal fun EventHeaderMapCalendarSection(
    event: Event,
    address: String,
    isRefreshing: Boolean,
    callbacks: EventHeaderCallbacks
) {
    FormCardContainer(
        params = FormCardContainerParams(
            Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_regular))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.spacing_small)
            )
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge
            )

            LabeledValueRow(
                label = stringResource(R.string.`when`),
                value = DateFormatter.formatDate(
                    context = androidx.compose.ui.platform.LocalContext.current,
                    dateString = event.beginDate
                )
            )

            LabeledValueRow(
                label = stringResource(R.string.where),
                value = address
            )

            val eventAddress = event.address
            if (!eventAddress.isNullOrBlank()) {
                LabeledValueRow(
                    label = stringResource(R.string.address),
                    value = eventAddress
                )
            }

            LocationInfoView(
                config = LocationInfoConfig(
                    latitude = event.latitude,
                    longitude = event.longitude,
                    address = address,
                    enabled = !isRefreshing,
                    onOpenMapClick = callbacks.onOpenMapClick,
                    onRouteClick = callbacks.onRouteClick
                )
            )

            if (event.isCurrent) {
                SWButton(
                    config = ButtonConfig(
                        modifier = Modifier.fillMaxWidth(),
                        size = SWButtonSize.LARGE,
                        mode = SWButtonMode.FILLED,
                        text = stringResource(R.string.event_add_to_calendar),
                        enabled = !isRefreshing,
                        onClick = callbacks.onAddToCalendarClick
                    )
                )
            }
        }
    }
}

@Composable
internal fun EventParticipantsSection(
    event: Event,
    isAuthorized: Boolean,
    isRefreshing: Boolean,
    onParticipantToggle: () -> Unit,
    onClickParticipants: () -> Unit
) {
    if (!isAuthorized) return

    Column(
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.spacing_regular)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        val participantsCount = event.trainingUsersCount ?: 0
        if (participantsCount > 0) {
            FormRowView(
                modifier = Modifier.fillMaxWidth(),
                leadingText = stringResource(R.string.participants),
                trailingText = pluralStringResource(
                    id = R.plurals.peopleCount,
                    count = participantsCount,
                    participantsCount
                ),
                enabled = !isRefreshing,
                onClick = onClickParticipants
            )
        }
        if (event.isCurrent) {
            SwitchFormRowView(
                modifier = Modifier.fillMaxWidth(),
                leadingText = stringResource(R.string.participate_too),
                isOn = event.trainHere ?: false,
                isEnabled = !isRefreshing,
                onCheckedChange = { onParticipantToggle() }
            )
        }
    }
}

@Composable
internal fun EventDescriptionSection(
    description: String
) {
    val parsedDescription = description.parseHtmlOrNull(compactMode = false)
    if (parsedDescription.isNullOrBlank()) return

    SectionView(
        titleID = R.string.event_description,
        addPaddingToTitle = true,
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_regular))
    ) {
        FormCardContainer(
            params = FormCardContainerParams()
        ) {
            Text(
                text = parsedDescription,
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
internal fun EventAuthorSection(
    event: Event,
    address: String,
    isAuthorized: Boolean,
    isRefreshing: Boolean,
    isEventAuthor: Boolean,
    onAuthorClick: (Long) -> Unit
) {
    SectionView(
        titleID = R.string.event_author,
        addPaddingToTitle = true,
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_regular))
    ) {
        val isEnabled = isAuthorized && !isRefreshing && !isEventAuthor
        UserRowView(
            data = UserRowData(
                enabled = isEnabled,
                imageStringURL = event.author.image,
                name = event.author.name,
                address = address,
                onClick = { onAuthorClick(event.author.id) }
            )
        )
    }
}

@Composable
internal fun EventPhotosSection(
    photos: List<Photo>,
    isRefreshing: Boolean,
    onPhotoClick: (Photo) -> Unit
) {
    PhotoSectionView(
        config = PhotoSectionConfig(
            photos = photos,
            enabled = !isRefreshing,
            onPhotoClick = onPhotoClick
        ),
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_regular))
    )
}

@Composable
internal fun EventCommentItem(
    modifier: Modifier = Modifier,
    comment: Comment,
    enabled: Boolean,
    currentUserId: Long?,
    showSectionHeader: Boolean = false,
    onAuthorClick: (Long) -> Unit,
    onActionClick: (Long, CommentAction) -> Unit
) {
    val author = comment.user
    val byMainUser = author?.id != null && author.id == currentUserId
    val commentContent: @Composable () -> Unit = {
        FormCardContainer(
            params = FormCardContainerParams(modifier)
        ) {
            CommentRowView(
                data = CommentRowData(
                    imageStringURL = author?.image,
                    authorName = author?.name ?: "",
                    dateString = DateFormatter.formatDate(
                        context = androidx.compose.ui.platform.LocalContext.current,
                        dateString = comment.date
                    ),
                    bodyText = comment.parsedBody.orEmpty(),
                    enabled = enabled,
                    byMainUser = byMainUser,
                    onAuthorClick = { author?.id?.let(onAuthorClick) },
                    onClickAction = { action -> onActionClick(comment.id, action) }
                )
            )
        }
    }

    if (showSectionHeader) {
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
            Text(
                text = stringResource(id = R.string.comments).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = dimensionResource(id = R.dimen.spacing_small) + dimensionResource(
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
internal fun EventAddCommentButton(
    isAuthorized: Boolean,
    isRefreshing: Boolean,
    onAddCommentClick: () -> Unit
) {
    SWButton(
        config = ButtonConfig(
            modifier = Modifier
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

@Composable
private fun LabeledValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.spacing_xxsmall)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
internal fun EventHeaderMapCalendarSectionPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            EventHeaderMapCalendarSection(
                event = previewEvent,
                address = "Москва, Парк Горького",
                isRefreshing = false,
                callbacks = EventHeaderCallbacks(
                    onOpenMapClick = {},
                    onRouteClick = {},
                    onAddToCalendarClick = {}
                )
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
internal fun EventParticipantsSectionPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column {
                EventParticipantsSection(
                    event = previewEvent.copy(
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

private const val PREVIEW_DESCRIPTION_HTML =
    "<p>Приглашаем всех на открытую тренировку! Начинаем в 10:00.</p>" +
        "<p>Что будет:</p><ul><li>Разминка</li><li>Подтягивания</li><li>Отжимания</li></ul>"

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
internal fun EventDescriptionSectionPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            EventDescriptionSection(description = PREVIEW_DESCRIPTION_HTML)
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
internal fun EventAuthorSectionPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            EventAuthorSection(
                event = previewEvent,
                address = "Москва",
                isAuthorized = true,
                isRefreshing = false,
                isEventAuthor = false,
                onAuthorClick = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
internal fun EventCommentItemPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                EventCommentItem(
                    comment = previewComment,
                    enabled = true,
                    currentUserId = 999L,
                    modifier = Modifier.fillMaxWidth(),
                    onAuthorClick = {},
                    onActionClick = { _, _ -> }
                )
                EventCommentItem(
                    comment = previewComment.copy(
                        user = previewComment.user?.copy(id = 999L)
                    ),
                    enabled = true,
                    currentUserId = 999L,
                    modifier = Modifier.fillMaxWidth(),
                    onAuthorClick = {},
                    onActionClick = { _, _ -> }
                )
            }
        }
    }
}

private val previewUser = User(
    id = 123L,
    name = "ivan_petrov",
    image = "https://workout.su/img/avatar_default.jpg",
    cityID = 1,
    countryID = 1,
    fullName = "Иван Петров"
)

private val previewEvent = Event(
    id = 1L,
    title = "Открытая тренировка в парке",
    description = "<p>Приглашаем всех желающих!</p>",
    beginDate = "2024-06-15 10:00:00",
    countryID = 1,
    cityID = 1,
    preview = "",
    latitude = "55.7558",
    longitude = "37.6173",
    isCurrent = true,
    address = "Москва, Парк Горького",
    photos = listOf(
        Photo(id = 1L, photo = "https://workout.su/files/trainings/photo1.jpg")
    ),
    author = previewUser,
    name = "Открытая тренировка",
    trainingUsersCount = 10,
    trainHere = true
)

private val previewComment = Comment(
    id = 1L,
    body = "Отличное мероприятие! Всем советую прийти.",
    date = "2024-06-10 14:30:00",
    user = previewUser
)
